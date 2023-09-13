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
import com.google.android.material.appbar.MaterialToolbar
import org.calyxos.datura.R

class AboutFragment : Fragment(R.layout.fragment_about) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar
        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Version
        view.apply {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
            findViewById<TextView>(R.id.appVersion).text = getString(
                R.string.version,
                packageInfo.versionName
            )
        }

        // Make required views clickable
        val linkViews = listOf(
            R.id.contributors_desc,
            R.id.contributing_orgs_desc,
            R.id.license
        )
        view.apply {
            linkViews.forEach {
                findViewById<TextView>(it).apply {
                    movementMethod = LinkMovementMethod.getInstance()
                    isClickable = true
                    removeUnderlineFromLinks()
                }
            }
        }
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
