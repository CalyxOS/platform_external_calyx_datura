/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.models

import android.graphics.Bitmap
import android.os.StrictMode

data class App(
    val name: String = String(),
    val packageName: String = String(),
    val icon: Bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.RGB_565),
    var systemApp: Boolean = false,
    val uid: Int = -1,
    val requestsInternetPermission: Boolean = false,
    var isExpanded: Boolean = false,
    val lastTimeUsed: Long = 0,
    var cleartextNetworkPolicy: Int = StrictMode.NETWORK_POLICY_INVALID,
    override val type: Type = Type.APP
) : DaturaItem
