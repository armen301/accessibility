package com.parent.accessibility_service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

class AppBlockerAccessibilityService : AccessibilityService() {

    private var blockedApps: MutableList<String> = mutableListOf()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Handle incoming broadcasts
            intent?.let {
                if (it.action == ACTION_FROM_APP) {
                    blockedApps = (it.getStringArrayExtra(EXTRA_KEY) ?: arrayOf()).toMutableList()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null && blockedApps.contains(packageName)) {
                performBlockingAction(packageName)
            }
        }
    }

    override fun onInterrupt() {
        println("onInterrupt()")
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.unregisterReceiver(receiver)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()

        val filter = IntentFilter(ACTION_FROM_APP)
        ContextCompat.registerReceiver(applicationContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun performBlockingAction(packageName: String) {
        val intent = Intent(ACTION_FROM_SERVICE)
        intent.putExtra(EXTRA_KEY, packageName)
        sendBroadcast(intent)
    }
}