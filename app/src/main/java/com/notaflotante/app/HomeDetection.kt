package com.notaflotante.app

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process

internal fun Context.hasUsageAccess(): Boolean {
    val ops = getSystemService(AppOpsManager::class.java)
    val mode = if (Build.VERSION.SDK_INT >= 29)
        ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
    else {
        @Suppress("DEPRECATION")
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

internal fun Context.originalHomePackage(): String? {
    @Suppress("DEPRECATION")
    val info = packageManager.resolveActivity(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY
    )?.activityInfo ?: return null
    // A system resolver is not an actual selected home.
    @Suppress("DEPRECATION")
    val homes = packageManager.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY
    )
    return info.packageName.takeIf { pkg -> homes.any { it.activityInfo.packageName == pkg } && pkg != packageName }
}

internal class HomeDetection(private val context: Context) {
    private val tracker = ForegroundTracker()
    private var readUntil = System.currentTimeMillis() - 60_000
    private var lastHomeCheck = 0L
    private var homePackage: String? = null

    /** Run on IO. The overlap handles events delivered slightly late by UsageStats. */
    fun isHomeVisible(): Boolean {
        if (!context.hasUsageAccess()) return false
        val now = System.currentTimeMillis()
        if (now - lastHomeCheck > 3_000 || now < lastHomeCheck) {
            homePackage = context.originalHomePackage()
            lastHomeCheck = now
        }
        val begin = if (now < readUntil) now - 60_000 else readUntil - 2_000
        val events = context.getSystemService(UsageStatsManager::class.java).queryEvents(begin, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> tracker.event(event.timeStamp, event.packageName, event.className, true)
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED ->
                    tracker.event(event.timeStamp, event.packageName, event.className, false)
            }
        }
        readUntil = now
        return tracker.isHome(homePackage, context.packageName)
    }
}
