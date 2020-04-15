/*
 * Copyright (C) 2020. by onlymash <im@fiepi.me>, All rights reserved
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package onlymash.flexbooru.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import onlymash.flexbooru.R
import onlymash.flexbooru.common.Settings.pageLimit
import onlymash.flexbooru.common.Values.BOORU_TYPE_DAN
import onlymash.flexbooru.common.Values.BOORU_TYPE_DAN1
import onlymash.flexbooru.common.Values.BOORU_TYPE_MOE
import onlymash.flexbooru.data.action.ActionArtist
import onlymash.flexbooru.data.model.common.Booru
import onlymash.flexbooru.data.repository.NetworkState
import onlymash.flexbooru.data.repository.artist.ArtistRepositoryImpl
import onlymash.flexbooru.data.repository.isRunning
import onlymash.flexbooru.ui.adapter.ArtistAdapter
import onlymash.flexbooru.ui.viewmodel.ArtistViewModel
import onlymash.flexbooru.ui.viewmodel.getArtistViewModel

private const val ORDER_DATE = "date"
private const val ORDER_UPDATED_AT = "updated_at"
private const val ORDER_NAME = "name"
private const val ORDER_COUNT = "post_count"

class ArtistFragment : SearchBarFragment() {

    private var action: ActionArtist? = null

    private lateinit var artistViewModel: ArtistViewModel
    private lateinit var artistAdapter: ArtistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        artistViewModel = getArtistViewModel(ArtistRepositoryImpl(booruApis))
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun getSearchBarHint(): CharSequence =
        getString(R.string.search_bar_hint_search_artists)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSearchBarTitle(getString(R.string.title_artists))
        artistAdapter = ArtistAdapter {
            artistViewModel.retry()
        }
        mainList.apply {
            layoutManager = LinearLayoutManager(this@ArtistFragment.context, RecyclerView.VERTICAL, false)
            adapter = artistAdapter
        }
        artistViewModel.artists.observe(viewLifecycleOwner, Observer { artistList ->
            artistList?.let {
                artistAdapter.submitList(it)
            }
        })
        artistViewModel.networkState.observe(viewLifecycleOwner, Observer {
            artistAdapter.setNetworkState(it)
            val isRunning = it.isRunning()
            progressBarHorizontal.isVisible = isRunning
            progressBar.isVisible = isRunning && artistAdapter.itemCount == 0
        })
        artistViewModel.refreshState.observe(viewLifecycleOwner, Observer {
            if (it != NetworkState.LOADING) {
                swipeRefresh.isRefreshing = false
            }
        })
        swipeRefresh.setOnRefreshListener {
            artistViewModel.refresh()
        }
    }

    override fun onBooruLoaded(booru: Booru?) {
        if (booru == null) {
            action = null
            artistViewModel.show(null)
            return
        }
        if (action == null) {
            action = ActionArtist(
                booru = booru,
                limit = artistLimit(booru.type),
                order = ORDER_COUNT,
                query = ""
            )
            setSearchBarMenu(when (booru.type) {
                BOORU_TYPE_DAN -> R.menu.artist_dan
                else -> R.menu.artist_moe
            })
        } else {
            action?.let {
                it.booru = booru
                it.limit = artistLimit(booru.type)
            }
        }
        artistViewModel.show(action)
    }

    private fun artistLimit(booruType: Int): Int {
        return when (booruType) {
            BOORU_TYPE_MOE, BOORU_TYPE_DAN1 -> 20
            else -> pageLimit
        }
    }

    override fun onApplySearch(query: String) {
        super.onApplySearch(query)
        action?.let {
            it.query = query
            artistViewModel.show(action)
            artistViewModel.refresh()
        }
    }

    private fun updateActionAndRefresh(action: ActionArtist) {
        artistViewModel.show(action)
        artistViewModel.refresh()
    }

    override fun onMenuItemClick(menuItem: MenuItem) {
        super.onMenuItemClick(menuItem)
        when (menuItem.itemId) {
            R.id.action_artist_order_date -> {
                action?.let{
                    it.order = if (it.booru.type == BOORU_TYPE_DAN) {
                        ORDER_UPDATED_AT
                    } else {
                        ORDER_DATE
                    }
                    updateActionAndRefresh(it)
                }
            }
            R.id.action_artist_order_name -> {
                action?.let{
                    it.order = ORDER_NAME
                    updateActionAndRefresh(it)
                }
            }
            R.id.action_artist_order_count -> {
                action?.let{
                    it.order = ORDER_COUNT
                    updateActionAndRefresh(it)
                }
            }
        }
    }
}