/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.models

import android.graphics.Bitmap

data class App(
    val name: String = String(),
    val packageName: String = String(),
    val icon: Bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.RGB_565),
    var systemApp: Boolean = false,
    val uid: Int = -1,
    val requestsInternetPermission: Boolean = false
)
