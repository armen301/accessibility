package com.parent.accessibility_service

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.APP_OPS_SERVICE
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import java.io.ByteArrayOutputStream

class AppBlockerService {
    companion object {
        const val SERVICE_DISABLED = "SERVICE_DISABLED"
        const val INTENT_BUNDLE_KEY = "INTENT_BUNDLE_KEY"

        @SuppressLint("StaticFieldLeak")
        private var activity: Activity? = null

        private var blockedApp: String? = null
        private var serviceDisabled = false

        @JvmStatic
        fun init(activity: Activity) {
            this.activity = activity
        }

        private fun bringAppToForeground(data: String?) {
            activity?.let {
                val intent = Intent(it, it::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                ContextCompat.startActivity(it, intent, bundleOf(INTENT_BUNDLE_KEY to data))
            }
        }

        @JvmStatic
        fun whichAppBlocked(): String? {
            val blocked = blockedApp
            blockedApp = null
            return blocked
        }

        @JvmStatic
        fun isServiceDisabled(): Boolean {
            val disabled = serviceDisabled
            serviceDisabled = false
            return disabled
        }

        @JvmStatic
        fun getAllApps(): Array<AppData> {
            val activity = this.activity ?: return emptyArray()

            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val resolveInfoList: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                activity.packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            }

            val appList = mutableListOf<String>()
            for (resolveInfo in resolveInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                if (!appList.contains(packageName)) {
                    appList.add(packageName)
                }
            }

            return appList.filterNot { it == activity.packageName }.map { packageName ->
                val applicationInfo = activity.appInfo(packageName)
                val appName = activity.packageManager.getApplicationLabel(applicationInfo).toString()
                val appIcon = activity.packageManager.getApplicationIcon(applicationInfo)
                val category = ApplicationInfo.getCategoryTitle(activity, applicationInfo.category)?.toString()

                AppData(
                    appName = appName,
                    appPackage = packageName,
                    icon = appIcon.toByteArray(),
                    category = category,
                )
            }.toTypedArray()
        }

        /**
         * @param apps - apps packages to be blocked
         */
        @JvmStatic
        fun appsToBeBlocked(apps: Array<String>) {
            activity?.applicationContext?.getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE)?.edit()
                ?.putStringSet(PREF_BLOCKED_APP_KEY, apps.toSet())?.apply()
        }

        /**
         * @param time - the time after which need to block an app
         */
        @JvmStatic
        fun blockAfter(time: Long) {
            activity?.applicationContext?.getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE)?.edit()
                ?.putLong(PREF_BLOCK_AFTER_KEY, time)?.apply()
        }

        private fun Activity.appInfo(packageName: String): ApplicationInfo {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            }
        }

        private fun Drawable.toByteArray(): ByteArray {
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)

            ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                return stream.toByteArray()
            }
        }

        @JvmStatic
        fun isAccessibilityServiceEnabled(): Boolean {
            activity?.let { activity ->
                val accessibilityManager =
                    activity.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                val enabledServices =
                    accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                val serviceComponentName = ComponentName(activity, AppBlockerAccessibilityService::class.java)

                return enabledServices.any {
                    it.resolveInfo.serviceInfo.packageName == activity.packageName && it.resolveInfo.serviceInfo.name == serviceComponentName.className
                }
            }
            return false
        }

        @JvmStatic
        fun checkUsageStatsPermission(): Boolean {
            activity?.let {
                val appOpsManager = it.getSystemService(APP_OPS_SERVICE) as AppOpsManager
                val mode = appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    it.packageName
                )
                return mode == AppOpsManager.MODE_ALLOWED
            }
            return false
        }

        @JvmStatic
        fun getAppUsageData(beginTime: Long, endTime: Long): Array<UsageStats> {
            activity?.let {
                val usageStatsManager = it.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, beginTime, endTime)
                    .toTypedArray()
            }

            return emptyArray()
        }

        @JvmStatic
        fun getAppUsageDataMap(beginTime: Long, endTime: Long): Map<String, Long> {
            val usageStats = getAppUsageData(beginTime, endTime)
            val map = mutableMapOf<String, Long>()
            usageStats.forEach {
                map[it.packageName] = it.totalTimeInForeground
            }
            return map
        }

        fun onInterrupt() {
            serviceDisabled = true
            bringAppToForeground(SERVICE_DISABLED)
            activity = null
        }

        fun onDestroy() {
            serviceDisabled = true
            bringAppToForeground(SERVICE_DISABLED)
            activity = null
        }

        fun onBlock(packageName: String) {
            bringAppToForeground(packageName)
        }
    }

}