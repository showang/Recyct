package me.showang.recyct.groups

import me.showang.recyct.RecyctAdapter
import me.showang.recyct.RecyctAdapter.Companion.TYPE_DEFAULT
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
    private val customViewHolderTypes: (Int) -> Int = { TYPE_DEFAULT }
) : TypeStrategy {
    override fun itemType(adapterIndex: Int): Int {
        return customViewHolderTypes(adapterIndex)
    }

    override val itemCount: Int
        get() = dataGroup.flatten().size
    override val root: TypeStrategy = this
}

class SectionTitleStrategy(
    private val dataGroup: List<List<Any>>,
    private val customViewHolderTypes: (Int) -> Int = { TYPE_DEFAULT }
) : TypeStrategy {

    override fun itemType(adapterIndex: Int): Int {
        var sectionCount = 0
        var itemCounter = 0
        for (dataList in dataGroup) {
            val currentCount = itemCounter + sectionCount
            val groupMaxIndex = currentCount - 1 // Size to Index
            if (adapterIndex == currentCount) {
                return RecyctAdapter.TYPE_SECTION_TITLE
            } else if (adapterIndex < groupMaxIndex) {
                break
            }
            sectionCount++
            itemCounter += dataList.size
        }
        return customViewHolderTypes(adapterIndex - sectionCount)
    }

    override val itemCount: Int
        get() = dataGroup.flatten().size + dataGroup.size

    override val root: TypeStrategy = this
}

abstract class StrategyDecorator(
    private val baseStrategy: TypeStrategy
) : TypeStrategy {
    override val root: TypeStrategy
        get() = baseStrategy
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
    var isLoadMoreDelegate: () -> Boolean
) : StrategyDecorator(base) {

    override fun itemType(adapterIndex: Int): Int {
        return when (adapterIndex) {
            itemCount - 1 -> TYPE_FOOTER.takeIf { !isLoadMoreDelegate() } ?: TYPE_LOAD_MORE
            else -> base.itemType(adapterIndex)
        }
    }

    override val itemCount: Int
        get() = 1 + base.itemCount

}