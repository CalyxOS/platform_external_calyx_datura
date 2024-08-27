/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.utils

import android.Manifest
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.graphics.Bitmap
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import org.calyxos.datura.R
import org.calyxos.datura.models.App
import org.calyxos.datura.models.DaturaItem
import org.calyxos.datura.models.Header
import java.util.Calendar

object CommonUtils {

    const val PREFERENCE_DEFAULT_INTERNET = "PREFERENCE_DEFAULT_INTERNET"
    const val PREFERENCE_NOTIFICATIONS = "PREFERENCE_NOTIFICATIONS"

    private const val TAG = "CommonUtils"

    val broadcastIntentFilter: IntentFilter
        get() = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }

    fun getAllPackagesWithHeader(context: Context): List<DaturaItem> {
        val appList = getAllPackages(context).toMutableList()
        val daturaItemList = mutableListOf<DaturaItem>()

        daturaItemList.apply {
            add(0, Header(context.getString(R.string.installed_apps)))
            addAll(appList.filter { !it.systemApp })
            add(size, Header(context.getString(R.string.system_apps)))
            addAll(appList.filter { it.systemApp })
        }
        return daturaItemList
    }

    private fun getAllPackages(context: Context): List<App> {
        val applicationList = mutableListOf<App>()
        val packageManager = context.packageManager

        val usageStatsList = getUsageStats(context)
        val packageList = getAppsInstalledForAllUsers(context)

        packageList.forEach {
            val systemApp = it.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM != 0
            val requestsInternetPermission =
                it.requestedPermissions?.contains(Manifest.permission.INTERNET) ?: false

            // Filter out system apps without internet permission
            // https://review.calyxos.org/c/CalyxOS/platform_packages_apps_Firewall/+/7295
            if (systemApp && !requestsInternetPermission) return@forEach

            val app = App(
                it.applicationInfo!!.loadLabel(packageManager).toString(),
                it.packageName,
                getIconForPackage(packageManager, it),
                systemApp,
                it.applicationInfo!!.uid,
                requestsInternetPermission,
                false,
                usageStatsList.firstOrNull { u -> u.packageName == it.packageName }?.lastTimeUsed
                    ?: 0L
            )
            applicationList.add(app)
        }

        applicationList.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        return applicationList
    }

    fun getIconForPackage(
        packageManager: PackageManager,
        packageInfo: PackageInfo
    ): Bitmap {
        val icon = packageInfo.applicationInfo!!.loadIcon(packageManager)
        val userHandle = UserHandle.getUserHandleForUid(packageInfo.applicationInfo!!.uid)
        val badgedIcon = if (icon.intrinsicWidth > 0 && icon.intrinsicHeight > 0) {
            packageManager.getUserBadgedIcon(icon, userHandle)
        } else {
            Log.w(TAG, "Using default activity icon for ${packageInfo.packageName}")
            packageManager.getUserBadgedIcon(packageManager.defaultActivityIcon, userHandle)
        }
        return badgedIcon.toBitmap(96, 96)
    }

    private fun getUsageStats(context: Context): List<UsageStats> {
        val usageStatsManager =
            context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.YEAR, -1)
        val startTime = calendar.timeInMillis
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )
    }

    // Lint complains about missing methods but they are available
    private fun getAppsInstalledForAllUsers(context: Context): List<PackageInfo> {
        val packages = mutableListOf<PackageInfo>()
        val userManager = context.getSystemService(UserManager::class.java)!!
        UserHandle.fromUserHandles(userManager.userProfiles).forEach { userID ->
            packages.addAll(
                context.packageManager.getInstalledPackagesAsUser(
                    PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
                    userID
                ).filter {
                    Process.isApplicationUid(it.applicationInfo?.uid ?: Process.INVALID_UID)
                }
            )
        }
        return packages
    }
}
