package me.showang.recyct.items

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.showang.recyct.R
import me.showang.recyct.RecyctViewHolder

class DefaultLoadMoreItem(private val onLoadMoreCallback: (() -> Unit)? = null,
                          private val retryDelegate: (() -> Unit)) : RecyctItemBase() {

    override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
        return object : RecyctViewHolder(inflater, parent, R.layout.item_load_more) {

            private val progress: View by id(R.id.progress)
            private val retryButton: View by id(R.id.retryButton)

            init {
                retryButton.setOnClickListener {
                    retryDelegate.invoke()
                }
            }

            override fun bind(data: Any, atIndex: Int) {
                if (data == true) { // onLoadMoreError
                    retryButton.visibility = View.VISIBLE
                    progress.visibility = View.GONE
                } else {
                    progress.visibility = View.VISIBLE
                    retryButton.visibility = View.GONE
                    itemView.post { onLoadMoreCallback?.invoke() }
                }
            }
        }
    }
}