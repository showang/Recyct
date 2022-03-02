package me.showang.recyct.groups

import me.showang.recyct.RecyctAdapter
import me.showang.recyct.RecyctAdapter.Companion.TYPE_FOOTER
import me.showang.recyct.RecyctAdapter.Companion.TYPE_HEADER
import me.showang.recyct.RecyctAdapter.Companion.TYPE_LOAD_MORE

interface TypeStrategy {
    fun itemType(adapterIndex: Int): Int

    val itemCount: Int

    val root: TypeStrategy
}

class BasicStrategy(
    private val dataGroup: List<List<Any>>,
    private val customViewHolderTypes: (Int) -> Int
) : TypeStrategy {
    override fun itemType(adapterIndex: Int): Int {
        return customViewHolderTypes(adapterIndex)
    }

    override val itemCount: Int
        get() = dataGroup.sumOf { it.size }
    override val root: TypeStrategy = this
}

class SectionTitleStrategy(
    private val dataGroup: List<List<Any>>,
    private val customViewHolderTypes: (Int) -> Int
) : TypeStrategy {

    override fun itemType(adapterIndex: Int): Int {
        var sectionCount = 0
        var itemCounter = 0
        for (dataList in dataGroup) {
            val currentCount = sectionCount +
                    if (itemCounter != 0) itemCounter else 0
            if (adapterIndex == currentCount) {
                return RecyctAdapter.TYPE_SECTION_TITLE
            } else if (adapterIndex < currentCount) {
                break
            }
            sectionCount++
            itemCounter += dataList.size
        }
        return customViewHolderTypes(adapterIndex - sectionCount)
    }

    override val itemCount: Int
        get() = dataGroup.sumOf { it.size } + dataGroup.size

    override val root: TypeStrategy = this
}

abstract class StrategyDecorator(
    private val baseStrategy: TypeStrategy
) : TypeStrategy {
    override val root: TypeStrategy
        get() = baseStrategy.root
}

class HeaderDecorator(
    private val base: TypeStrategy
) : StrategyDecorator(base) {

    override val itemCount: Int
        get() = 1 + base.itemCount

    override fun itemType(adapterIndex: Int): Int {
        return when (adapterIndex) {
            0 -> TYPE_HEADER
            else -> base.itemType(adapterIndex - 1)
        }
    }
}

class FooterDecorator(
    private val base: TypeStrategy,
    val hasLoadMoreDelegate: () -> Boolean,
    val hasFooterItemDelegate: () -> Boolean
) : StrategyDecorator(base) {

    override fun itemType(adapterIndex: Int): Int {
        return when (adapterIndex) {
            itemCount - 1 -> when {
                hasLoadMoreDelegate() -> TYPE_LOAD_MORE
                hasFooterItemDelegate() -> TYPE_FOOTER
                else -> base.itemType(adapterIndex)
            }
            else -> base.itemType(adapterIndex)
        }
    }

    override val itemCount: Int
        get() = base.itemCount + if (hasLoadMoreDelegate() || hasFooterItemDelegate()) 1 else 0

}