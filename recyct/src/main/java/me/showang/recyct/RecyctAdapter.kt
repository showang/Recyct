package me.showang.recyct

import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.showang.recyct.items.DefaultLoadMoreItem
import me.showang.recyct.items.RecyctItem

open class RecyctAdapter(vararg data: List<Any>, defaultScope: CoroutineScope? = null) : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_DEFAULT = 0
        const val TYPE_HEADER = Int.MAX_VALUE - 1
        const val TYPE_FOOTER = Int.MAX_VALUE - 2
        const val TYPE_LOAD_MORE = Int.MAX_VALUE - 3
        const val TYPE_SECTION_TITLE = Int.MAX_VALUE - 4
    }

    private val uiScope: CoroutineScope by lazy { defaultScope ?: CoroutineScope(Dispatchers.Main) }
    private val dataGroup = mutableListOf<List<Any>>().apply { addAll(data) }
    private val unionData: List<Any>
        get() = dataGroup.fold(mutableListOf()) { total, next -> total.apply { addAll(next) } }
    private val dataLength: Int get() = dataGroup.map { it.size }.sum()
    private val dataSectionCount: Int get() = dataLength + if (hasSectionTitle) dataGroup.size else 0

    private val dataIndex = { itemIndex: Int ->
        itemIndex - (headerItem?.run { 1 }
                ?: 0) - if (hasSectionTitle) sectionIndex(itemIndex) + 1 else 0
    }
    private val viewHolderTypeMap = mutableMapOf<Int, RecyctItem>()

    private var headerItem: RecyctItem? = null
    private var footerItem: RecyctItem? = null
    private var loadMoreItem: RecyctItem? = null

    private var sectionTitleItem: RecyctItem? = null
    private var sectionTitleData: MutableList<Any> = mutableListOf()
    private val hasSectionTitle: Boolean get() = sectionTitleItem != null
    private val sectionIndex = { itemIndex: Int ->
        val index = itemIndex - (headerItem?.run { 1 } ?: 0)
        var dataCount = 0
        var sectionCount = 0
        for (currentData in dataGroup) {
            sectionCount++
            dataCount += currentData.size
            if (index < dataCount + sectionCount) {
                break
            }
        }
        sectionCount - 1
    }

    var enableLoadMore: Boolean by didSet(false, ::updateLoadMoreState)
    var isLoadMoreFail: Boolean by didSet(false, ::updateLoadMoreState)
    private val isLoadMoreEnabled get() = enableLoadMore && loadMoreItem != null

    fun appendDataGroup(dataList: List<Any>, sectionTitleData: Any? = null) {
        dataGroup.add(dataList)
        if (hasSectionTitle) {
            sectionTitleData
                    ?: throw IllegalArgumentException("You have to insert section title data when insert a new group.")
            this.sectionTitleData.add(sectionTitleData)
        }
    }

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
            notifyItemChanged(dataSectionCount + (headerItem?.run { 1 } ?: 0))
        }
    }

    fun unregisterHeader() {
        headerItem = null
    }

    fun unregisterFooter() {
        footerItem = null
    }

    fun defaultLoadMore(loadMoreCallback: (() -> Unit)? = null) {
        loadMoreItem = DefaultLoadMoreItem(loadMoreCallback) {
            isLoadMoreFail = false
        }
    }

    fun sectionsByGroup(sectionItem: RecyctItem, sectionData: List<Any>) {
        this.sectionTitleItem = sectionItem
        if (sectionData.size < dataGroup.size) throw IllegalArgumentException("section data is not enough.")
        this.sectionTitleData = sectionData.toMutableList()
    }

    protected open fun customViewHolderTypes(dataIndex: Int): Int {
        return TYPE_DEFAULT
    }

    private fun itemTypeByUserDef(itemIndex: Int): Int {
        val index = itemIndex - (headerItem?.run { 1 } ?: 0)
        if (sectionTitleItem == null) return customViewHolderTypes(index)

        var sectionCount = 0
        var itemCounter = 0
        for (dataList in dataGroup) {
            val currentCount = itemCounter + sectionCount
            val groupMaxIndex = currentCount - 1 // Size to Index
            if (index == currentCount) {
                return TYPE_SECTION_TITLE
            } else if (index < groupMaxIndex) {
                break
            }
            sectionCount++
            itemCounter += dataList.size
        }
        return customViewHolderTypes(index - sectionCount)
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
        val dataSize = dataLength + if (hasSectionTitle) dataGroup.size else 0
        return when (position) {
            0 -> headerItem?.run { TYPE_HEADER }
                    ?: if (dataSize == 0) lastItemType(position, ::itemTypeByUserDef)
                    else itemTypeByUserDef(position)
            dataSize -> headerItem?.run { itemTypeByUserDef(position) }
                    ?: lastItemType(position, ::itemTypeByUserDef)
            dataSize + 1 -> lastItemType(position, ::itemTypeByUserDef)
            else -> itemTypeByUserDef(position)
        }
    }

    private fun lastItemType(position: Int, otherwise: (Int) -> Int): Int {
        return if (isLoadMoreEnabled) TYPE_LOAD_MORE
        else footerItem?.run { TYPE_FOOTER } ?: otherwise(position)
    }

    final override fun onCreateViewHolder(parent: ViewGroup, type: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder =
            when (type) {
                TYPE_HEADER -> headerItem
                TYPE_FOOTER -> footerItem
                TYPE_LOAD_MORE -> loadMoreItem
                TYPE_SECTION_TITLE -> sectionTitleItem
                else -> viewHolderTypeMap[type]
            }?.let {
                @Suppress("UNNECESSARY_SAFE_CALL") //For unit test coverage.
                it.create(LayoutInflater.from(parent.context), parent)?.apply {
                    clickDelegate = it.clickDelegate
                    parentItem = it
                }
            } ?: throw Error("No RecyctItem registered.")

    final override fun getItemCount(): Int {
        return dataSectionCount + (headerItem?.let { 1 } ?: 0) + when {
            isLoadMoreEnabled || footerItem != null -> 1
            else -> 0
        }
    }

    final override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, itemIndex: Int) {
        val vh = holder as? RecyctViewHolder ?: throw Error("ViewHolder is not a RecycHolder")
        itemDataPair(itemIndex, ::getItemViewType)?.let { (data, index) ->
            vh.currentItemIndex = itemIndex
            vh.currentData = data
            vh.bind(data, index)
        }
    }

    private fun itemDataPair(itemIndex: Int, typeDelegate: (Int) -> Int): Pair<Any, Int>? {
        return when (typeDelegate(itemIndex)) {
            TYPE_HEADER -> headerItem?.initData?.let { Pair(it, itemIndex) }
            TYPE_FOOTER -> footerItem?.initData?.let { Pair(it, itemIndex) }
            TYPE_LOAD_MORE -> Pair(isLoadMoreFail, itemIndex)
            TYPE_SECTION_TITLE -> sectionIndex(itemIndex).let { sectionIndex ->
                if (sectionIndex < sectionTitleData.size)
                    Pair(sectionTitleData[sectionIndex], sectionIndex)
                else null
            }
            else -> dataIndex(itemIndex).let { Pair(unionData[it], it) }
        }
    }

    fun notifyDataAppended(newDataSize: Int) {
        itemCount.also { notifyItemRangeChanged(it, it + newDataSize) }
    }

    fun notifyGroupDataChanged(groupDataIndex: Int, groupIndex: Int = 0) {
        uiScope.launch {
            var itemCount = groupDataIndex
            withContext(IO) {
                for (index in 0 .. (groupIndex - 1)) {
                    itemCount += dataGroup[index].size
                }
                itemCount += headerItem?.run { 1 } ?: 0
                itemCount += sectionTitleItem?.run { groupIndex + 1 } ?: 0
            }
            notifyItemChanged(itemCount)
        }
    }

}
