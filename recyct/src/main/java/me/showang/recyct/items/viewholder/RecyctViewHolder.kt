package me.showang.recyct.items.viewholder

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import me.showang.recyct.didSetNullable

abstract class RecyctViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var currentData: Any? = null
    var currentDataIndex: Int = -1
    var currentItemIndex: Int = -1
    var defaultClickDelegate by didSetNullable<(Any, Int, Int) -> Unit> { value ->
        value?.let { delegate ->
            itemView.setOnClickListener {
                currentData?.let { data ->
                    delegate(data, currentDataIndex, currentItemIndex)
                }
            }
        }
    }

    abstract fun bind(data: Any, dataIndex: Int, itemIndex: Int)

    protected val context: Context get() = itemView.context
}
