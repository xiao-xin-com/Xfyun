@file:JvmName("RxSpeechSynthesizer")

package com.xiaoxin.xfyun.rxmsc

import android.content.Context
import android.os.Bundle
import com.iflytek.cloud.*
import io.reactivex.*


private fun setParam(context: Context, mTts: SpeechSynthesizer) {
    with(context.getSynthesizerParameter()) {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, params)
        // 根据合成引擎设置相应参数
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, engineType)
        mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY, ttsDataNotify)
        // 设置在线合成发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, voiceName)
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

fun Context.createSynthesizer(): Single<Int> {
    return Single.create<Int> { emitter ->
        SpeechSynthesizer.createSynthesizer(applicationContext, RxInitListener(emitter))
    }
}

fun Context.getSynthesizer(): Single<SpeechSynthesizer> {
    val synthesizer = SpeechSynthesizer.getSynthesizer()
    return if (synthesizer != null) {
        Single.just(synthesizer)
    } else {
        createSynthesizer().map { SpeechSynthesizer.getSynthesizer() }
    }
}

fun Context.startSpeaking(text: String): Observable<SpeakEvent> {
    return getSynthesizer().doOnSuccess { setParam(this@startSpeaking, it) }
        .flatMapObservable { tts ->
            Observable.create<SpeakEvent> { emitter ->
                emitter.setCancellable { tts.stopSpeaking() }
                emitter.takeUnless { it.isDisposed }?.apply {
                    val code = tts.startSpeaking(text, RxSynthesizerListener(emitter))
                    if (code != ErrorCode.SUCCESS) {
                        onError(ErrorCodeException(code))
                    }
                }
            }
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
    override fun onEvent(eventType: Int, arg1: Int, arg2: Int, bundle: Bundle?) = Unit

    override fun onSpeakBegin() = emitter.onNext(SpeakEvent.ON_BEGIN)

    override fun onSpeakPaused() = emitter.onNext(SpeakEvent.ON_PAUSED)

    override fun onSpeakResumed() = emitter.onNext(SpeakEvent.ON_RESUMED)

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
