package me.showang.recyct

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import me.showang.recyct.groups.*
import me.showang.recyct.items.DefaultLoadMoreItem
import me.showang.recyct.items.RecyctItem
import me.showang.recyct.items.viewholder.RecyctViewHolder

@Suppress("unused")
open class RecyctAdapter(
    vararg data: List<Any>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val dataGroup = mutableListOf<List<Any>>().apply { addAll(data) }
    private val unionData: List<Any> get() = dataGroup.flatten()
    private val dataLength: Int get() = dataGroup.sumOf { it.size }
    private val dataSectionCount: Int get() = dataLength + if (hasSectionTitle) dataGroup.size else 0

    private val dataIndex = { itemIndex: Int ->
        itemIndex - (headerItem?.run { 1 }
            ?: 0) - if (hasSectionTitle) sectionIndex(itemIndex) + 1 else 0
    }
    private val viewHolderTypeMap = mutableMapOf<Int, RecyctItem>()
    private var currentStrategy: TypeStrategy = BasicStrategy(dataGroup, ::customViewHolderTypes)

    private var headerItem: RecyctItem? = null
    private var footerItem: RecyctItem? = null
    private var loadMoreItem: RecyctItem? = null

    private var sectionTitleItem: RecyctItem? = null
    private var sectionDataDelegate: (Int) -> Any = {}
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

    fun appendDataGroup(dataList: List<Any>) {
        dataGroup.add(dataList.toMutableList())
    }

    fun register(
        recyctItem: RecyctItem,
        type: Int = TYPE_DEFAULT,
        clickDelegate: ((data: Any, dataIndex: Int, itemIndex: Int) -> Unit)? = null
    ) {
        checkTypeReserved(type)
        viewHolderTypeMap[type] = recyctItem.apply { this.itemClickDelegate = clickDelegate }
    }

    fun registerHeader(
        headerItem: RecyctItem,
        withData: Any? = null,
        clickListener: ((data: Any, dataIndex: Int, itemIndex: Int) -> Unit)? = null
    ) {
        headerItem.initData = withData
        this.headerItem = headerItem.apply { itemClickDelegate = clickListener }
        decorateStrategy()
    }

    private fun decorateStrategy(basicStrategy: TypeStrategy = currentStrategy) {
        currentStrategy = basicStrategy.root.let {
            headerItem?.run { HeaderDecorator(it) } ?: it
        }.let {
            FooterDecorator(it, { enableLoadMore }) { footerItem != null }
        }
    }

    fun registerFooter(
        item: RecyctItem,
        data: Any? = null,
        clickListener: ((data: Any, dataIndex: Int, itemIndex: Int) -> Unit)? = null
    ) {
        item.initData = data
        footerItem = item.apply { itemClickDelegate = clickListener }
        decorateStrategy()
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
            notifyItemChanged(currentStrategy.itemCount - 1)
        }
    }

    fun updateDataGroup(newDataGroup: List<List<Any>>) {
        dataGroup.clear()
        dataGroup.addAll(newDataGroup)
        decorateStrategy(SectionTitleStrategy(dataGroup, ::customViewHolderTypes))
    }

    fun unregisterHeader() {
        headerItem = null
        decorateStrategy()
    }

    fun unregisterFooter() {
        footerItem = null
        decorateStrategy()
    }

    fun defaultLoadMore(loadMoreCallback: (() -> Unit)? = null) {
        loadMoreItem = DefaultLoadMoreItem(loadMoreCallback) {
            isLoadMoreFail = false
        }
        decorateStrategy()
    }

    fun sectionsByGroup(sectionItem: RecyctItem, dataDelegate: (Int) -> Any) {
        sectionTitleItem = sectionItem
        sectionDataDelegate = dataDelegate
        decorateStrategy(SectionTitleStrategy(dataGroup, ::customViewHolderTypes))
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
        return currentStrategy.itemType(position)
    }

    final override fun onCreateViewHolder(
        parent: ViewGroup,
        type: Int
    ): RecyclerView.ViewHolder =
        when (type) {
            TYPE_HEADER -> headerItem
            TYPE_FOOTER -> footerItem
            TYPE_LOAD_MORE -> loadMoreItem
            TYPE_SECTION_TITLE -> sectionTitleItem
            else -> viewHolderTypeMap[type]
        }?.run {
            @Suppress("UNNECESSARY_SAFE_CALL") //For unit test coverage.
            create(LayoutInflater.from(parent.context), parent)?.apply {
                defaultClickDelegate = itemClickDelegate
            }
        } ?: throw Error(
            "No RecyctItem registered with type: ${
                when (type) {
                    TYPE_HEADER -> "TYPE_HEADER"
                    TYPE_FOOTER -> "TYPE_FOOTER"
                    TYPE_LOAD_MORE -> "TYPE_LOAD_MORE"
                    TYPE_SECTION_TITLE -> "TYPE_SECTION_TITLE"
                    else -> viewHolderTypeMap[type].toString()
                }
            }"
        )

    final override fun getItemCount(): Int {
        return currentStrategy.itemCount
    }

    final override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val vh = holder as? RecyctViewHolder
            ?: throw Error("ViewHolder is not a RecycHolder")
        itemDataPair(position, ::getItemViewType)?.let { (data, index) ->
            vh.currentData = data
            vh.currentDataIndex = index
            vh.currentItemIndex = position
            vh.bind(data, index, position)
        }
    }

    private fun itemDataPair(itemIndex: Int, typeDelegate: (Int) -> Int): Pair<Any, Int>? {
        return when (typeDelegate(itemIndex)) {
            TYPE_HEADER -> headerItem?.initData?.let { Pair(it, 0) }
            TYPE_FOOTER -> footerItem?.initData?.let { Pair(it, 0) }
            TYPE_LOAD_MORE -> Pair(isLoadMoreFail, 0)
            TYPE_SECTION_TITLE -> sectionIndex(itemIndex).let { sectionIndex ->
                Pair(sectionDataDelegate(sectionIndex), sectionIndex)
            }
            else -> dataIndex(itemIndex).let { Pair(unionData[it], it) }
        }
    }

    fun notifyDataAppended(newDataSize: Int) {
        itemCount.also {
            val loadMoreOffset = if (isLoadMoreEnabled) 1 else 0
            notifyItemRangeChanged(it - loadMoreOffset, newDataSize + loadMoreOffset)
        }
    }

    fun clearData() {
        val size = itemCount
        dataGroup.clear()
        notifyItemRangeRemoved(0, size)
    }

    suspend fun notifyGroupDataChanged(
        groupDataIndex: Int,
        groupIndex: Int = 0
    ) {
        var itemCount = groupDataIndex
        withContext(IO) {
            for (index in 0 until groupIndex) {
                itemCount += dataGroup[index].size
            }
            headerItem?.run { itemCount += 1 }
            sectionTitleItem?.run { itemCount += (groupIndex + 1) }
        }
        notifyItemChanged(itemCount)
    }

    companion object {
        const val TYPE_DEFAULT = Int.MAX_VALUE
        const val TYPE_HEADER = Int.MAX_VALUE - 1
        const val TYPE_FOOTER = Int.MAX_VALUE - 2
        const val TYPE_LOAD_MORE = Int.MAX_VALUE - 3
        const val TYPE_SECTION_TITLE = Int.MAX_VALUE - 4
    }
}
