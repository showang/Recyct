package me.showang.recyct.items

import android.view.LayoutInflater
import android.view.ViewGroup
import me.showang.recyct.R
import me.showang.recyct.RecyctViewHolder

class DefaultLoadMoreItem(private val onLoadMoreCallback: (() -> Unit)? = null) : RecyctItemBase() {

    override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
        return object : RecyctViewHolder(inflater, parent, R.layout.item_load_more) {
            override fun bind(data: Any, atIndex: Int) {
                itemView.post { onLoadMoreCallback?.invoke() }
            }
        }
    }
}