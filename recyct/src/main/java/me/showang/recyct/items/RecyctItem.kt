package me.showang.recyct.items

import android.view.LayoutInflater
import android.view.ViewGroup
import me.showang.recyct.items.viewholder.RecyctViewHolder

abstract class RecyctItem {
    var initData: Any? = null
    var itemClickDelegate: ((data: Any, dataIndex: Int, itemIndex: Int) -> Unit)? = null

    abstract fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder
}