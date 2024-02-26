package com.parent.accessibility_service

interface AccessibilityServiceInteraction {

    fun setBlockedApps(apps: List<String>)

    companion object {
        @JvmStatic
        fun asInterface(binder: android.os.IBinder): AccessibilityServiceInteraction {
            return binder as AccessibilityServiceInteraction
        }
    }
}