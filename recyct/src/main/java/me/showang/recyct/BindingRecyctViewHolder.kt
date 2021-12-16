package me.showang.recyct

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import me.showang.recyct.items.RecyctItem
import kotlin.properties.ObservableProperty

abstract class RecyctViewHolder(
    itemView: View,
    val parentItem: RecyctItem
) : RecyclerView.ViewHolder(itemView) {

    abstract var currentData: Any?
    abstract var currentDataIndex: Int
    abstract var currentItemIndex: Int
    abstract var defaultClickDelegate: ((Any, Int, Int) -> Unit)?

    abstract fun bind(data: Any, dataIndex: Int, itemIndex: Int)

    protected val context: Context get() = itemView.context
}

abstract class BindingRecyctViewHolder<Binding : ViewBinding>(
    protected val binding: Binding,
    parentItem: RecyctItem
) : RecyctViewHolder(binding.root, parentItem) {

    override var currentData: Any? = null
    override var currentDataIndex: Int = -1
    override var currentItemIndex: Int = -1
    override var defaultClickDelegate by didSetNullable<(Any, Int, Int) -> Unit> { value ->
        value?.let { delegate ->
            itemView.setOnClickListener {
                currentData?.let { data ->
                    delegate(data, currentDataIndex, currentItemIndex)
                }
            }
        }
    }

}

abstract class LegacyRecyctViewHolder(
    @LayoutRes resId: Int,
    inflater: LayoutInflater,
    parent: ViewGroup?,
    parentItem: RecyctItem
) : RecyctViewHolder(inflater.inflate(resId, parent, false), parentItem) {

    override var currentData: Any? = null
    override var currentDataIndex: Int = -1
    override var currentItemIndex: Int = -1
    override var defaultClickDelegate by didSetNullable<(Any, Int, Int) -> Unit> { value ->
        value?.let { delegate ->
            itemView.setOnClickListener {
                currentData?.let { data ->
                    delegate(data, currentDataIndex, currentItemIndex)
                }
            }
        }
    }

    protected fun <T : View> id(@IdRes resId: Int) =
        object : ObservableProperty<T>(itemView.findViewById<T>(resId)) {}
}

