package me.showang.recyct.items

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.showang.recyct.items.viewholder.BindingRecyctViewHolder
import me.showang.recyct.databinding.ItemLoadMoreBinding

class DefaultLoadMoreItem(
    private val onLoadMoreCallback: (() -> Unit)? = null,
    private val retryDelegate: (() -> Unit)
) : RecyctItem() {

    override fun create(inflater: LayoutInflater, parent: ViewGroup) =
        object : BindingRecyctViewHolder<ItemLoadMoreBinding>(
            ItemLoadMoreBinding.inflate(inflater, parent, false),
            this
        ) {
            init {
                binding.retryButton.setOnClickListener {
                    retryDelegate.invoke()
                }
            }

            override fun bind(data: Any, dataIndex: Int, itemIndex: Int) = binding.run {
                if (data == true) { // onLoadMoreError
                    retryButton.visibility = View.VISIBLE
                    progress.visibility = View.GONE
                } else {
                    itemView.post { onLoadMoreCallback?.invoke() }
                    progress.visibility = View.VISIBLE
                    retryButton.visibility = View.GONE
                }
            }
        }
}