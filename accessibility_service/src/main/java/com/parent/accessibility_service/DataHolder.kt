package com.parent.accessibility_service

object BlockedAppsDataHolder {
    private var data: Array<String>? = null

    fun setData(value: Array<String>) {
        data = value
    }

    fun getData(): Array<String>? {
        return data
    }
}

object TimestampDataHolder {
    private var data: Long? = null

    fun setData(value: Long) {
        data = value
    }

    fun getData(): Long? {
        return data
    }
}