package com.parent.accessibility_service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

class AppBlockerAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val blockAfter = TimestampDataHolder.getData() ?: return
        val blockedApps = BlockedAppsDataHolder.getData() ?: return

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

    override fun onDestroy() {
        AppBlockerService.onDestroy()
        super.onDestroy()
    }

    override fun onInterrupt() {
        AppBlockerService.onInterrupt()
    }
}