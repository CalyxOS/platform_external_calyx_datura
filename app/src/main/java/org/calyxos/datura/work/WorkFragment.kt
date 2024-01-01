/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.work

import android.content.ComponentName
import android.content.pm.CrossProfileApps
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import org.calyxos.datura.R
import org.calyxos.datura.databinding.FragmentWorkBinding

class WorkFragment : Fragment(R.layout.fragment_work) {

    private var _binding: FragmentWorkBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWorkBinding.bind(view)

        binding.switchProfile.apply {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
