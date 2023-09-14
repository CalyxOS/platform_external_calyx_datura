/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.main

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.calyxos.datura.models.App
import org.calyxos.datura.utils.CommonUtils
import org.calyxos.datura.models.Sort
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class MainActivityViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    var sort = Sort.NAME

    private val _appList = MutableStateFlow(emptyList<App>())
    val appList = _appList.asStateFlow()

    init {
        _appList.value = getAppList()
    }

    private fun getAppList(): List<App> {
        return CommonUtils.getAllPackages(context)
    }

    fun getFilteredAppList(text: String): List<App> {
        return if (text.isNotBlank()) {
            _appList.value.filter { it.name.contains(text, true) }
        } else {
            emptyList()
        }
    }

    fun sortAppList(sort: Sort) {
        if (sort == Sort.LAST_USED) {
            _appList.value = _appList.value.sortedByDescending { a -> a.lastTimeUsed }
        } else {
            _appList.value =
                _appList.value.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        }
    }
}
