/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.utils

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import androidx.core.graphics.drawable.toBitmap
import org.calyxos.datura.models.App

object CommonUtils {

    fun getAllPackages(context: Context): List<App> {
        val applicationList = mutableListOf<App>()
        val packageManager = context.packageManager

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
                packageInfo.requestedPermissions?.contains(Manifest.permission.INTERNET) ?: false
            )
            applicationList.add(app)
        }

        // Filter out system apps without internet permission
        // https://review.calyxos.org/c/CalyxOS/platform_packages_apps_Firewall/+/7295
        applicationList.sortBy { it.name }
        return applicationList.filterNot { it.systemApp && !it.requestsInternetPermission }
    }
}
