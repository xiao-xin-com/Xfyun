@file:JvmName("RxMediaPlayer")

package com.xiaoxin.library.rxaudio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.support.annotation.RawRes
import android.support.annotation.RequiresApi
import android.view.SurfaceHolder
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.CompletableTransformer
import io.reactivex.schedulers.Schedulers


class PlayerException(val mp: MediaPlayer?, val what: Int, val extra: Int) : Exception()

private fun CompletableEmitter.emitter(block: CompletableEmitter.() -> Unit): Unit? {
    return this.takeUnless { it.isDisposed }?.block()
}

private class RxMediaPlayerListener(
    private val emitter: CompletableEmitter
) : MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    override fun onCompletion(mp: MediaPlayer?) {
        mp?.release()
        emitter.onComplete()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mp?.release()
        emitter.onError(PlayerException(mp, what, extra))
        return true
    }
}

private fun setMediaPlayer(context: Context, emitter: CompletableEmitter, mp: MediaPlayer?) {
    fun emitter(block: CompletableEmitter.() -> Unit) = emitter.emitter(block)
    if (mp == null) {
        emitter.onError(CreateException())
        return
    }

    val listener = RxMediaPlayerListener(emitter)

    val audioManager = context.applicationContext
        .getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val focusChangeListener: (Int) -> Unit = { focusChange: Int ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE -> emitter {
                mp.takeUnless { it.isPlaying }?.start()
            }
            AudioManager.AUDIOFOCUS_LOSS -> emitter {
                listener.onCompletion(mp)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> emitter {
                mp.takeIf { it.isPlaying }?.pause()
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val audioFocusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(AudioAttributes.Builder().build())
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
        audioManager.requestAudioFocus(audioFocusRequest)

        emitter.setCancellable {
            mp.release()
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
    } else {
        val durationHint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
        } else {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        }
        audioManager.requestAudioFocus(focusChangeListener, -1, durationHint)
        emitter.setCancellable {
            mp.release()
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }

    with(mp) {
        emitter {
            setOnCompletionListener(listener)
            setOnErrorListener(listener)
            start()
        }
    }
}

class CreateException : NullPointerException()

fun Context.startPlay(@RawRes resId: Int): Completable {
    return Completable.create { emitter ->
        val ctx: Context = applicationContext
        setMediaPlayer(ctx, emitter, MediaPlayer.create(ctx, resId))
    }.subscribeOn(Schedulers.io())
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Context.startPlay(
    @RawRes resId: Int,
    audioAttributes: AudioAttributes?,
    audioSessionId: Int
): Completable {
    return Completable.create { emitter ->
        val ctx: Context = applicationContext
        val mp: MediaPlayer? = MediaPlayer.create(ctx, resId, audioAttributes, audioSessionId)
        setMediaPlayer(ctx, emitter, mp)
    }.subscribeOn(Schedulers.io())
}

fun Context.startPlay(uri: Uri): Completable {
    return Completable.create { emitter ->
        val ctx: Context = applicationContext
        setMediaPlayer(ctx, emitter, MediaPlayer.create(ctx, uri))
    }.subscribeOn(Schedulers.io())
}

fun Context.startPlay(uri: Uri, holder: SurfaceHolder?): Completable {
    return Completable.create { emitter ->
        val ctx: Context = applicationContext
        setMediaPlayer(ctx, emitter, MediaPlayer.create(ctx, uri, holder))
    }.subscribeOn(Schedulers.io())
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Context.startPlay(
    uri: Uri,
    holder: SurfaceHolder?,
    audioAttributes: AudioAttributes?,
    audioSessionId: Int
): Completable {
    return Completable.create { emitter ->
        val ctx: Context = applicationContext
        val mp: MediaPlayer? = MediaPlayer.create(ctx, uri, holder, audioAttributes, audioSessionId)
        setMediaPlayer(ctx, emitter, mp)
    }.subscribeOn(Schedulers.io())
}

@JvmOverloads
fun Context.requestAudioFocus(
    streamType: Int,
    durationHint: Int,
    listener: (Int) -> Unit = { }
): CompletableTransformer {
    return CompletableTransformer trans@{ t ->
        val audioManager = applicationContext
            .getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return@trans t

        return@trans if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setLegacyStreamType(streamType)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            val focusRequest = AudioFocusRequest.Builder(durationHint)
                .setOnAudioFocusChangeListener(listener)
                .setAudioAttributes(audioAttributes)
                .build()

            t.doOnSubscribe {
                audioManager.requestAudioFocus(focusRequest)
            }.doFinally {
                audioManager.abandonAudioFocusRequest(focusRequest)
            }
        } else {
            t.doOnSubscribe {
                audioManager.requestAudioFocus(listener, streamType, durationHint)
            }.doFinally {
                audioManager.abandonAudioFocus(listener)
            }
        }
    }
}