/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.utils

import android.Manifest
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import androidx.core.graphics.drawable.toBitmap
import org.calyxos.datura.R
import org.calyxos.datura.models.App
import org.calyxos.datura.models.DaturaItem
import org.calyxos.datura.models.Header
import java.util.Calendar

object CommonUtils {

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
        val packageList = packageManager.getInstalledPackages(
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
        ).filter { Process.isApplicationUid(it.applicationInfo.uid) }

        packageList.forEach { packageInfo ->
            val app = App(
                packageInfo.applicationInfo.loadLabel(packageManager).toString(),
                packageInfo.packageName,
                packageInfo.applicationInfo.loadIcon(packageManager).toBitmap(96, 96),
                packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                packageInfo.applicationInfo.uid,
                packageInfo.requestedPermissions?.contains(Manifest.permission.INTERNET) ?: false,
                false,
                usageStatsList.firstOrNull { it.packageName == packageInfo.packageName }?.lastTimeUsed ?: 0L
            )
            applicationList.add(app)
        }

        // Filter out system apps without internet permission
        // https://review.calyxos.org/c/CalyxOS/platform_packages_apps_Firewall/+/7295
        applicationList.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        return applicationList.filterNot { it.systemApp && !it.requestsInternetPermission }
    }

    private fun getUsageStats(context: Context): List<UsageStats> {
        val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
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
}
