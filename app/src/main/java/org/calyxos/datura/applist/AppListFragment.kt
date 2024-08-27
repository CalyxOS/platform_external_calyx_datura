/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.applist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.calyxos.datura.R
import org.calyxos.datura.databinding.FragmentAppListBinding
import org.calyxos.datura.main.MainActivityViewModel
import org.calyxos.datura.models.App
import org.calyxos.datura.models.DaturaItem
import org.calyxos.datura.models.Sort
import org.calyxos.datura.utils.CommonUtils
import javax.inject.Inject

@AndroidEntryPoint
class AppListFragment : Fragment(R.layout.fragment_app_list) {

    private val TAG = AppListFragment::class.java.simpleName

    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainActivityViewModel by activityViewModels()

    @Inject
    lateinit var appListRVAdapter: AppListRVAdapter

    @Inject
    lateinit var searchAppListRVAdapter: AppListRVAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAppListBinding.bind(view)

        // Register receiver to update list on install/uninstall
        activity?.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        Intent.ACTION_PACKAGE_ADDED,
                        Intent.ACTION_PACKAGE_REMOVED -> viewModel.fetchAppList()

                        else -> Log.i(TAG, "Got an unhandled Intent ${intent?.action}")
                    }
                }
            },
            CommonUtils.broadcastIntentFilter,
            Context.RECEIVER_NOT_EXPORTED
        )

        // Get target UID if provided to activity, e.g. by link in Settings
        val uid: Int = activity?.intent?.getIntExtra(Intent.EXTRA_UID, -1) ?: -1

        // Recycler View
        binding.recyclerView.adapter = appListRVAdapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.appList.collect { list ->
                appListRVAdapter.submitList(list)
                if (uid != -1) {
                    scrollToAndExpandUid(binding.recyclerView, uid, list)
                }
            }
        }

        // Search View
        binding.searchBar.apply {
            inflateMenu(R.menu.menu_main)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.about -> {
                        findNavController().navigate(R.id.aboutFragment)
                    }
                    R.id.sort -> {
                        if (viewModel.sort == Sort.NAME) {
                            viewModel.sortAppList(Sort.LAST_USED)
                            binding.recyclerView.post { binding.recyclerView.scrollToPosition(0) }
                            it.title = getString(R.string.sort_by_name)
                            viewModel.sort = Sort.LAST_USED
                        } else {
                            viewModel.sortAppList(Sort.NAME)
                            binding.recyclerView.post { binding.recyclerView.scrollToPosition(0) }
                            it.title = getString(R.string.sort_by_last_used)
                            viewModel.sort = Sort.NAME
                        }
                    }
                    R.id.settings -> {
                        findNavController().navigate(R.id.settingsFragment)
                    }
                }
                true
            }
        }

        binding.searchView.setupWithSearchBar(binding.searchBar)
        binding.searchRecyclerView.adapter = searchAppListRVAdapter

        binding.searchView.editText.addTextChangedListener {
            searchAppListRVAdapter.submitList(viewModel.getFilteredAppList(it.toString()))
        }

        // Handle back press when search view is visible
        activity?.onBackPressedDispatcher?.addCallback(this) {
            if (binding.searchView.isShowing) {
                binding.searchView.hide()
            } else {
                activity?.finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun scrollToAndExpandUid(recyclerView: RecyclerView, uid: Int, list: List<DaturaItem>) {
        var foundApp: App? = null
        val uidPosition: Int = list.withIndex().firstOrNull { indexedItem ->
            if (indexedItem.value is App) {
                val app: App = (indexedItem.value as App)
                if (app.uid == uid) {
                    foundApp = app
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }?.index ?: -1
        if (uidPosition != -1) {
            if (foundApp?.requestsInternetPermission == true) {
                foundApp?.isExpanded = true
            }
            recyclerView.scrollToPosition(uidPosition)
        }
    }
}
