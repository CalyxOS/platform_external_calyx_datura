/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.applist

import android.net.NetworkPolicyManager
import android.net.NetworkPolicyManager.POLICY_REJECT_ALL
import android.net.NetworkPolicyManager.POLICY_REJECT_CELLULAR
import android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND
import android.net.NetworkPolicyManager.POLICY_REJECT_VPN
import android.net.NetworkPolicyManager.POLICY_REJECT_WIFI
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
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

        // Map of switches to their policy
        val mapOfViewAndPolicy = mapOf(
            R.id.mainSwitch to POLICY_REJECT_ALL,
            R.id.backgroundSwitch to POLICY_REJECT_METERED_BACKGROUND,
            R.id.wifiSwitch to POLICY_REJECT_WIFI,
            R.id.mobileSwitch to POLICY_REJECT_CELLULAR,
            R.id.vpnSwitch to POLICY_REJECT_VPN
        )

        holder.view.apply {
            findViewById<ImageView>(R.id.appIcon).background = app.icon.toDrawable(resources)
            findViewById<TextView>(R.id.appName).text = app.name

            // Expand layout on root view click
            expandLayout(holder.view, app.isExpanded, app.requestsInternetPermission)
            setOnClickListener {
                if (it.isVisible && app.requestsInternetPermission) {
                    currentList.find { a -> a.packageName == app.packageName }?.isExpanded =
                        !app.isExpanded

                    expandLayout(holder.view, app.isExpanded, app.requestsInternetPermission)
                }
            }

            // Switches, Checked/0 == Allowed to connect to internet (default)
            mapOfViewAndPolicy.forEach { (viewID, policy) ->
                findViewById<MaterialSwitch>(viewID).apply {
                    setOnCheckedChangeListener(null)
                    isEnabled = app.requestsInternetPermission
                    isChecked =
                        (networkPolicyManager.getUidPolicy(app.uid) and policy) == 0 &&
                        app.requestsInternetPermission

                    setOnCheckedChangeListener { view, isChecked ->
                        if (view.isVisible) {
                            if (isChecked) {
                                networkPolicyManager.removeUidPolicy(app.uid, policy)
                            } else {
                                networkPolicyManager.addUidPolicy(app.uid, policy)
                            }

                            // Reflect appropriate settings status
                            updateSettingsMode(
                                holder.view,
                                mapOfViewAndPolicy.keys,
                                !app.requestsInternetPermission
                            )
                        }
                    }
                }
            }

            updateSettingsMode(this, mapOfViewAndPolicy.keys, !app.requestsInternetPermission)
        }
    }

    private fun expandLayout(rootView: View, expand: Boolean, canExpand: Boolean) {
        rootView.apply {
            val expandButton = findViewById<MaterialButton>(R.id.expandButton)
            expandButton.isClickable = false

            if (!canExpand) {
                // This view can't be expanded, remove drop down arrow and exit
                expandButton.isEnabled = false
                return
            }

            expandButton.isEnabled = true
            findViewById<LinearLayout>(R.id.expandLayout).apply {
                if (!expand) {
                    expandButton.icon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_down)
                    this.visibility = View.GONE
                } else {
                    expandButton.icon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_up)
                    this.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateSettingsMode(rootView: View, switches: Set<Int>, forceDefault: Boolean) {
        rootView.apply {
            val settingsMode = findViewById<TextView>(R.id.settingsMode)

            if (switches.all { findViewById<MaterialSwitch>(it).isChecked } || forceDefault) {
                settingsMode.text = context.getString(R.string.default_settings)
            } else {
                settingsMode.text = context.getString(R.string.custom_settings)
            }
        }
    }
}
