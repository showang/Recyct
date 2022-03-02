package me.showang.recyct.items.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes

abstract class LoadMoreViewHolder(
    @LayoutRes layout: Int,
    inflater: LayoutInflater,
    parent: ViewGroup
) : LegacyRecyctViewHolder(layout, inflater, parent) {

    final override fun bind(data: Any, dataIndex: Int, itemIndex: Int) {
        (data as? Boolean)?.let { bind(it, itemIndex) }
    }

    abstract fun bind(isLoadMoreFailed: Boolean, adapterIndex: Int)

}