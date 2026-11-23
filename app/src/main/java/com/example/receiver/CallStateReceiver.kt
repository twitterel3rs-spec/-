package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class IncomingCallEvent(
    val phoneNumber: String,
    val timestamp: Long = System.currentTimeMillis()
)

class CallStateReceiver : BroadcastReceiver() {

    companion object {
        private val _incomingCallFlow = MutableStateFlow<IncomingCallEvent?>(null)
        val incomingCallFlow: StateFlow<IncomingCallEvent?> = _incomingCallFlow.asStateFlow()

        fun postCallEvent(number: String) {
            if (number.isNotBlank()) {
                _incomingCallFlow.value = IncomingCallEvent(number)
            }
        }

        fun clearCallEvent() {
            _incomingCallFlow.value = null
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        try {
            // Check permission before touching any telephony API or intent extras
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_PHONE_STATE
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                val state = try {
                    intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                } catch (t: Throwable) {
                    null
                }

                if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                    val incomingNumber = try {
                        intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    } catch (t: Throwable) {
                        null
                    }
                    if (!incomingNumber.isNullOrBlank()) {
                        postCallEvent(incomingNumber)
                    }
                }
            }
        } catch (t: Throwable) {
            // Guarantee zero crashes from system broadcast delivery
        }
    }
}
