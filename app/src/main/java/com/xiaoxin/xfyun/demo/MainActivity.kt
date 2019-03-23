package com.xiaoxin.xfyun.demo

import android.media.RingtoneManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.iflytek.cloud.SpeechConstant
import com.xiaoxin.library.rxaudio.startPlay
import com.xiaoxin.xfyun.rxmsc.getSynthesizerParameter
import com.xiaoxin.xfyun.rxmsc.speak
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getSynthesizerParameter().apply {
            engineType = SpeechConstant.TYPE_LOCAL
        }
        startSpeaking.setOnClickListener {
            Completable.defer { speak(edit.text.toString()) }
                .repeatWhen { it.delay(1,TimeUnit.SECONDS) }
                .subscribeOn(Schedulers.io())
                .subscribe({
                    Toast.makeText(this, "播放完成", Toast.LENGTH_SHORT).show()
                }, {
                    Log.e("MainActivity", "播放失败", it)
                })
        }

        play.setOnClickListener {
            startPlay(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)).subscribe({
                Toast.makeText(this, "播放完成", Toast.LENGTH_SHORT).show()
            }, {
                Log.e("MainActivity", "播放失败", it)
            })
        }
    }
}
