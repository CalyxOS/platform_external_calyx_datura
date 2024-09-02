/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.main

import android.os.Bundle
import android.os.UserManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.AndroidEntryPoint
import org.calyxos.datura.R
import org.calyxos.datura.databinding.ActivityMainBinding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Adjust root view's paddings for edgeToEdge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.setPadding(insets.left, insets.top, insets.right, 0)
            WindowInsetsCompat.CONSUMED
        }

        setContentView(binding.root)

        if (getSystemService(UserManager::class.java)?.isManagedProfile == true) {
            navigateToWorkFragment()
        }
    }

    private fun navigateToWorkFragment() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.navigation_resource, true)
            .build()
        navOptions.shouldLaunchSingleTop()

        navController.navigate(R.id.workFragment, null, navOptions)
    }
}
