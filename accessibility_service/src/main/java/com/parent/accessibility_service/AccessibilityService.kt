package com.parent.accessibility_service

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.APP_OPS_SERVICE
import android.content.Intent
import android.content.IntentFilter
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

internal const val ACTION_FROM_SERVICE = "ACTION_FROM_SERVICE"
internal const val ACTION_FROM_APP = "ACTION_FROM_APP"
internal const val ARRAY_KEY = "ARRAY_KEY"
internal const val TIME_KEY = "TIME_KEY"
internal const val SERVICE_DISABLED = "SERVICE_DISABLED"

const val INTENT_BUNDLE_KEY = "INTENT_BUNDLE_KEY"

class AccessibilityService {

    private lateinit var activity: Activity
    private var blockedApp: String? = null
    private var serviceDisabled = false

    fun init(activity: Activity) {
        this.activity = activity
        val filter = IntentFilter(ACTION_FROM_SERVICE)
        ContextCompat.registerReceiver(
            activity.applicationContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (it.action == ACTION_FROM_SERVICE) {
                    val data = it.getStringExtra(ARRAY_KEY) ?: return
                    if (data == SERVICE_DISABLED) {
                        serviceDisabled = true
                        bringAppToForeground(data)
                        return
                    }

                    blockedApp = data
                    bringAppToForeground(data)
                }
            }
        }
    }

    private fun bringAppToForeground(data: String?) {
        val intent = Intent(activity, activity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        ContextCompat.startActivity(activity, intent, bundleOf(INTENT_BUNDLE_KEY to data))
    }

    fun whichAppBlocked(): String? {
        val blocked = blockedApp
        blockedApp = null
        return blocked
    }

    fun isServiceDisabled(): Boolean {
        val disabled = serviceDisabled
        serviceDisabled = false
        return disabled
    }

    fun getAllApps(): Array<AppData> {
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

            AppData(
                appName = appName,
                appPackage = packageName,
                icon = appIcon.toByteArray()
            )
        }.toTypedArray()
    }

    /**
     * @param apps - apps packages to be blocked
     */
    fun appsToBeBlocked(apps: Array<String>) {
        val intent = Intent(ACTION_FROM_APP).apply {
            putExtra(ARRAY_KEY, apps)
        }
        activity.applicationContext.sendBroadcast(intent)
    }

    /**
     * @param time - the time after which need to block an app
     */
    fun blockAfter(time: Long) {
        val intent = Intent(ACTION_FROM_APP).apply {
            putExtra(TIME_KEY, time)
        }
        activity.applicationContext.sendBroadcast(intent)
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

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val serviceComponentName = ComponentName(context, AppBlockerAccessibilityService::class.java)

        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName && it.resolveInfo.serviceInfo.name == serviceComponentName.className
        }
    }

    fun checkUsageStatsPermission(): Boolean {
        val appOpsManager = activity.getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            activity.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getAppUsageData(beginTime: Long, endTime: Long): Array<UsageStats> {
        val usageStatsManager = activity.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, beginTime, endTime).toTypedArray()
    }

    fun getAppUsageDataMap(beginTime: Long, endTime: Long): Map<String, Long> {
        val usageStats = getAppUsageData(beginTime, endTime)
        val map = mutableMapOf<String, Long>()
        usageStats.forEach {
            map[it.packageName] = it.totalTimeInForeground
        }
        return map
    }
}