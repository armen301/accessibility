package com.parent.accessibility_service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
import androidx.core.content.ContextCompat

class AppBlockerAccessibilityService : AccessibilityService() {

    private lateinit var blockedApps: Array<String>
    private var blockAfter = 0L

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (it.action == ACTION_FROM_APP) {
                    it.getStringArrayExtra(ARRAY_KEY)?.let { stringArray ->
                        blockedApps = stringArray
                        return
                    }
                    blockAfter = it.getLongExtra(TIME_KEY, 0)
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        if (blockAfter == 0L || !this::blockedApps.isInitialized) {
            return
        }

        when (event.eventType) {
            TYPE_VIEW_CLICKED,
            TYPE_VIEW_SCROLLED,
            TYPE_WINDOW_STATE_CHANGED,
            TYPE_WINDOW_CONTENT_CHANGED -> {
                val packageName = event.packageName
                if (packageName != null && blockedApps.contains(packageName) && System.currentTimeMillis() >= blockAfter) {
                    performBlockingAction(packageName.toString())
                }
            }

            else -> Unit
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        val filter = IntentFilter(ACTION_FROM_APP)
        ContextCompat.registerReceiver(applicationContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationContext.unregisterReceiver(receiver)
        performServiceDisabledAction()
    }

    override fun onInterrupt() {
        performServiceDisabledAction()
    }

    private fun performServiceDisabledAction() {
        val intent = Intent(ACTION_FROM_SERVICE)
        intent.putExtra(ARRAY_KEY, SERVICE_DISABLED)
        sendBroadcast(intent)
    }

    private fun performBlockingAction(packageName: String) {
        val intent = Intent(ACTION_FROM_SERVICE)
        intent.putExtra(ARRAY_KEY, packageName)
        sendBroadcast(intent)
    }
}