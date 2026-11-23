package com.example.receiver

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.service.SennaraBackgroundService
import com.example.sync.AppSyncCoordinator
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context ?: return
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        try {
            val prefs = ctx.getSharedPreferences("sennara_terminal_prefs", Context.MODE_PRIVATE)
            val bgEnabled = prefs.getBoolean("background_service_enabled", true)
            if (!bgEnabled) return

            val app = ctx.applicationContext as? Application
            if (app != null) {
                if (FirebaseApp.getApps(app).isEmpty()) {
                    FirebaseApp.initializeApp(app)
                }
                val hasUser = try {
                    Firebase.auth.currentUser != null
                } catch (_: Throwable) {
                    false
                }
                if (hasUser) {
                    AppSyncCoordinator.getInstance(app).ensureStarted()
                    SennaraBackgroundService.syncServiceRunningState(app, true)
                }
            }
        } catch (t: Throwable) {
            Log.w("BootCompletedReceiver", "Boot start skipped: ${t.message}")
        }
    }
}
