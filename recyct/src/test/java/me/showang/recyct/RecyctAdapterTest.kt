package me.showang.recyct

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.showang.recyct.items.DefaultLoadMoreItem
import me.showang.recyct.items.RecyctItem
import me.showang.recyct.items.RecyctItemBase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(LayoutInflater::class)
class RecyctAdapterTest {

    private lateinit var data: MutableList<Int>
    private lateinit var context: Context
    private lateinit var parent: ViewGroup
    private lateinit var itemView: View
    private lateinit var adapter: RecyctAdapter
    private lateinit var inflater: LayoutInflater
    private lateinit var observable: Any
    private lateinit var observableClazz: Class<*>

    private lateinit var header: RecyctItem
    private lateinit var footer: RecyctItem

    @Before
    fun setup() {
        data = (0..50).toMutableList()
        adapter = RecyctAdapter(data).also(::mockAdapter)
        context = mock(Context::class.java)
        parent = mock(ViewGroup::class.java)
        itemView = mock(View::class.java)
        header = mock(RecyctItem::class.java)
        footer = mock(RecyctItem::class.java)
        inflater = mockLayoutInflater(parent)
    }

    @Test
    fun testRegister_reservedErrors() {
        (Int.MAX_VALUE - 10..Int.MAX_VALUE).forEach {
            try {
                adapter.register(mock(RecyctItem::class.java), it)
                assert(false) { "Reserved range: Int.MAX_VALUE - 10 ~ Int.MAX_VALUE" }
            } catch (e: Error) {
            }
        }
    }

    @Test
    fun testRegister() {
        mockLayoutInflater(parent)
        adapter = object : RecyctAdapter(data) {
            override fun customViewHolderTypes(dataIndex: Int): Int {
                return dataIndex % 2
            }
        }
        val delegateOdd: (Any, Int) -> Unit = { _, _ -> }
        val delegateEven: (Any, Int) -> Unit = { _, _ -> }
        adapter.register(defaultRecyctItem(), 0, delegateEven)
        adapter.register(defaultRecyctItem(), 1, delegateOdd)

        data.map {
            adapter.createViewHolder(parent, adapter.getItemViewType(it)) as RecyctViewHolder
        }.forEachIndexed { index, vh ->
            adapter.bindViewHolder(vh, index)
            if (index % 2 == 0) assert(vh.clickDelegate == delegateEven)
            else assert(vh.clickDelegate == delegateOdd)
        }
    }

    @Test
    fun testItemCount_oneSource() {
        assert(adapter.itemCount == 51)

        adapter.registerHeader(header)
        assert(adapter.itemCount == 52)

        adapter.registerFooter(footer)
        assert(adapter.itemCount == 53)

        adapter.loadMoreEnabled = true
        assert(adapter.itemCount == 53)

        adapter.unregisterFooter()
        assert(adapter.itemCount == 53)

        adapter.unregisterHeader()
        assert(adapter.itemCount == 52)

        adapter.loadMoreEnabled = false
        assert(adapter.itemCount == 51)
    }

    @Test
    fun testViewTypes() {
        val data: List<Any> = adapter.dataGroup.fold(mutableListOf()) { total, next -> total.apply { addAll(next) } }
        adapter = RecyctAdapter(data).also(::mockAdapter)
        data.onEach { assert(adapter.getItemViewType(it as Int) == RecyctAdapter.TYPE_DEFAULT) }

        adapter.registerHeader(header)
        assert(adapter.getItemViewType(0) == RecyctAdapter.TYPE_HEADER)
        data.onEach { assert(adapter.getItemViewType(it as Int + 1) == RecyctAdapter.TYPE_DEFAULT) }

        adapter.registerFooter(footer)
        assert(adapter.getItemViewType(0) == RecyctAdapter.TYPE_HEADER)
        data.onEach { assert(adapter.getItemViewType(it as Int + 1) == RecyctAdapter.TYPE_DEFAULT) }
        assert(adapter.getItemViewType(data.size + 1) == RecyctAdapter.TYPE_FOOTER)

        adapter.loadMoreEnabled = true
        assert(adapter.getItemViewType(0) == RecyctAdapter.TYPE_HEADER)
        data.onEach { assert(adapter.getItemViewType(it as Int + 1) == RecyctAdapter.TYPE_DEFAULT) }
        assert(adapter.getItemViewType(data.size + 1) == RecyctAdapter.TYPE_LOAD_MORE)

        adapter.unregisterFooter()
        assert(adapter.getItemViewType(0) == RecyctAdapter.TYPE_HEADER)
        data.onEach { assert(adapter.getItemViewType(it as Int + 1) == RecyctAdapter.TYPE_DEFAULT) }
        assert(adapter.getItemViewType(data.size + 1) == RecyctAdapter.TYPE_LOAD_MORE)

        adapter.loadMoreEnabled = false
        assert(adapter.getItemViewType(0) == RecyctAdapter.TYPE_HEADER)
        data.onEach { assert(adapter.getItemViewType(it as Int + 1) == RecyctAdapter.TYPE_DEFAULT) }
        assert(adapter.getItemViewType(data.size + 1) == RecyctAdapter.TYPE_DEFAULT)
    }

