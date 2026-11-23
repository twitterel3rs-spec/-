package com.example.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object BackgroundNotificationHelper {

    const val CHANNEL_BG_SYNC = "sennara_bg_sync_channel"
    const val CHANNEL_ORDER_ALERTS = "sennara_order_alerts_channel"
    const val FOREGROUND_SERVICE_NOTIFICATION_ID = 7101

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val bgChannel = NotificationChannel(
            CHANNEL_BG_SYNC,
            "مراقبة الأوردرات والطباعة في الخلفية",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "يحافظ على اتصال الكاشير والبائع بالسحابة واستقبال أوامر الطباعة فوراً"
            setShowBadge(false)
        }
        manager.createNotificationChannel(bgChannel)

        val alertChannel = NotificationChannel(
            CHANNEL_ORDER_ALERTS,
            "تنبيهات الأوردرات المعلقة والطباعة",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "تنبيهات الأوردرات المتأخرة، عدم استجابة الكاشير، وحالة الطباعة"
            enableVibration(true)
        }
        manager.createNotificationChannel(alertChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(fallback)
            } catch (_: Exception) {}
        }
    }

    private fun mainActivityPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(context, 7100, intent, flags)
    }

    fun buildForegroundServiceNotification(
        context: Context,
        deviceRole: String,
        isOnline: Boolean,
        isCloudConnected: Boolean,
        pendingCount: Int
    ): Notification {
        ensureChannels(context)
        val roleTitle = if (deviceRole == "SELLER") "صنارة وشبكة — بائع صالة" else "صنارة وشبكة — الكاشير الرئيسي"
        val statusText = when {
            !isCloudConnected -> "الوضع المحلي — جاهز للعمل"
            !isOnline && pendingCount > 0 ->
                if (deviceRole == "SELLER") "تليفونك (البائع) أوفلاين — يوجد $pendingCount أوردر معلق"
                else "جهاز الكاشير أوفلاين — بانتظار عودة الإنترنت"
            !isOnline ->
                if (deviceRole == "SELLER") "تليفونك (البائع) أوفلاين — أي أوردر جديد سيُعلق مؤقتاً"
                else "جهاز الكاشير أوفلاين الآن"
            deviceRole == "SELLER" && pendingCount > 0 -> "تليفونك متصل بالسحابة — يوجد $pendingCount أوردر بانتظار رد الكاشير"
            deviceRole == "SELLER" -> "تليفونك متصل بالسحابة ويعمل في الخلفية لمتابعة الأوردرات ✓"
            else -> "الكاشير متصل بالسحابة وفي انتظار أوردرات البائعين للطباعة الفورية ✓"
        }

        return NotificationCompat.Builder(context, CHANNEL_BG_SYNC)
            .setSmallIcon(R.drawable.ic_stat_sennara)
            .setContentTitle(roleTitle)
            .setContentText(statusText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(statusText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(mainActivityPendingIntent(context))
            .build()
    }

    fun notifyOrderAlert(
        context: Context,
        notificationKey: String,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notifId = 8000 + (notificationKey.hashCode() and 0x0FFF)
        val notification = NotificationCompat.Builder(context, CHANNEL_ORDER_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_sennara)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(mainActivityPendingIntent(context))
            .build()
        try {
            manager.notify(notifId, notification)
        } catch (_: SecurityException) {
        } catch (_: Exception) {}
    }
}
