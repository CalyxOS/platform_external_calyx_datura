/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.applist

import android.net.NetworkPolicyManager
import android.net.NetworkPolicyManager.POLICY_REJECT_ALL
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import org.calyxos.datura.R
import org.calyxos.datura.models.App
import javax.inject.Inject
import javax.inject.Singleton

class AppListRVAdapter @Inject constructor(
    appListDiffUtil: AppListDiffUtil,
    private val networkPolicyManager: NetworkPolicyManager
) : ListAdapter<App, AppListRVAdapter.ViewHolder>(appListDiffUtil) {

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    @Singleton
    class AppListDiffUtil @Inject constructor() : DiffUtil.ItemCallback<App>() {

        override fun areItemsTheSame(oldItem: App, newItem: App): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: App, newItem: App): Boolean {
            return when {
                oldItem.icon != newItem.icon -> false
                oldItem.name != newItem.name -> false
                oldItem.packageName != newItem.packageName -> false
                oldItem.systemApp != newItem.systemApp -> false
                oldItem.uid != newItem.uid -> false
                else -> true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_view_app_list, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = getItem(position)

        holder.view.apply {
            findViewById<ImageView>(R.id.appIcon).background = app.icon.toDrawable(resources)
            findViewById<TextView>(R.id.appName).text = app.name
            findViewById<TextView>(R.id.appPkgName).text = app.packageName

            // Main Switch (POLICY_REJECT_ALL)
            // Checked/0 == Allowed to connect to internet (default)
            findViewById<MaterialSwitch>(R.id.mainSwitch).apply {
                isEnabled = app.requestsInternetPermission
                isChecked =
                    (networkPolicyManager.getUidPolicy(app.uid) and POLICY_REJECT_ALL) == 0 &&
                    app.requestsInternetPermission

                setOnCheckedChangeListener { _, isChecked ->
                    if (isVisible) {
                        if (isChecked) {
                            networkPolicyManager.removeUidPolicy(app.uid, POLICY_REJECT_ALL)
                        } else {
                            networkPolicyManager.addUidPolicy(app.uid, POLICY_REJECT_ALL)
                        }
                    }
                }
            }
        }
    }
}
