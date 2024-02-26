package com.parent.accessibility_service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_SETTINGS
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

class AppLaunchAccessibilityService : AccessibilityService() {

    private var blockedApps: MutableList<String> = mutableListOf()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Handle incoming broadcasts
            intent?.let {
                if (it.action == "ACTION_FROM_APP") {
                    blockedApps = (it.getStringArrayExtra("KEY") ?: arrayOf()).toMutableList()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null && blockedApps.contains(packageName)) {
                // App is blocked, take necessary action
                // For example, show a blocking activity or dialog
                // Or bring the parent control app to the foreground
                // Depending on the implementation
                performBlockingAction(packageName)
            }
        }
    }

    override fun onInterrupt() {
        println("onInterrupt()")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the BroadcastReceiver
        applicationContext.unregisterReceiver(receiver)
    }



    override fun onKeyEvent(event: KeyEvent?): Boolean {
        when(event?.action) {
            KEYCODE_SETTINGS -> println("KEYCODE_SETTINGS")
        }
        return super.onKeyEvent(event)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        println("onServiceConnected()")
        super.onServiceConnected()

        // Register the BroadcastReceiver
        val filter = IntentFilter("ACTION_FROM_APP")
        ContextCompat.registerReceiver(applicationContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    private fun performBlockingAction(packageName: String) {
        // Perform blocking action here, e.g., show a dialog
        println("Launch is blocked")

        val intent = Intent("ACTION_FROM_ACCESSIBILITY_SERVICE")
        intent.putExtra("KEY", packageName)
        sendBroadcast(intent)
    }
}