    @Test
    fun testMultiSource() {
        adapter = RecyctAdapter((0..49).toMutableList(), (0..49).toMutableList())
        assert(adapter.itemCount == 100)
    }

    @Test
    fun testCreateViewHolder_default() {
        val clickDelegate: (Any, Int) -> Unit = { _, _ -> }
        mockLayoutInflater(parent)
        adapter.also(::mockAdapter)

        try {
            adapter.onCreateViewHolder(parent, 0)
            assert(false) { "Not register item yet" }
        } catch (e: Error) {
        }

        val recyctItem: RecyctItem = defaultRecyctItem()
        adapter.apply { register(recyctItem, clickDelegate = clickDelegate) }
        val vh = adapter.onCreateViewHolder(parent, RecyctAdapter.TYPE_DEFAULT) as RecyctViewHolder
        assert(vh.parentItem == recyctItem)
        assert(vh.clickDelegate == clickDelegate)

    }

    @Test
    fun testCreateViewHolder_header() {
        val headerData = "This is header."
        val clickDelegate: (Any, Int) -> Unit = { _, _ -> }
        mockLayoutInflater(parent)
        header = defaultRecyctItem()
        adapter.registerHeader(header, headerData, clickDelegate)
        val headerVh = adapter.onCreateViewHolder(parent, RecyctAdapter.TYPE_HEADER) as RecyctViewHolder
        assert(headerVh.parentItem == header)
        assert(headerVh.clickDelegate == clickDelegate)
        assert(header.initData == headerData)

        itemView.performClick()

        "This is new header data.".also {
            adapter.updateHeader(it)
            assert(header.initData == it)
        }
    }

    @Test
    fun testCreateViewHolder_footer() {
        val footerData = "This is footer."
        val clickDelegate: (Any, Int) -> Unit = { _, _ -> }
        mockLayoutInflater(parent)
        footer = defaultRecyctItem()
        adapter.registerFooter(footer, footerData, clickDelegate)
        val footerVh = adapter.onCreateViewHolder(parent, RecyctAdapter.TYPE_FOOTER) as RecyctViewHolder
        assert(footerVh.parentItem == footer)
        assert(footerVh.clickDelegate?.equals(clickDelegate) ?: false)
        assert(footer.initData == footerData)
        "Update footer data".also {
            adapter.updateFooter(it)
            assert(footer.initData == it)
        }

        header = defaultRecyctItem()
        adapter.registerHeader(header)

        "Update footer data with Header".also {
            adapter.updateFooter(it)
            assert(footer.initData == it)
        }
    }

    @Test
    fun testCreateViewHolder_loadMore() {
        mockLayoutInflater(parent)
        adapter.defaultLoadMore { }
        val footerVh = adapter.onCreateViewHolder(parent, RecyctAdapter.TYPE_LOAD_MORE) as RecyctViewHolder
        assert(footerVh.parentItem is DefaultLoadMoreItem)
    }

    @Test
    fun testBindViewHolder_default() {
        val initData = data
        val vh = recyctViewHolder(inflater, parent) { data, atIndex ->
            assert(data as Int == atIndex)
        }
        initData.forEach { adapter.bindViewHolder(vh, it) }

        val headerVh = recyctViewHolder(inflater, parent) { _, atIndex ->
            assert(0 == atIndex)
        }
        adapter.registerHeader(header)
        adapter.bindViewHolder(headerVh, 0)
    }

    @Test
    fun testBindViewHolder_header() {
        val headerData = "This is header"
        var headerItem = defaultRecyctItem()
        adapter.registerHeader(headerItem, headerData)

        (0..(adapter.itemCount - 1)).map {
            recyctViewHolder(inflater, parent) { data, atIndex ->
                when {
                    atIndex == 0 && data is String -> assert(data == headerData)
                    else -> assert(data as Int == atIndex)
                }
            }
        }.forEachIndexed { index, inVh -> adapter.bindViewHolder(inVh, index) }

        headerItem = defaultRecyctItem()
        adapter.registerHeader(headerItem)
        (0..(adapter.itemCount - 1)).map {
            recyctViewHolder(inflater, parent) { data, atIndex ->
                when {
                    atIndex == 0 && data == null -> assert(true)
                    else -> assert(data as Int == atIndex)
                }
            }
        }.forEachIndexed { index, inVh -> adapter.bindViewHolder(inVh, index) }
    }

