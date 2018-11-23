package me.showang.recyct

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import me.showang.recyct.items.RecyctItem
import kotlin.properties.ObservableProperty

abstract class RecyctViewHolder(inflater: LayoutInflater, parent: ViewGroup, resId: Int)
    : androidx.recyclerview.widget.RecyclerView.ViewHolder(inflater.inflate(resId, parent, false)) {

    var parentItem: RecyctItem? = null
    var currentData: Any? = null
    var currentItemIndex: Int = -1
    var clickDelegate by didSetNullable<(Any, Int) -> Unit> { value ->
        value?.let { delegate ->
            itemView.setOnClickListener {
                currentData?.let {
                    data -> delegate(data, currentItemIndex)
                }
            }
        }
    }

    protected val context: Context get() = itemView.context
    protected fun <T : View> id(@IdRes resId: Int) = object : ObservableProperty<T>(itemView.findViewById<T>(resId)) {}

    abstract fun bind(data: Any, atIndex: Int)
}

