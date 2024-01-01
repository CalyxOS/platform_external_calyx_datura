/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.about

import android.content.pm.PackageManager
import android.os.Bundle
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.calyxos.datura.R
import org.calyxos.datura.databinding.FragmentAboutBinding

class AboutFragment : Fragment(R.layout.fragment_about) {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAboutBinding.bind(view)

        // Toolbar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Version
        val packageInfo = requireContext().packageManager.getPackageInfo(
            requireContext().packageName,
            PackageManager.PackageInfoFlags.of(0)
        )
        binding.appVersion.text = getString(
            R.string.version,
            packageInfo.versionName
        )

        // Make required views clickable
        val linkViews = listOf(
            binding.contributorsDesc,
            binding.contributingOrgsDesc,
            binding.license
        )
        linkViews.forEach {
            it.apply {
                movementMethod = LinkMovementMethod.getInstance()
                isClickable = true
                removeUnderlineFromLinks()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun TextView.removeUnderlineFromLinks() {
        val spannable = SpannableString(text)
        for (urlSpan in spannable.getSpans(0, spannable.length, URLSpan::class.java)) {
            spannable.setSpan(
                object : URLSpan(urlSpan.url) {
                    override fun updateDrawState(textPaint: TextPaint) {
                        super.updateDrawState(textPaint)
                        textPaint.isUnderlineText = false
                    }
                },
                spannable.getSpanStart(urlSpan),
                spannable.getSpanEnd(urlSpan),
                0
            )
        }
        text = spannable
    }
}
