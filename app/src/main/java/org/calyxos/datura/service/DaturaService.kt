/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import lineageos.providers.LineageSettings
import org.calyxos.datura.models.MinimalApp
import org.calyxos.datura.utils.CommonUtils
import org.calyxos.datura.utils.NotificationUtil

@AndroidEntryPoint
class DaturaService : Service() {

    companion object {
        const val ACTION_ALLOW_INTERNET_ACCESS = "org.calyxos.datura.ACTION_ALLOW_INTERNET_ACCESS"
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var userManager: UserManager
    private val TAG = DaturaService::class.java.simpleName

    private val packageReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null) {
                val uid = intent.getIntExtra(Intent.EXTRA_UID, Process.INVALID_UID)
                val userHandle = UserHandle.getUserHandleForUid(uid)
                if (userHandle in userManager.userProfiles) {
                    val packageName = intent.data!!.encodedSchemeSpecificPart

                    when (intent.action) {
                        Intent.ACTION_PACKAGE_ADDED -> {
                            val app = getMinimalApp(packageName, uid)
                            val isUpdate = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                            if (!isUpdate && app?.requestsInternetPermission == true) {
                                if (LineageSettings.Secure.getInt(
                                        context.contentResolver,
                                        LineageSettings.Secure.DEFAULT_RESTRICT_NETWORK_DATA,
                                        0
                                    ) == 1
                                ) {
                                    onPackageInstalled(context, app)
                                }
                            }
                        }

                        Intent.ACTION_PACKAGE_REMOVED -> onPackageRemoved(packageName, uid)

                        else -> Log.i(TAG, "Got an unhandled action")
                    }
                }
            }
        }

        private fun onPackageInstalled(context: Context, app: MinimalApp) {
            Log.i(TAG, "Auto-denying internet access for ${app.uid}")
            notificationManager.notify(
                app.packageName.hashCode() + app.uid,
                NotificationUtil.getNewAppNotification(context, app)
            )
        }

        private fun onPackageRemoved(packageName: String, uid: Int) {
            notificationManager.cancel(packageName.hashCode() + uid)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(NotificationManager::class.java)!!
        notificationManager.createNotificationChannels(
            NotificationUtil.getNotificationChannels(this)
        )
        userManager = getSystemService(UserManager::class.java)!!

        registerReceiverForAllUsers(
            packageReceiver,
            CommonUtils.broadcastIntentFilter,
            null,
            null
        )
    }

    @SuppressLint("WrongConstant")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            100,
            NotificationUtil.getFGSNotification(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    private fun getMinimalApp(packageName: String, uid: Int): MinimalApp? {
        val packageInfo = packageManager.getPackageInfoAsUser(
            packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            UserHandle.getUserId(uid)
        )

        if (!Process.isApplicationUid(packageInfo.applicationInfo?.uid ?: Process.INVALID_UID)) {
            Log.i(TAG, "$packageName is not an app")
            return null
        }

        val requestsInternetPerm =
            packageInfo.requestedPermissions?.contains(Manifest.permission.INTERNET) ?: false

        return MinimalApp(
            name = packageInfo.applicationInfo!!.loadLabel(packageManager).toString(),
            packageName = packageInfo.packageName,
            icon = CommonUtils.getIconForPackage(packageManager, packageInfo),
            uid = uid,
            requestsInternetPermission = requestsInternetPerm
        )
    }
}
