package me.showang.recyct

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import me.showang.recyct.items.DefaultLoadMoreItem
import me.showang.recyct.items.RecyctItem

open class RecyctAdapter(vararg val dataGroup: List<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_DEFAULT = Int.MAX_VALUE
        const val TYPE_HEADER = Int.MAX_VALUE - 1
        const val TYPE_FOOTER = Int.MAX_VALUE - 2
        const val TYPE_LOAD_MORE = Int.MAX_VALUE - 3
    }

    private val unionData: List<Any>
        get() = dataGroup.fold(mutableListOf()) { total, next -> total.apply { addAll(next) } }
    private val dataLength: Int get() = dataGroup.map { it.size }.reduce { acc, i -> acc + i }

    private val dataIndex = { itemIndex: Int -> itemIndex - (headerItem?.run { 1 } ?: 0) }
    private val viewHolderTypeMap = mutableMapOf<Int, RecyctItem>()

    private var headerItem: RecyctItem? = null
    private var footerItem: RecyctItem? = null
    private var loadMoreItem: RecyctItem = DefaultLoadMoreItem()

    var loadMoreEnabled: Boolean by didSet(false, ::updateLoadMoreState)
    var isLoadMoreFail: Boolean by didSet(false, ::updateLoadMoreState)

    fun register(recyctItem: RecyctItem, type: Int = TYPE_DEFAULT, clickDelegate: ((data: Any, dataIndex: Int) -> Unit)? = null) {
        checkTypeReserved(type)
        viewHolderTypeMap[type] = recyctItem.apply { this.clickDelegate = clickDelegate }
    }

    fun registerHeader(headerItem: RecyctItem, withData: Any? = null, clickListener: ((data: Any, itemIndex: Int) -> Unit)? = null) {
        headerItem.initData = withData
        this.headerItem = headerItem.apply { clickDelegate = clickListener }
    }

    fun registerFooter(footerItem: RecyctItem, data: Any? = null, clickListener: ((data: Any, itemIndex: Int) -> Unit)? = null) {
        footerItem.initData = data
        this.footerItem = footerItem.apply {
            clickDelegate = clickListener
        }
    }

    fun updateHeader(data: Any) {
        headerItem?.apply {
            initData = data
            notifyItemChanged(0)
        }
    }

    fun updateFooter(data: Any) {
        footerItem?.apply {
            initData = data
            notifyItemChanged(dataLength + (headerItem?.run { 1 } ?: 0))
        }
    }

    fun unregisterHeader() {
        headerItem = null
    }

    fun unregisterFooter() {
        footerItem = null
    }

    fun defaultLoadMore(callback: (() -> Unit)) {
        loadMoreItem = DefaultLoadMoreItem(callback)
    }

    protected open fun customViewHolderTypes(dataIndex: Int): Int {
        return TYPE_DEFAULT
    }

    private fun checkTypeReserved(type: Int) {
        if (type >= Int.MAX_VALUE - 10 && type != TYPE_DEFAULT)
            throw Error("Reserved types in range Int.MAX ~ Int.MAX - 10 by RecyctAdapter.")
    }

    private fun updateLoadMoreState(loadMoreEnabled: Boolean) {
        footerItem?.run {
            notifyItemChanged(itemCount - 1)
        } ?: if (loadMoreEnabled) notifyItemInserted(itemCount)
        else notifyItemRemoved(itemCount - 1)
    }

    final override fun getItemViewType(position: Int): Int {
        val customType = { index: Int ->
            customViewHolderTypes(index - (headerItem?.run { 1 } ?: 0))
        }
        val dataSize = dataLength
        return when (position) {
            0 -> headerItem?.run { TYPE_HEADER } ?: customType(position)
            dataSize -> headerItem?.run { customType(position) }
                    ?: lastItemType(position, customType)
            dataSize + 1 -> lastItemType(position, customType)
            else -> customType(position)
        }
    }

    private fun lastItemType(position: Int, otherwise: (Int) -> Int): Int {
        return if (loadMoreEnabled) TYPE_LOAD_MORE
        else footerItem?.run { TYPE_FOOTER } ?: otherwise(position)
    }

    final override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val recyctItem: RecyctItem? = when (type) {
            TYPE_HEADER -> headerItem
            TYPE_FOOTER -> footerItem
            TYPE_LOAD_MORE -> loadMoreItem
            else -> viewHolderTypeMap[type]
        }
        return recyctItem?.let {
            it.create(inflater, parent).apply {
                clickDelegate = recyctItem.clickDelegate
                parentItem = recyctItem
            }
        } ?: throw Error("No RecyctItem registered.")
    }

    final override fun getItemCount(): Int {
        return dataLength + (headerItem?.let { 1 } ?: 0) + when {
            loadMoreEnabled -> 1
            footerItem != null -> 1
            else -> 0
        }
    }

    final override fun onBindViewHolder(holder: RecyclerView.ViewHolder, itemIndex: Int) {
        val vh = holder as? RecyctViewHolder ?: throw Error("ViewHolder is not a RecycHolder")
        val dataIndexPair = when (getItemViewType(itemIndex)) {
            TYPE_HEADER -> headerItem?.initData?.let { Pair(it, itemIndex) }
            TYPE_FOOTER -> footerItem?.initData?.let { Pair(it, itemIndex) }
            TYPE_LOAD_MORE -> Pair(isLoadMoreFail, itemIndex)
            else -> dataIndex(itemIndex).let { Pair(unionData[it], it) }
        }
        dataIndexPair?.let { (data, index) ->
            vh.currentItemIndex = itemIndex
            vh.currentData = data
            vh.bind(data, index)
        }
    }

    fun notifyDataAppended(newDataSize: Int) {
        itemCount.also { notifyItemRangeChanged(it, it + newDataSize) }
    }

}

