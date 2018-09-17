package me.showang.recyct.items

import android.view.LayoutInflater
import android.view.ViewGroup
import me.showang.recyct.RecyctViewHolder

interface RecyctItem {
    var initData: Any?
    var clickDelegate: ((data: Any, index: Int) -> Unit)?
    fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder

}