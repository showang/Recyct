package me.showang.recyct

import me.showang.recyct.RecyctAdapter.Companion.TYPE_DEFAULT
import me.showang.recyct.groups.FooterDecorator
import me.showang.recyct.groups.HeaderDecorator
import me.showang.recyct.groups.SectionTitleStrategy
import org.junit.Test

class SectionTitleStrategyTest {


    @Test
    fun testSectionTitleStrategy() {
        val dataGroup = listOf(
            (1..10).toList(),
            (1..20).toList(),
            (1..5).toList()
        )
        val dataCount = dataGroup.flatten().size
        val strategy = SectionTitleStrategy(dataGroup) { TYPE_DEFAULT }

        assert(strategy.itemCount == dataCount + dataGroup.size)
        assert(strategy.itemType(0) == RecyctAdapter.TYPE_SECTION_TITLE)
        assert(strategy.itemType(1) == TYPE_DEFAULT)
        assert(strategy.itemType(10) == TYPE_DEFAULT)
        assert(strategy.itemType(11) == RecyctAdapter.TYPE_SECTION_TITLE)

        assert(strategy.itemType(strategy.itemCount - 1) == TYPE_DEFAULT)
    }

    @Test
    fun testSectionTitleStrategy_withHeader() {
        val dataGroup = listOf(
            (1..10).toList(),
            (1..20).toList(),
            (1..5).toList()
        )
        val dataCount = dataGroup.flatten().size
        val strategy =
            HeaderDecorator(SectionTitleStrategy(dataGroup) { TYPE_DEFAULT })

        assert(strategy.itemCount == dataCount + dataGroup.size + 1)
        assert(strategy.itemType(0) == RecyctAdapter.TYPE_HEADER)
        assert(strategy.itemType(1) == RecyctAdapter.TYPE_SECTION_TITLE)
        assert(strategy.itemType(2) == TYPE_DEFAULT)
        assert(strategy.itemType(11) == TYPE_DEFAULT)
        assert(strategy.itemType(12) == RecyctAdapter.TYPE_SECTION_TITLE)

        assert(strategy.itemType(strategy.itemCount - 1) == TYPE_DEFAULT)
    }

    @Test
    fun testSectionTitleStrategy_withFooter() {
        val dataGroup = listOf(
            (1..10).toList(),
            (1..20).toList(),
            (1..5).toList()
        )
        val dataCount = dataGroup.flatten().size
        var isLoadMore = false
        val hasFooter = true
        FooterDecorator(
            SectionTitleStrategy(dataGroup) { TYPE_DEFAULT },
            { isLoadMore }) { hasFooter }.run {
            assert(itemCount == dataCount + dataGroup.size + 1)
            assert(itemType(0) == RecyctAdapter.TYPE_SECTION_TITLE)
            assert(itemType(1) == TYPE_DEFAULT)
            assert(itemType(10) == TYPE_DEFAULT)
            assert(itemType(11) == RecyctAdapter.TYPE_SECTION_TITLE)

            assert(itemType(itemCount - 7) == RecyctAdapter.TYPE_SECTION_TITLE)
            assert(itemType(itemCount - 2) == TYPE_DEFAULT)

            assert(itemType(itemCount - 1) == RecyctAdapter.TYPE_FOOTER)
            isLoadMore = true
            assert(itemType(itemCount - 1) == RecyctAdapter.TYPE_LOAD_MORE)
        }
    }

    @Test
    fun testSectionTitleStrategy_withHeaderFooter() {
        val dataGroup = listOf(
            (1..10).toList(),
            (1..20).toList(),
            (1..5).toList()
        )
        val dataCount = dataGroup.flatten().size
        var isLoadMore = false
        val hasFooter = true
        FooterDecorator(
            HeaderDecorator(
                SectionTitleStrategy(dataGroup) { TYPE_DEFAULT }
            ),
            { isLoadMore }
        ) { hasFooter }.run {
            assert(itemCount == dataCount + dataGroup.size + 1 + 1)
            assert(itemType(0) == RecyctAdapter.TYPE_HEADER)
            assert(itemType(1) == RecyctAdapter.TYPE_SECTION_TITLE)
            assert(itemType(2) == TYPE_DEFAULT)
            assert(itemType(11) == TYPE_DEFAULT)
            assert(itemType(12) == RecyctAdapter.TYPE_SECTION_TITLE)

            assert(itemType(itemCount - 7) == RecyctAdapter.TYPE_SECTION_TITLE)
            assert(itemType(itemCount - 2) == TYPE_DEFAULT)

            assert(itemType(itemCount - 1) == RecyctAdapter.TYPE_FOOTER)
            isLoadMore = true
            assert(itemType(itemCount - 1) == RecyctAdapter.TYPE_LOAD_MORE)
        }
    }

    @Test
    fun testSectionTitleStrategy_withFooterHeader() {
        val dataGroup = listOf(
            (1..10).toList(),
            (1..20).toList(),
            (1..5).toList()
        )
        val dataCount = dataGroup.flatten().size
        var isLoadMore = false
        val hasFooter = true
        HeaderDecorator(
            FooterDecorator(
                SectionTitleStrategy(dataGroup) { TYPE_DEFAULT },
                { isLoadMore }) { hasFooter }).run {
            assert(itemCount == dataCount + dataGroup.size + 1 + 1)
            assert(itemType(0) == RecyctAdapter.TYPE_HEADER)
            assert(itemType(1) == RecyctAdapter.TYPE_SECTION_TITLE)
            assert(itemType(2) == TYPE_DEFAULT)
            assert(itemType(11) == TYPE_DEFAULT)
            assert(itemType(12) == RecyctAdapter.TYPE_SECTION_TITLE)

            assert(itemType(itemCount - 7) == RecyctAdapter.TYPE_SECTION_TITLE)
            assert(itemType(itemCount - 2) == TYPE_DEFAULT)

            assert(itemType(itemCount - 1) == RecyctAdapter.TYPE_FOOTER)
            isLoadMore = true
            assert(itemType(itemCount - 1) == RecyctAdapter.TYPE_LOAD_MORE)
        }
    }

}