    @Test
    fun testBindViewHolder_footer() {
        val footerData = "This is footer"
        val footerItem = defaultRecyctItem()
        adapter.registerFooter(footerItem, footerData)
        val itemCount = adapter.itemCount
        (0..(itemCount - 1)).map {
            recyctViewHolder(inflater, parent) { data, atIndex ->
                when {
                    atIndex == itemCount - 1 && data is String -> assert(data == footerData)
                    else -> assert(data as Int == atIndex)
                }
            }
        }.forEachIndexed { index, inVh -> adapter.bindViewHolder(inVh, index) }
    }

    @Test
    fun testBindViewHolder_loadMore() {
        var startLoadMore = false
        mockLayoutInflater(parent)

        adapter.apply {
            register(object : RecyctItemBase("load more") {
                override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
                    return recyctViewHolder(inflater, parent) { data, atIndex ->
                        assert(data as Int == atIndex)
                    }
                }
            })
            defaultLoadMore { startLoadMore = true }
            loadMoreEnabled = true
        }
        val itemCount = adapter.itemCount
        (0..(itemCount - 1)).map {
            adapter.createViewHolder(parent, adapter.getItemViewType(it))
        }.forEachIndexed { index, inVh -> adapter.bindViewHolder(inVh, index) }

        verify(itemView).post(argThat {
            it.run()
            true
        })
        assert(startLoadMore)

        adapter.isLoadMoreFail = true
    }

    @Test
    fun testUpdateWithNoItem() {
        adapter.updateHeader("updateHeader")
        adapter.updateFooter("updateFooter")
    }

    @Test
    fun testBindViewHolder_noCallback() {
        mockLayoutInflater(parent)

        adapter.apply {
            register(object : RecyctItemBase() {
                override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
                    return recyctViewHolder(inflater, parent) { data, atIndex ->
                        when (atIndex) {
                            itemCount - 1 -> assert(!(data as Boolean))
                            else -> assert(data as Int == atIndex)
                        }
                    }
                }
            })
            loadMoreEnabled = true
        }
        val itemCount = adapter.itemCount
        (0..(itemCount - 1)).map {
            adapter.createViewHolder(parent, adapter.getItemViewType(it))
        }.forEachIndexed { index, inVh -> adapter.bindViewHolder(inVh, index) }

        verify(itemView).post(argThat {
            it.run()
            true
        })
    }

    @Test
    fun testNotifyDataAppended() {
        data.addAll((0..50).toMutableList())
        adapter.notifyDataAppended(51)
//        verify(observable).notifyItemRangeChanged(argThat { start ->
//            start == 51
//        }, argThat { last ->
//            last == 51 + 51
//        })
    }

    private fun defaultRecyctItem(): RecyctItem = object : RecyctItemBase() {
        override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
            return recyctViewHolder(inflater, parent)
        }
    }

    private fun recyctViewHolder(inflater: LayoutInflater, parent: ViewGroup, assertions: (data: Any?, atIndex: Int) -> Unit = { _, _ -> }): RecyctViewHolder {
        return object : RecyctViewHolder(inflater, parent, 0) {
            override fun bind(data: Any, atIndex: Int) {
                assertions(data, atIndex)
            }
        }
    }

    private fun mockLayoutInflater(parent: ViewGroup): LayoutInflater {
        mockStatic(LayoutInflater::class.java)
        val inflater = mock(LayoutInflater::class.java)
        `when`(parent.context).thenReturn(context)
        `when`(LayoutInflater.from(context)).thenReturn(inflater)
        `when`(inflater.inflate(anyInt(), eq(parent), eq(false))).thenReturn(itemView)
        return inflater
    }

    private fun mockAdapter(adapter: RecyctAdapter) {
        val observableField = adapter.javaClass.superclass?.getDeclaredField("mObservable")
        observableField?.isAccessible = true
        observableClazz = RecyclerView::class.java.declaredClasses
                .reduce { acc, clazz ->
                    when {
                        clazz.toString().contains("AdapterDataObservable") -> clazz
                        acc != null -> acc
                        else -> null
                    }
                }
        observable = mock(observableClazz)
        observableField?.set(adapter, observable)
    }
}