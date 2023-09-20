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
import org.calyxos.datura.models.DaturaItem
import org.calyxos.datura.models.Header
import org.calyxos.datura.models.Type
import javax.inject.Inject
import javax.inject.Singleton

class AppListRVAdapter @Inject constructor(
    daturaItemDiffUtil: DaturaItemDiffUtil,
    private val networkPolicyManager: NetworkPolicyManager
) : ListAdapter<DaturaItem, RecyclerView.ViewHolder>(daturaItemDiffUtil) {

    inner class AppViewHolder(val view: View) : RecyclerView.ViewHolder(view)
    inner class HeaderViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    @Singleton
    class DaturaItemDiffUtil @Inject constructor() : DiffUtil.ItemCallback<DaturaItem>() {

        override fun areItemsTheSame(oldItem: DaturaItem, newItem: DaturaItem): Boolean {
            return when (oldItem.type) {
                newItem.type -> {
                    if (oldItem.type == Type.APP) {
                        (oldItem as App).packageName == (newItem as App).packageName
                    } else {
                        (oldItem as Header).name == (newItem as Header).name
                    }
                }
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: DaturaItem, newItem: DaturaItem): Boolean {
            return when (oldItem.type) {
                Type.APP -> {
                    val oldApp = (oldItem as App)
                    val newApp = (newItem as App)
                    when {
                        oldApp.icon != newApp.icon -> false
                        oldApp.name != newApp.name -> false
                        oldApp.packageName != newApp.packageName -> false
                        oldApp.systemApp != newApp.systemApp -> false
                        oldApp.uid != newApp.uid -> false
                        oldApp.requestsInternetPermission != newApp.requestsInternetPermission -> false
                        oldApp.isExpanded != newApp.isExpanded -> false
                        else -> true
                    }
                }
                Type.HEADER -> {
                    (oldItem as Header).name == (newItem as Header).name
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            Type.APP.ordinal -> {
                AppViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.recycler_view_app_list, parent, false)
                )
            }
            else -> {
                HeaderViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.recycler_view_header_list, parent, false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AppViewHolder) {
            onBindAppViewHolder(holder, position)
        } else if (holder is HeaderViewHolder) {
            onBindHeaderViewHolder(holder, position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return currentList[position].type.ordinal
    }

    private fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int) {
        val header = getItem(position) as Header

        holder.view.findViewById<TextView>(R.id.header).text = header.name
    }

    private fun onBindAppViewHolder(holder: AppViewHolder, position: Int) {
        val app = getItem(position) as App

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
                    (currentList[position] as App).isExpanded = !app.isExpanded
                    expandLayout(holder.view, app.isExpanded, true)
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
                            updateSettingsText(
                                holder.view,
                                mapOfViewAndPolicy.keys,
                                app.requestsInternetPermission
                            )
                        }
                    }
                }
            }

            updateSettingsText(this, mapOfViewAndPolicy.keys, app.requestsInternetPermission)
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

    private fun updateSettingsText(rootView: View, switches: Set<Int>, reqInternetPerm: Boolean) {
        rootView.apply {
            val settingsMode = findViewById<TextView>(R.id.settingsMode)

            if (reqInternetPerm) {
                if (switches.all { findViewById<MaterialSwitch>(it).isChecked }) {
                    settingsMode.text = context.getString(R.string.default_settings)
                } else {
                    settingsMode.text = context.getString(R.string.custom_settings)
                }
            } else {
                settingsMode.text = String()
            }
        }
    }
}
