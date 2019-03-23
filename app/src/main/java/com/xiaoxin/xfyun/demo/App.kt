package com.xiaoxin.xfyun.demo

import android.app.Application
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechUtility

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val appId = "57ee08bd"
        val netCheck = false
        SpeechUtility.createUtility(
            this,
            "${SpeechConstant.APPID}=$appId"
        //,${SpeechConstant.NET_CHECK}=$netCheck
        )
    }
}