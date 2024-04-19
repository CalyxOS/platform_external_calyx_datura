/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.NetworkPolicyManager
import android.os.INetworkManagementService
import android.os.ServiceManager
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    @Singleton
    @Provides
    fun providesNetworkPolicyManager(@ApplicationContext context: Context): NetworkPolicyManager {
        return NetworkPolicyManager.from(context)
    }

    @Singleton
    @Provides
    fun providesDefaultSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Singleton
    @Provides
    fun providesNetworkManagementService(): INetworkManagementService {
        return INetworkManagementService.Stub.asInterface(
            ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE)
        )
    }
}
