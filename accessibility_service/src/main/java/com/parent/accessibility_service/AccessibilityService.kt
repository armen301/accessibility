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

class AccessibilityService {

    private lateinit var activity: Activity

    fun init(activity: Activity) {
        this.activity = activity
        val filter = IntentFilter("ACTION_FROM_ACCESSIBILITY_SERVICE")
        ContextCompat.registerReceiver(activity.applicationContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Handle incoming broadcasts
            intent?.let {
                if (it.action == "ACTION_FROM_ACCESSIBILITY_SERVICE") {
                    val data = it.getStringExtra("KEY")
                    // Process the received data
                    bringAppToForeground(data)
                }
            }
        }
    }

    private fun bringAppToForeground(data: String?) {
        val intent = Intent(activity, activity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        ContextCompat.startActivity(activity, intent, bundleOf("key" to data))
    }


    fun getAllApps(activity: Activity): List<AppData> {
        // Create an Intent with the action you are interested in, e.g., ACTION_MAIN for main activities
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

        // Query all activities that can be performed for the given intent
        val resolveInfoList: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            activity.packageManager.queryIntentActivities(intent, PackageManager.GET_META_DATA)
        }

        val appList = mutableListOf<String>()
        // Extract package names from ResolveInfo
        for (resolveInfo in resolveInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            if (!appList.contains(packageName)) {
                appList.add(packageName)
            }
        }

        return appList.map { packageName ->
            val applicationInfo = activity.appInfo(packageName)
            val appName = activity.packageManager.getApplicationLabel(applicationInfo).toString()
            val appIcon = activity.packageManager.getApplicationIcon(applicationInfo)

            AppData(
                appName = appName,
                appPackage = packageName,
                icon = appIcon.toByteArray()
            )
        }
    }

    fun blockApps(activity: Activity, toBeBlocked: Array<String>) {
        val intent = Intent("ACTION_FROM_APP").apply {
            putExtra("KEY", toBeBlocked)
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
        // Create a Bitmap with the same dimensions as the drawable
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)

        // Create a Canvas with the Bitmap
        val canvas = Canvas(bitmap)

        // Draw the Drawable onto the Canvas
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)

        // Convert Bitmap to ByteArray
        ByteArrayOutputStream().use { stream->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        }
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val serviceComponentName = ComponentName(context, AppLaunchAccessibilityService::class.java)

        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName && it.resolveInfo.serviceInfo.name == serviceComponentName.className
        }
    }
}