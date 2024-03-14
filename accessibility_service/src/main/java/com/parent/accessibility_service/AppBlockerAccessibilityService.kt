package com.parent.accessibility_service

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

const val PREF_BLOCKED_APP_KEY = "PREF_BLOCKED_APP_KEY"
const val PREF_BLOCK_AFTER_KEY = "PREF_BLOCK_AFTER_KEY"
const val PREF_FILE_NAME = "PREF_FILE_NAME"

class AppBlockerAccessibilityService : AccessibilityService() {

    private lateinit var pref: SharedPreferences

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val blockAfter = pref.getLong(PREF_BLOCK_AFTER_KEY, 0)
        if (blockAfter == 0L) return
        val blockedApps = pref.getStringSet(PREF_BLOCKED_APP_KEY, null) ?: return

        when (event.eventType) {
            TYPE_VIEW_CLICKED,
            TYPE_VIEW_SCROLLED,
            TYPE_WINDOW_STATE_CHANGED,
            TYPE_WINDOW_CONTENT_CHANGED -> {
                val packageName = event.packageName
                if (packageName != null && blockedApps.contains(packageName) && System.currentTimeMillis() >= blockAfter) {
                    AppBlockerService.onBlock(packageName.toString())
                }
            }

            else -> Unit
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        pref = applicationContext.getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE)
    }

    override fun onDestroy() {
        AppBlockerService.onDestroy()
        super.onDestroy()
    }

    override fun onInterrupt() {
        AppBlockerService.onInterrupt()
    }
}