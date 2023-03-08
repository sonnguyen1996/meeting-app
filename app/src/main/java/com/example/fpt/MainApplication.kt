package com.example.fpt

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import live.videosdk.rtc.android.VideoSDK
@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        VideoSDK.initialize(applicationContext)
    }
}