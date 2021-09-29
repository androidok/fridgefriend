/*
 * Copyright 2021 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.fridge.detail

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.swipe.SimpleSwipeCallback
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.detail.databinding.DetailListBinding
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.detail.item.DetailItemViewHolder
import com.pyamsoft.fridge.detail.item.DetailItemViewState
import com.pyamsoft.fridge.detail.item.DetailListAdapter
import com.pyamsoft.fridge.theme.R as R2
import com.pyamsoft.fridge.ui.R as R4
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.ImageTarget
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.R as R3
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.removeAllItemDecorations
import com.pyamsoft.pydroid.util.asDp
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import com.pyamsoft.pydroid.util.tintWith
import io.cabriole.decorator.LinearBoundsMarginDecoration
import io.cabriole.decorator.LinearMarginDecoration
import javax.inject.Inject
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import timber.log.Timber

class DetailList
@Inject
internal constructor(
    owner: LifecycleOwner,
    private val imageLoader: ImageLoader,
    private val theming: ThemeProvider,
    parent: ViewGroup,
    factory: DetailItemComponent.Factory,
) : BaseUiView<DetailViewState, DetailViewEvent.ListEvent, DetailListBinding>(parent) {

  override val viewBinding = DetailListBinding::inflate

  override val layoutRoot by boundView { detailSwipeRefresh }

  private var touchHelper: ItemTouchHelper? = null
  private var modelAdapter: DetailListAdapter? = null

  private var lastScrollPosition = 0

  private var leftBehindLoaded: Loaded? = null
  private var leftBehindDrawable: Drawable? = null

  private var rightBehindLoaded: Loaded? = null
  private var rightBehindDrawable: Drawable? = null

  private val topDecoration = LinearBoundsMarginDecoration(bottomMargin = 0)
  private val bottomDecoration = LinearBoundsMarginDecoration(bottomMargin = 0)

  init {
    doOnInflate {
      binding.detailList.layoutManager =
          LinearLayoutManager(binding.detailList.context).apply {
            isItemPrefetchEnabled = true
            initialPrefetchItemCount = 3
          }
    }

    doOnInflate {
      modelAdapter =
          DetailListAdapter(
              owner = owner,
              factory = factory,
              callback =
                  object : DetailListAdapter.Callback {

                    override fun onIncreaseCount(index: Int) {
                      publish(DetailViewEvent.ListEvent.IncreaseItemCount(index))
                    }

                    override fun onDecreaseCount(index: Int) {
                      publish(DetailViewEvent.ListEvent.DecreaseItemCount(index))
                    }

                    override fun onItemExpanded(index: Int) {
                      publish(DetailViewEvent.ListEvent.ExpandItem(index))
                    }

                    override fun onPresenceChange(index: Int) {
                      publish(DetailViewEvent.ListEvent.ChangeItemPresence(index))
                    }
                  })
      binding.detailList.adapter = modelAdapter
    }

    doOnInflate {
      // Fast Scroll
      FastScrollerBuilder(binding.detailList)
          .useMd2Style()
          .setPopupTextProvider(usingAdapter())
          .build()
    }

    doOnInflate {
      // Offset the refresh indicator so it appears where we expect it to on page
      // start is the "start location"
      // end is the "pulldown distance"
      binding.detailSwipeRefresh.setProgressViewOffset(false, 200, 400)
    }

    doOnInflate {
      binding.detailSwipeRefresh.setOnRefreshListener {
        publish(DetailViewEvent.ListEvent.ForceRefresh)
      }
    }

    doOnInflate { savedInstanceState ->
      val position = savedInstanceState.get(LAST_SCROLL_POSITION) ?: -1
      if (position >= 0) {
        Timber.d("Last scroll position saved at: $position")
        lastScrollPosition = position
      }
    }

    doOnSaveState { outState ->
      val manager = binding.detailList.layoutManager
      if (manager is LinearLayoutManager) {
        val position = manager.findFirstVisibleItemPosition()
        if (position > 0) {
          outState.put(LAST_SCROLL_POSITION, position)
          return@doOnSaveState
        }
      }

      outState.remove<Nothing>(LAST_SCROLL_POSITION)
    }

    doOnInflate {
      val margin = 16.asDp(binding.detailList.context)

      // Standard margin on all items
      LinearMarginDecoration.create(margin = margin).apply {
        binding.detailList.addItemDecoration(this)
      }
    }

    doOnInflate {
      val staticTopDecoration = LinearBoundsMarginDecoration(topMargin = 0)
      binding.detailList
          .doOnApplyWindowInsets { _, insets, _ ->
            val toolbarTopMargin = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            staticTopDecoration.setMargin(top = toolbarTopMargin)
            binding.detailList.invalidateItemDecorations()
          }
          .also { doOnTeardown { it.cancel() } }
      binding.detailList.addItemDecoration(staticTopDecoration)
    }

    doOnInflate {
      binding.detailList.apply {
        addItemDecoration(bottomDecoration)
        addItemDecoration(topDecoration)
      }
    }

    doOnTeardown { binding.detailList.removeAllItemDecorations() }

    doOnTeardown {
      binding.detailList.adapter = null

      touchHelper?.attachToRecyclerView(null)
      binding.detailSwipeRefresh.setOnRefreshListener(null)

      modelAdapter = null
      touchHelper = null
    }

    doOnTeardown { clearLoaded() }
  }

  private fun clearLoaded() {
    leftBehindLoaded?.dispose()
    leftBehindLoaded = null

    rightBehindLoaded?.dispose()
    rightBehindLoaded = null

    leftBehindDrawable = null
    rightBehindDrawable = null
  }

  private fun setupSwipeCallback(state: DetailViewState) {
    val isFresh = state.showing == DetailViewState.Showing.FRESH
    val swipeAwayDeletes = isFresh && state.listItemPresence == NEED
    val swipeAwayRestores = !isFresh && state.listItemPresence == HAVE

    val consumeSwipeDirection = ItemTouchHelper.RIGHT
    val spoilSwipeDirection = ItemTouchHelper.LEFT
    val directions = consumeSwipeDirection or spoilSwipeDirection

    applySwipeCallback(
        swipeAwayDeletes,
        swipeAwayRestores,
        directions,
    ) { position, direction ->
      val holder = binding.detailList.findViewHolderForAdapterPosition(position)
      if (holder == null) {
        Timber.w("ViewHolder is null, cannot respond to swipe")
        return@applySwipeCallback
      }
      if (holder !is DetailItemViewHolder) {
        Timber.w("ViewHolder is not DetailItemViewHolder, cannot respond to swipe")
        return@applySwipeCallback
      }

      if (direction == consumeSwipeDirection || direction == spoilSwipeDirection) {
        if (swipeAwayDeletes) {
          deleteListItem(position)
        } else if (swipeAwayRestores) {
          if (direction == consumeSwipeDirection) {
            // Restore from archive
            restoreListItem(position)
          } else {
            // Delete forever
            deleteListItem(position)
          }
        } else {
          if (direction == consumeSwipeDirection) {
            consumeListItem(position)
          } else {
            spoilListItem(position)
          }
        }
      }
    }
  }

  @CheckResult
  private fun Drawable.themeIcon(): Drawable {
    val color = if (theming.isDarkTheme()) R3.color.white else R3.color.black
    return this.tintWith(layoutRoot.context, color)
  }

  private inline fun createSwipeCallback(
      directions: Int,
      crossinline itemSwipeCallback: (position: Int, directions: Int) -> Unit,
  ) {
    val left = leftBehindDrawable
    val right = rightBehindDrawable
    if (left == null || right == null) {
      return
    }

    val cb =
        object : SimpleSwipeCallback.ItemSwipeCallback {

          override fun itemSwiped(position: Int, direction: Int) {
            itemSwipeCallback(position, direction)
          }
        }

    val swipeCallback =
        SimpleSwipeCallback(cb, left, directions, Color.TRANSPARENT).apply {
          withBackgroundSwipeRight(Color.TRANSPARENT)
          withLeaveBehindSwipeRight(right)
        }

    // Detach any existing helper from the recyclerview
    touchHelper?.attachToRecyclerView(null)

    // Attach new helper
    val helper = ItemTouchHelper(swipeCallback).apply { attachToRecyclerView(binding.detailList) }

    // Set helper for cleanup later
    touchHelper = helper
  }

  private inline fun applySwipeCallback(
      swipeAwayDeletes: Boolean,
      swipeAwayRestores: Boolean,
      directions: Int,
      crossinline itemSwipeCallback: (position: Int, directions: Int) -> Unit,
  ) {
    clearLoaded()
    leftBehindLoaded =
        imageLoader
            .asDrawable()
            .load(
                when {
                  swipeAwayDeletes -> R4.drawable.ic_delete_24dp
                  swipeAwayRestores -> R4.drawable.ic_delete_24dp
                  else -> R2.drawable.ic_spoiled_24dp
                })
            .mutate { it.themeIcon() }
            .into(
                object : ImageTarget<Drawable> {
                  override fun clear() {
                    // Does nothing on its own, clear the touch helper to free
                  }

                  override fun setImage(image: Drawable) {
                    leftBehindDrawable = image
                    createSwipeCallback(directions, itemSwipeCallback)
                  }
                })

    rightBehindLoaded =
        imageLoader
            .asDrawable()
            .load(
                when {
                  swipeAwayDeletes -> R4.drawable.ic_delete_24dp
                  swipeAwayRestores -> R.drawable.ic_restore_from_trash_24
                  else -> R2.drawable.ic_consumed_24dp
                })
            .mutate { it.themeIcon() }
            .into(
                object : ImageTarget<Drawable> {
                  override fun clear() {
                    // Does nothing on its own, clear the touch helper to free
                  }

                  override fun setImage(image: Drawable) {
                    rightBehindDrawable = image
                    createSwipeCallback(directions, itemSwipeCallback)
                  }
                })
  }

  @CheckResult
  private fun usingAdapter(): DetailListAdapter {
    return modelAdapter.requireNotNull()
  }

  private fun restoreListItem(position: Int) {
    publish(DetailViewEvent.ListEvent.RestoreItem(position))
  }

  private fun deleteListItem(position: Int) {
    publish(DetailViewEvent.ListEvent.DeleteItem(position))
  }

  private fun consumeListItem(position: Int) {
    publish(DetailViewEvent.ListEvent.ConsumeItem(position))
  }

  private fun spoilListItem(position: Int) {
    publish(DetailViewEvent.ListEvent.SpoilItem(position))
  }

  private fun setList(
      list: List<FridgeItem>,
      expirationRange: DetailViewState.ExpirationRange?,
      sameDayExpired: DetailViewState.IsSameDayExpired?,
  ) {
    val data = list.map { DetailItemViewState(it, expirationRange, sameDayExpired) }
    usingAdapter().submitList(data)
  }

  private fun clearList() {
    usingAdapter().submitList(null)
  }

  private fun handleLoading(loading: Boolean) {
    binding.detailSwipeRefresh.isRefreshing = loading
  }

  private fun handleList(state: DetailViewState) {
    state.displayedItems.let { items ->
      when {
        items.isEmpty() -> clearList()
        else -> setList(items, state.expirationRange, state.isSameDayExpired)
      }
    }
  }

  private fun restoreLastScrollPosition(state: DetailViewState) {
    if (lastScrollPosition > 0) {
      if (!state.isLoading && state.displayedItems.isNotEmpty()) {
        val position = lastScrollPosition
        lastScrollPosition = 0

        Timber.d("Restoring visual scroll position: $position")
        binding.detailList.scrollToPosition(position)
      }
    }
  }

  private fun handleTopOffset(height: Int) {
    // Add additional padding to the list top to account for floating toolbar
    topDecoration.setMargin(top = height)
    binding.detailList.invalidateItemDecorations()
  }

  private fun handleBottomOffset(height: Int) {
    // Add additional padding to the list bottom to account for the height change in MainContainer
    bottomDecoration.setMargin(bottom = height)
    binding.detailList.invalidateItemDecorations()
  }

  override fun onRender(state: UiRender<DetailViewState>) {
    state.mapChanged { it.bottomOffset }.render(viewScope) { handleBottomOffset(it) }
    state.mapChanged { it.topOffset }.render(viewScope) { handleTopOffset(it) }
    state.mapChanged { it.isLoading }.render(viewScope) { handleLoading(it) }
    state.render(viewScope) { s ->
      handleList(s)
      setupSwipeCallback(s)
      restoreLastScrollPosition(s)
    }
  }

  companion object {
    private const val LAST_SCROLL_POSITION = "detail_last_scroll_position"
  }
}
