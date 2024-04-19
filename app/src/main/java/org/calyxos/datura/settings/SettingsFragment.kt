/*
 * SPDX-FileCopyrightText: 2024 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.os.UserManager
import android.provider.Settings
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.android.net.module.util.ConnectivitySettingsUtils.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME
import com.android.net.module.util.ConnectivitySettingsUtils.getPrivateDnsMode
import dagger.hilt.android.AndroidEntryPoint
import lineageos.providers.LineageSettings
import org.calyxos.datura.R
import org.calyxos.datura.databinding.FragmentSettingsBinding
import org.calyxos.datura.service.DaturaService
import org.calyxos.datura.utils.CommonUtils.PREFERENCE_CLEARTEXT
import org.calyxos.datura.utils.CommonUtils.PREFERENCE_DEFAULT_INTERNET
import org.calyxos.datura.utils.CommonUtils.PREFERENCE_NOTIFICATIONS

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)

        findPreference<SwitchPreferenceCompat>(PREFERENCE_DEFAULT_INTERNET)?.let {
            it.isChecked = LineageSettings.Secure.getInt(
                context?.contentResolver,
                LineageSettings.Secure.DEFAULT_RESTRICT_NETWORK_DATA,
                0
            ) == 0
            it.setOnPreferenceChangeListener { _, newValue ->
                if (!newValue.toString().toBoolean()) {
                    requireContext().startForegroundService(
                        Intent(
                            context,
                            DaturaService::class.java
                        )
                    )
                } else {
                    requireContext().stopService(Intent(context, DaturaService::class.java))
                }
                LineageSettings.Secure.putInt(
                    context?.contentResolver,
                    LineageSettings.Secure.DEFAULT_RESTRICT_NETWORK_DATA,
                    if (newValue as Boolean) {
                        0
                    } else {
                        1
                    }
                )
            }
        }

        findPreference<Preference>(PREFERENCE_NOTIFICATIONS)?.apply {
            setOnPreferenceClickListener {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).also {
                    it.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    startActivity(it)
                }
                true
            }
        }

        findPreference<SwitchPreferenceCompat>(PREFERENCE_CLEARTEXT)?.let {
            it.isVisible = requireContext().getSystemService(UserManager::class.java).isSystemUser
            it.isChecked = isGlobalClearTextEnabled(requireContext())
            it.isEnabled = isGlobalClearTextPrefEnabled(requireContext())

            it.setOnPreferenceChangeListener { _, newValue ->
                val result = LineageSettings.Global.putInt(
                    context?.contentResolver,
                    LineageSettings.Global.CLEARTEXT_NETWORK_POLICY,
                    if (newValue as Boolean) {
                        StrictMode.NETWORK_POLICY_REJECT
                    } else {
                        StrictMode.NETWORK_POLICY_INVALID
                    }
                )
                it.isEnabled = isGlobalClearTextPrefEnabled(requireContext())
                result
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findPreference<SwitchPreferenceCompat>(PREFERENCE_CLEARTEXT)?.apply {
            isEnabled = isGlobalClearTextPrefEnabled(context)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun isGlobalClearTextPrefEnabled(ctx: Context): Boolean {
        return isGlobalClearTextEnabled(ctx) ||
            getPrivateDnsMode(ctx) == PRIVATE_DNS_MODE_PROVIDER_HOSTNAME
    }

    private fun isGlobalClearTextEnabled(context: Context): Boolean {
        return LineageSettings.Global.getInt(
            context.contentResolver,
            LineageSettings.Global.CLEARTEXT_NETWORK_POLICY,
            StrictMode.NETWORK_POLICY_INVALID
        ) == StrictMode.NETWORK_POLICY_REJECT
    }
}
