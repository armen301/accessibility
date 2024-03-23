package com.parent.accessibility_service

data class AppData(
    val appName: String,
    val appPackage: String,
    val icon: ByteArray,
    val category: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppData

        if (appName != other.appName) return false
        if (appPackage != other.appPackage) return false
        if (!icon.contentEquals(other.icon)) return false
        if (category != other.category) return false

        return true
    }

    override fun hashCode(): Int {
        var result = appName.hashCode()
        result = 31 * result + appPackage.hashCode()
        result = 31 * result + icon.contentHashCode()
        result = 31 * result + (category?.hashCode() ?: 0)
        return result
    }
}