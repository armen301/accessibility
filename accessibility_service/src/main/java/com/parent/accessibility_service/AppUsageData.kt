package com.parent.accessibility_service

import android.app.usage.UsageStats

data class AppUsageData(
    val usageStats: UsageStats,
    val appData: AppData,
)
