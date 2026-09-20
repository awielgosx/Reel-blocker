package com.reelblocker.tracking

/** The apps this tool tracks and (in a later milestone) blocks reels/shorts within. */
enum class MonitoredApp(val packageName: String, val displayName: String) {
    INSTAGRAM("com.instagram.android", "Instagram"),
    FACEBOOK("com.facebook.katana", "Facebook"),
    YOUTUBE("com.google.android.youtube", "YouTube");

    companion object {
        private val byPackage = entries.associateBy { it.packageName }

        fun fromPackageName(packageName: String?): MonitoredApp? = byPackage[packageName]

        val allPackageNames: List<String> = entries.map { it.packageName }
    }
}
