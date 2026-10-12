package com.example

import android.app.Application
import com.example.core.logger.AppLogger

class DompetPintarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.initialize(this)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                AppLogger.recordCrash(thread, throwable)
            } catch (_: Exception) {}

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
