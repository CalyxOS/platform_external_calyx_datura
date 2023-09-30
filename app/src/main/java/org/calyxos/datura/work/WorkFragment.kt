/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.work

import android.content.ComponentName
import android.content.pm.CrossProfileApps
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import org.calyxos.datura.R

class WorkFragment : Fragment(R.layout.fragment_work) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.switchProfile).apply {
            val crossProfileApps = view.context.getSystemService(CrossProfileApps::class.java)
            val targetUser = crossProfileApps.targetUserProfiles.first()

            text = crossProfileApps.getProfileSwitchingLabel(targetUser)
            setOnClickListener {
                crossProfileApps.startMainActivity(
                    ComponentName(
                        view.context.packageName,
                        "${view.context.packageName}.main.MainActivity"
                    ),
                    targetUser
                )
            }
        }
    }
}
