package me.showang.recyct

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import me.showang.recyct.items.RecyctItem

abstract class RecyctViewHolder(inflater: LayoutInflater, parent: ViewGroup, resId: Int)
    : RecyclerView.ViewHolder(inflater.inflate(resId, parent, false)) {

    var parentItem: RecyctItem? = null
    var currentData: Any? = null
    var currentItemIndex: Int = -1
    var clickDelegate by didSetNullable<(Any, Int) -> Unit> { value ->
        value?.let { delegate ->
            itemView.setOnClickListener { currentData?.let { data -> delegate(data, currentItemIndex) } }
        }
    }

    abstract fun bind(data: Any, atIndex: Int)

}

