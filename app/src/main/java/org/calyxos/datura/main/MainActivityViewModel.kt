/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.main

import android.annotation.SuppressLint
import android.content.Context
import android.os.INetworkManagementService
import android.os.StrictMode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.calyxos.datura.models.App
import org.calyxos.datura.models.DaturaItem
import org.calyxos.datura.models.Sort
import org.calyxos.datura.models.Type
import org.calyxos.datura.utils.CommonUtils
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak") // false positive, see https://github.com/google/dagger/issues/3253
class MainActivityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkManagementService: INetworkManagementService
) : ViewModel() {

    var sort = Sort.NAME

    private val _appList = MutableStateFlow(emptyList<DaturaItem>())
    val appList = _appList.asStateFlow()

    init {
        fetchAppList()
    }

    fun fetchAppList() {
        _appList.value = CommonUtils.getAllPackagesWithHeader(context)
        if (sort != Sort.NAME) sortAppList(sort)
    }

    fun updateAppList(list: List<DaturaItem>) {
        _appList.value = list
    }

    fun getFilteredAppList(text: String): List<DaturaItem> {
        return if (text.isNotBlank()) {
            _appList.value.filter { it.type == Type.APP && (it as App).name.contains(text, true) }
        } else {
            emptyList()
        }
    }

    fun sortAppList(sort: Sort) {
        if (sort == Sort.LAST_USED) {
            _appList.value = _appList.value.toMutableList().apply {
                val systemHeader = this.indexOfLast { it.type == Type.HEADER }

                // Installed apps
                if (this.filterIsInstance<App>().any { !it.systemApp }) {
                    subList(1, systemHeader - 1).sortByDescending { a -> (a as App).lastTimeUsed }
                }

                // System apps
                if (this.filterIsInstance<App>().any { it.systemApp }) {
                    subList(
                        systemHeader + 1,
                        this.size
                    ).sortByDescending { a -> (a as App).lastTimeUsed }
                }
            }
        } else {
            _appList.value = _appList.value.toMutableList().apply {
                val systemHeader = this.indexOfLast { it.type == Type.HEADER }

                // Installed apps
                if (this.filterIsInstance<App>().any { !it.systemApp }) {
                    subList(
                        1,
                        systemHeader - 1
                    ).sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { (it as App).name })
                }

                // System apps
                if (this.filterIsInstance<App>().any { it.systemApp }) {
                    subList(
                        systemHeader + 1,
                        this.size
                    ).sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { (it as App).name })
                }
            }
        }
    }

    fun resetPerAppClearTextStatus() {
        viewModelScope.launch(NonCancellable) {
            _appList.value.filter {
                it.type == Type.APP &&
                    (it as App).cleartextNetworkPolicy == StrictMode.NETWORK_POLICY_ACCEPT
            }.forEach {
                networkManagementService.setUidCleartextNetworkPolicy(
                    (it as App).uid, StrictMode.NETWORK_POLICY_INVALID
                )
            }
        }
    }
}
