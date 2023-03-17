package com.example.fpt

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import live.videosdk.rtc.android.VideoSDK
@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(applicationContext)
        VideoSDK.initialize(applicationContext)
    }
}