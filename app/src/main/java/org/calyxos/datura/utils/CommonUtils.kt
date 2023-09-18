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
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.content.pm.UserInfo
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
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
        val packageList = getAppsInstalledForAllUsers(context)
            .filter { Process.isApplicationUid(it.uid) }

        packageList.forEach {
            val pkgInfo = packageManager.getPackageInfo(
                it.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
            val systemApp = it.flags and ApplicationInfo.FLAG_SYSTEM != 0
            val requestsInternetPermission =
                pkgInfo.requestedPermissions?.contains(Manifest.permission.INTERNET) ?: false

            // Filter out system apps without internet permission
            // https://review.calyxos.org/c/CalyxOS/platform_packages_apps_Firewall/+/7295
            if (systemApp && !requestsInternetPermission) return@forEach

            val app = App(
                it.loadLabel(packageManager).toString(),
                it.packageName,
                packageManager.getUserBadgedIcon(
                    it.loadIcon(packageManager),
                    UserHandle.getUserHandleForUid(it.uid)
                ).toBitmap(96, 96),
                systemApp,
                it.uid,
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

    // TODO: Drop this and use ASfP or something else that doesn't requires reflection
    private fun getAppsInstalledForAllUsers(context: Context): List<ApplicationInfo> {
        val packages = mutableListOf<ApplicationInfo>()
        val userManager = context.getSystemService(UserManager::class.java)
        userManager.getProfiles(getMyUserId()).forEach {
            packages.addAll(
                context.packageManager.getInstalledApplicationsAsUser(
                    ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
                    it.id
                )
            )
        }
        return packages
    }

    private fun getMyUserId(): Int {
        val types = arrayOf(
            Int::class.javaPrimitiveType
        )
        val method = UserHandle::class.java.getMethod("getUserId", *types)
        val response = method.invoke(this, Process.myUid())
        return response as Int
    }

    private fun UserManager.getProfiles(
        userId: Int
    ): List<UserInfo> {
        val types = arrayOf(
            Int::class.javaPrimitiveType
        )
        val method = this.javaClass.getMethod("userId", *types)
        val response = method.invoke(this, userId)
        return response as List<UserInfo>
    }

    private fun PackageManager.getInstalledApplicationsAsUser(
        flags: ApplicationInfoFlags,
        userId: Int
    ): List<ApplicationInfo> {
        val types = arrayOf(
            ApplicationInfoFlags::class.java,
            Int::class.javaPrimitiveType
        )
        val method = this.javaClass.getMethod("getInstalledApplicationsAsUser", *types)
        val response = method.invoke(this, flags, userId)
        return response as List<ApplicationInfo>
    }
}
