package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.sync.AppSyncCoordinator
import com.example.util.BackgroundNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SennaraBackgroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notificationUpdateJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!startForegroundSafely()) {
            stopSelf()
            return
        }
        try {
            val coordinator = AppSyncCoordinator.getInstance(application)
            coordinator.ensureStarted()
            notificationUpdateJob?.cancel()
            notificationUpdateJob = serviceScope.launch {
                combine(
                    coordinator.deviceRole,
                    coordinator.isDeviceOnline,
                    coordinator.isCloudConnected,
                    coordinator.pendingSyncOrdersCount
                ) { role, online, connected, pending ->
                    BackgroundNotificationHelper.buildForegroundServiceNotification(
                        context = this@SennaraBackgroundService,
                        deviceRole = role,
                        isOnline = online,
                        isCloudConnected = connected,
                        pendingCount = pending
                    )
                }.collect { notif ->
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                    manager?.notify(BackgroundNotificationHelper.FOREGROUND_SERVICE_NOTIFICATION_ID, notif)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error initializing SennaraBackgroundService coordinator", t)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!startForegroundSafely()) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            AppSyncCoordinator.getInstance(application).ensureStarted()
        } catch (_: Throwable) {}
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            val coordinator = AppSyncCoordinator.getInstance(application)
            if (coordinator.backgroundServiceEnabled.value && coordinator.cloudUser.value != null) {
                val restartIntent = Intent(applicationContext, SennaraBackgroundService::class.java)
                val piFlags = android.app.PendingIntent.FLAG_ONE_SHOT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
                val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    android.app.PendingIntent.getForegroundService(applicationContext, 7102, restartIntent, piFlags)
                } else {
                    android.app.PendingIntent.getService(applicationContext, 7102, restartIntent, piFlags)
                }
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                alarmManager?.set(
                    android.app.AlarmManager.ELAPSED_REALTIME,
                    android.os.SystemClock.elapsedRealtime() + 1500L,
                    pendingIntent
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "onTaskRemoved reschedule skipped: ${t.message}")
        }
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w(TAG, "Foreground service timeout reached for fgsType=$fgsType; stopping cleanly.")
        stopSelf()
    }

    override fun onDestroy() {
        notificationUpdateJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startForegroundSafely(): Boolean {
        val initialNotif = try {
            val coordinator = AppSyncCoordinator.getInstance(application)
            BackgroundNotificationHelper.buildForegroundServiceNotification(
                context = this,
                deviceRole = coordinator.deviceRole.value,
                isOnline = coordinator.isDeviceOnline.value,
                isCloudConnected = coordinator.isCloudConnected.value,
                pendingCount = coordinator.pendingSyncOrdersCount.value
            )
        } catch (_: Throwable) {
            BackgroundNotificationHelper.buildForegroundServiceNotification(
                context = this,
                deviceRole = "CASHIER",
                isOnline = true,
                isCloudConnected = false,
                pendingCount = 0
            )
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    BackgroundNotificationHelper.FOREGROUND_SERVICE_NOTIFICATION_ID,
                    initialNotif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(
                    BackgroundNotificationHelper.FOREGROUND_SERVICE_NOTIFICATION_ID,
                    initialNotif
                )
            }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "startForegroundSafely failed: ${t.message}")
            false
        }
    }

    companion object {
        private const val TAG = "SennaraBgService"

        fun syncServiceRunningState(context: Context, shouldRun: Boolean) {
            try {
                val intent = Intent(context, SennaraBackgroundService::class.java)
                if (shouldRun) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.stopService(intent)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "syncServiceRunningState($shouldRun) skipped/failed: ${t.message}")
            }
        }
    }
}
