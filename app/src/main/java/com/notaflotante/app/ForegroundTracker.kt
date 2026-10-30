package com.notaflotante.app

/** Only a positive, current HOME resume may expose notes; unknown state stays hidden. */
internal class ForegroundTracker {
    var packageName: String? = null
        private set
    private var activity: String? = null
    private var lastTimestamp = Long.MIN_VALUE

    fun event(timestamp: Long, packageName: String, className: String?, resumed: Boolean) {
        if (timestamp < lastTimestamp) return
        lastTimestamp = timestamp
        if (resumed) {
            this.packageName = packageName
            activity = className
        } else if (this.packageName == packageName && (className == null || activity == className)) {
            this.packageName = null
            activity = null
        }
    }

    fun isHome(homePackage: String?, ownPackage: String): Boolean =
        homePackage != null && homePackage != ownPackage && packageName == homePackage
}
