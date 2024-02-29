package com.parent.accessibility_service

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
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
internal const val EXTRA_KEY = "EXTRA_KEY"

const val INTENT_BUNDLE_KEY = "INTENT_BUNDLE_KEY"

class AccessibilityService {

    private lateinit var activity: Activity

    fun init(activity: Activity) {
        this.activity = activity
        val filter = IntentFilter(ACTION_FROM_SERVICE)
        ContextCompat.registerReceiver(activity.applicationContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                if (it.action == ACTION_FROM_SERVICE) {
                    bringAppToForeground(it.getStringExtra(EXTRA_KEY))
                }
            }
        }
    }

    private fun bringAppToForeground(data: String?) {
        val intent = Intent(activity, activity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        ContextCompat.startActivity(activity, intent, bundleOf(INTENT_BUNDLE_KEY to data))
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

    fun blockApps(toBeBlocked: Array<String>) {
        val intent = Intent(ACTION_FROM_APP).apply {
            putExtra(EXTRA_KEY, toBeBlocked)
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
}