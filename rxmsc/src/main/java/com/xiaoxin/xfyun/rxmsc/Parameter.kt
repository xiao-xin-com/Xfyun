package com.xiaoxin.xfyun.rxmsc

import android.content.Context
import android.media.AudioManager
import com.iflytek.cloud.SpeechConstant
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class Parameter(
    private val context: Context,
    private val name: String
) {
    private val keyValue by lazy {
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    @JvmOverloads
    fun String(key: String, defaultValue: String? = null): ReadWriteProperty<Any, String?> {
        return object : ReadWriteProperty<Any, String?> {
            override fun getValue(thisRef: Any, property: KProperty<*>): String? =
                keyValue.getString(key, defaultValue)

            override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) =
                keyValue.edit().putString(key, value).apply()
        }
    }

    @JvmOverloads
    fun Int(key: String, defaultValue: Int = 0): ReadWriteProperty<Any, Int> {
        return object : ReadWriteProperty<Any, Int> {
            override fun getValue(thisRef: Any, property: KProperty<*>): Int =
                keyValue.getInt(key, defaultValue)

            override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) =
                keyValue.edit().putInt(key, value).apply()
        }
    }

    @JvmOverloads
    fun Long(key: String, defaultValue: Long = 0): ReadWriteProperty<Any, Long> {
        return object : ReadWriteProperty<Any, Long> {
            override fun getValue(thisRef: Any, property: KProperty<*>): Long =
                keyValue.getLong(key, defaultValue)

            override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) =
                keyValue.edit().putLong(key, value).apply()
        }
    }

    @JvmOverloads
    fun Boolean(key: String, defaultValue: Boolean = false): ReadWriteProperty<Any, Boolean> {
        return object : ReadWriteProperty<Any, Boolean> {
            override fun getValue(thisRef: Any, property: KProperty<*>): Boolean =
                keyValue.getBoolean(key, defaultValue)

            override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) =
                keyValue.edit().putBoolean(key, value).apply()
        }
    }
}


object SynthesizerParameter {
    private val parameter by lazy { Parameter(RxMsc.context, "SynthesizerParameter") }
    @JvmStatic
    var voiceName by parameter.String(SpeechConstant.VOICE_NAME, "xiaoyan")
    @JvmStatic
    var engineType by parameter.String(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD)
    @JvmStatic
    var speed by parameter.Int(SpeechConstant.SPEED, 50)
    @JvmStatic
    var pitch by parameter.Int(SpeechConstant.PITCH, 50)
    @JvmStatic
    var volume by parameter.Int(SpeechConstant.VOICE_NAME, 50)
    @JvmStatic
    var ttsDataNotify by parameter.String(SpeechConstant.TTS_DATA_NOTIFY, "1")
    @JvmStatic
    var params by parameter.String(SpeechConstant.PARAMS, null)
    @JvmStatic
    var streamType by parameter.Int(SpeechConstant.STREAM_TYPE, AudioManager.STREAM_MUSIC)
    @JvmStatic
    var ttsAudioPath by parameter.String(SpeechConstant.TTS_AUDIO_PATH, null)
    @JvmStatic
    var audioFormat by parameter.String(SpeechConstant.AUDIO_FORMAT, "pcm")
    @JvmStatic
    var keyRequestFocus by parameter.Boolean(SpeechConstant.KEY_REQUEST_FOCUS, true)
}