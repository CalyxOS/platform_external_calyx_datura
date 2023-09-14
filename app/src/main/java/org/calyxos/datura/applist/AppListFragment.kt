/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.applist

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.calyxos.datura.R
import org.calyxos.datura.main.MainActivityViewModel
import org.calyxos.datura.models.Sort
import javax.inject.Inject

@AndroidEntryPoint(Fragment::class)
class AppListFragment : Hilt_AppListFragment(R.layout.fragment_app_list) {

    private val viewModel: MainActivityViewModel by activityViewModels()

    @Inject
    lateinit var appListRVAdapter: AppListRVAdapter

    @Inject
    lateinit var searchAppListRVAdapter: AppListRVAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recycler View
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.appList.collect { appListRVAdapter.submitList(it) }
        }
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView).apply {
            adapter = appListRVAdapter
        }

        // Search View
        val searchView = view.findViewById<SearchView>(R.id.searchView)
        val searchBar = view.findViewById<SearchBar>(R.id.searchBar)

        searchBar.apply {
            inflateMenu(R.menu.menu_main)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.about -> {
                        findNavController().navigate(R.id.aboutFragment)
                    }
                    R.id.sort -> {
                        if (viewModel.sort == Sort.NAME) {
                            viewModel.sortAppList(Sort.LAST_USED)
                            recyclerView.post { recyclerView.scrollToPosition(0) }
                            it.title = getString(R.string.sort_by_name)
                            viewModel.sort = Sort.LAST_USED
                        } else {
                            viewModel.sortAppList(Sort.NAME)
                            recyclerView.post { recyclerView.scrollToPosition(0) }
                            it.title = getString(R.string.sort_by_last_used)
                            viewModel.sort = Sort.NAME
                        }
                    }
                }
                true
            }
        }

        searchView.setupWithSearchBar(searchBar)
        view.findViewById<RecyclerView>(R.id.searchRecyclerView).adapter = searchAppListRVAdapter

        searchView.editText.addTextChangedListener {
            searchAppListRVAdapter.submitList(viewModel.getFilteredAppList(it.toString()))
        }

        // Handle back press when search view is visible
        activity?.onBackPressedDispatcher?.addCallback(this) {
            if (searchView.isShowing) {
                searchView.hide()
            } else {
                activity?.finish()
            }
        }
    }
}
