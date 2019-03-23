@file:JvmName("RxSpeechSynthesizer")

package com.xiaoxin.xfyun.rxmsc

import android.content.Context
import android.os.Bundle
import com.iflytek.cloud.*
import com.iflytek.cloud.util.ResourceUtil
import io.reactivex.*
import java.util.concurrent.CancellationException

//获取发音人资源路径
//合成通用资源
//发音人资源
private fun Context.getResourcePath(voice: String): String {
    return buildString {
        append(
            ResourceUtil.generateResourcePath(
                this@getResourcePath,
                ResourceUtil.RESOURCE_TYPE.assets, "tts/common.jet"
            )
        )
        append(";")
        append(
            ResourceUtil.generateResourcePath(
                this@getResourcePath,
                ResourceUtil.RESOURCE_TYPE.assets,
                "tts/$voice.jet"
            )
        )
    }
}

private fun setParam(context: Context, mTts: SpeechSynthesizer) {


    with(context.getSynthesizerParameter()) {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, params)
        val engineType = engineType
        val voiceName = voiceName
        // 根据合成引擎设置相应参数
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, engineType)
        // 设置在线合成发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, voiceName)
        if (engineType == SpeechConstant.TYPE_LOCAL) {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL)
            //本地合成不设置语速、音调、音量，默认使用语记设置
            //开发者如需自定义参数，请参考在线合成参数设置
            //设置发音人资源路径
            mTts.setParameter(
                ResourceUtil.TTS_RES_PATH,
                context.getResourcePath(voiceName ?: "xiaoyan")
            )
        } else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD)
        }
        mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY, ttsDataNotify)
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, speed.toString())
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, pitch.toString())
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, volume.toString())

        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, streamType.toString())
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, keyRequestFocus.toString())

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, audioFormat)
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, ttsAudioPath)
        Unit
    }
}

//错误码包装异常
class ErrorCodeException(val code: Int) : Exception()

private class RxInitListener(
    private val emitter: SingleEmitter<Int>
) : InitListener {
    override fun onInit(code: Int) {
        if (code == ErrorCode.SUCCESS) {
            emitter.onSuccess(code)
        } else {
            emitter.onError(ErrorCodeException(code))
        }
    }
}

private fun Context.createSynthesizer(): Single<Int> {
    return Single.create<Int> { emitter ->
        SpeechSynthesizer.createSynthesizer(
            applicationContext,
            RxInitListener(emitter)
        )
    }
}

fun Context.getSynthesizer(): Single<SpeechSynthesizer> {
    return Single.defer {
        val synthesizer = SpeechSynthesizer.getSynthesizer()
        return@defer if (synthesizer != null) {
            Single.just(synthesizer)
        } else {
            createSynthesizer().map { SpeechSynthesizer.getSynthesizer() }
        }
    }
}

fun Context.startSpeaking(text: String): Observable<SpeakEvent> {
    return getSynthesizer().doOnSuccess { setParam(this@startSpeaking, it) }
        .flatMapObservable { tts ->
            Observable.using({ tts }, { speechSynthesizer ->
                Observable.create<SpeakEvent> { emitter ->
                    emitter.takeUnless { it.isDisposed }?.apply {
                        val code =
                            speechSynthesizer.startSpeaking(text, RxSynthesizerListener(emitter))
                        if (code != ErrorCode.SUCCESS) {
                            onError(ErrorCodeException(code))
                        }
                    }
                }
            }, { it.stopSpeaking() }, true)
        }
}

fun Context.speak(text: String): Completable {
    return startSpeaking(text).ignoreElements()
}

enum class SpeakEvent {
    ON_BEGIN, ON_PAUSED, ON_RESUMED, ON_ERROR, ON_COMPLETED
}

private class RxSynthesizerListener(
    private val emitter: ObservableEmitter<SpeakEvent>
) : SynthesizerListener {

    override fun onSpeakProgress(percent: Int, beginPos: Int, endPos: Int) = Unit
    override fun onBufferProgress(percent: Int, beginPos: Int, endPos: Int, info: String?) = Unit
    override fun onSpeakBegin() = emitter.onNext(SpeakEvent.ON_BEGIN)
    override fun onSpeakPaused() = emitter.onNext(SpeakEvent.ON_PAUSED)
    override fun onSpeakResumed() = emitter.onNext(SpeakEvent.ON_RESUMED)
    override fun onEvent(eventType: Int, arg1: Int, arg2: Int, bundle: Bundle?) {
        if (eventType == SpeechEvent.EVENT_TTS_CANCEL) {
            emitter.onNext(SpeakEvent.ON_ERROR)
            emitter.onError(CancellationException())
        }
    }

    override fun onCompleted(error: SpeechError?) {
        if (error != null) {
            emitter.onNext(SpeakEvent.ON_ERROR)
            emitter.onError(error)
        } else {
            emitter.onNext(SpeakEvent.ON_COMPLETED)
            emitter.onComplete()
        }
    }
}
