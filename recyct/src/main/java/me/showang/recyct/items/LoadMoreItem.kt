package me.showang.recyct.items

import android.view.LayoutInflater
import android.view.ViewGroup
import me.showang.recyct.items.viewholder.LoadMoreViewHolder
import me.showang.recyct.items.viewholder.RecyctViewHolder

abstract class LoadMoreItem : RecyctItem() {

    final override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
        return createViewHolder(inflater, parent)
    }

    abstract fun createViewHolder(inflater: LayoutInflater, parent: ViewGroup): LoadMoreViewHolder
}