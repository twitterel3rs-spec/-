package com.example

import android.app.Application
import android.content.Context
import android.os.Process
import android.util.Log

class SennaraApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            val arLocale = java.util.Locale("ar")
            java.util.Locale.setDefault(arLocale)
            val config = resources.configuration
            config.setLocale(arLocale)
            config.setLayoutDirection(arLocale)
        } catch (_: Exception) {}
        setupGlobalExceptionHandler()
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("SennaraApp", "CRITICAL UNCAUGHT EXCEPTION on thread: ${thread.name}", throwable)

            try {
                val now = System.currentTimeMillis()
                val prefs = getSharedPreferences("sennara_crash_log", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("last_crash_message", throwable.localizedMessage ?: throwable.toString())
                    .putString("last_crash_stacktrace", Log.getStackTraceString(throwable))
                    .putLong("last_crash_timestamp", now)
                    .apply()
            } catch (t: Throwable) {
                Log.e("SennaraApp", "Error recording crash state", t)
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                System.exit(10)
            }
        }
    }
}
