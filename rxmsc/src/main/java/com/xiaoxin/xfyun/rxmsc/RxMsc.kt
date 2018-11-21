package com.xiaoxin.xfyun.rxmsc

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
object RxMsc {
    private lateinit var context_: Context
    val context get() = context_
    @JvmStatic
    fun init(context: Context) {
        this.context_ = context.applicationContext
    }
}