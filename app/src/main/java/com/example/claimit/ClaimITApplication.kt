package com.example.claimit

import android.app.Application

class ClaimITApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ClaimITApplication
            private set
    }
}
