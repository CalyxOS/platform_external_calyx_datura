/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.applist

import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.calyxos.datura.R

@AndroidEntryPoint(Fragment::class)
class AppListFragment : Hilt_AppListFragment(R.layout.fragment_app_list)
