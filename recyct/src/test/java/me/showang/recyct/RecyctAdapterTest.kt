package me.showang.recyct

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
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
import java.lang.reflect.Field
import java.lang.reflect.Modifier


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
        `when`(itemView.context).thenReturn(context)
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
    fun testRegister_itemClick() {
        adapter = object : RecyctAdapter(data) {
            override fun customViewHolderTypes(dataIndex: Int): Int {
                return dataIndex % 2
            }
        }
        var click = false
        val delegate: (Any, Int) -> Unit = { _, _ -> click = true }
        adapter.register(defaultRecyctItem(), 0, delegate)

        adapter.bindViewHolder(adapter.createViewHolder(parent, adapter.getItemViewType(0)) as RecyctViewHolder, 0)

        verify(itemView).setOnClickListener(argThat {
            it?.onClick(itemView)
            true
        })
        assert(click)
    }

    @Test
    fun testRegister_itemClickWithNoCurrentData() {
        adapter = object : RecyctAdapter(data) {
            override fun customViewHolderTypes(dataIndex: Int): Int {
                return dataIndex % 2
            }
        }
        var click = false
        val delegate: (Any, Int) -> Unit = { _, _ -> click = true }
        adapter.register(object : RecyctItemBase() {
            override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
                return object : RecyctViewHolder(inflater, parent, 0) {
                    override fun bind(data: Any, atIndex: Int) {}
                }
            }

        }, 0, delegate)

        adapter.createViewHolder(parent, adapter.getItemViewType(0)) as RecyctViewHolder

        verify(itemView).setOnClickListener(argThat {
            it?.onClick(itemView)
            true
        })
        assert(!click)
    }

    @Test
    fun testItemCount_oneSource() {
        assert(adapter.itemCount == 51)

        adapter.registerHeader(header)
        assert(adapter.itemCount == 52)

        adapter.registerFooter(footer)
        assert(adapter.itemCount == 53)

        adapter.enableLoadMore = true
        adapter.defaultLoadMore { }
        assert(adapter.itemCount == 53)

        adapter.unregisterFooter()
        assert(adapter.itemCount == 53)

        adapter.unregisterHeader()
        assert(adapter.itemCount == 52)

        adapter.enableLoadMore = false
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

        adapter.enableLoadMore = true
        adapter.defaultLoadMore { }
        assert(adapter.getItemViewType(0) == RecyctAdapter.TYPE_HEADER)
        data.onEach { assert(adapter.getItemViewType(it as Int + 1) == RecyctAdapter.TYPE_DEFAULT) }
        assert(adapter.getItemViewType(data.size + 1) == RecyctAdapter.TYPE_LOAD_MORE)

        adapter.unregisterFooter()
        assert(adapter.getItemViewType(0) == RecyctAdapter.TYPE_HEADER)
        data.onEach { assert(adapter.getItemViewType(it as Int + 1) == RecyctAdapter.TYPE_DEFAULT) }
        assert(adapter.getItemViewType(data.size + 1) == RecyctAdapter.TYPE_LOAD_MORE)

        adapter.enableLoadMore = false
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
        adapter.also(::mockAdapter)

        try {
            adapter.onCreateViewHolder(parent, 0)
            assert(false) { "Not register item yet" }
        } catch (e: Error) {
            assert(e.message == "No RecyctItem registered.")
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
        header = defaultRecyctItem()

        try {
            adapter.onCreateViewHolder(parent, RecyctAdapter.TYPE_HEADER) as RecyctViewHolder
            assert(false) { "No header item registered." }
        } catch (e: Error) {
            assert(e.message == "No RecyctItem registered.")
        }

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
        footer = defaultRecyctItem()

        try {
            adapter.onCreateViewHolder(parent, RecyctAdapter.TYPE_FOOTER) as RecyctViewHolder
            assert(false) { "No footer item registered." }
        } catch (e: Error) {
            assert(e.message == "No RecyctItem registered.")
        }

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
        `when`(itemView.findViewById<View>(R.id.progress)).thenReturn(mock(View::class.java))
        `when`(itemView.findViewById<View>(R.id.retryButton)).thenReturn(mock(View::class.java))
        try {
            adapter.onCreateViewHolder(parent, RecyctAdapter.TYPE_LOAD_MORE) as RecyctViewHolder
            assert(false) { "No load more item registered." }
        } catch (e: Error) {
            assert(e.message == "No RecyctItem registered.")
        }

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
        initData.forEach {
            adapter.bindViewHolder(vh, it)
            assert(vh.currentItemIndex == it)
            assert(vh.currentData == it)
        }

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
        var footerItem = defaultRecyctItem()
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

        footerItem = defaultRecyctItem()
        adapter.registerFooter(footerItem)
        (0..(itemCount - 1)).map {
            recyctViewHolder(inflater, parent) { data, atIndex ->
                when {
                    atIndex == itemCount - 1 && data == null -> assert(true)
                    else -> assert(data as Int == atIndex)
                }
            }
        }.forEachIndexed { index, inVh -> adapter.bindViewHolder(inVh, index) }
    }

    @Test
    fun testBindViewHolder_loadMore() {
        var startLoadMore = false
        val progress = mock(View::class.java)
        val retryButton = mock(View::class.java)
        `when`(itemView.findViewById<View>(R.id.progress)).thenReturn(progress)
        `when`(itemView.findViewById<View>(R.id.retryButton)).thenReturn(retryButton)

        adapter.apply {
            register(object : RecyctItemBase() {
                override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
                    return recyctViewHolder(inflater, parent) { data, atIndex ->
                        assert(data as Int == atIndex)
                    }
                }
            }.apply { initData = "load more" })
            defaultLoadMore { startLoadMore = true }
            enableLoadMore = true
        }
        val itemCount = adapter.itemCount
        (0..(itemCount - 1)).map {
            adapter.createViewHolder(parent, adapter.getItemViewType(it))
        }.forEachIndexed { index, inVh -> adapter.bindViewHolder(inVh, index) }

        verify(itemView).post(argThat {
            it.run()
            true
        })
        verify(retryButton).setOnClickListener(argThat {
            it?.onClick(retryButton)
            true
        })
        assert(startLoadMore)

        adapter.isLoadMoreFail = true
        (0..(itemCount - 1)).map {
            adapter.createViewHolder(parent, adapter.getItemViewType(it))
        }.forEachIndexed { index, inVh -> adapter.bindViewHolder(inVh, index) }
    }

    @Test
    fun testBindViewHolder_loadMoreNoCallback() {
        val progress = mock(View::class.java)
        val retryButton = mock(View::class.java)
        `when`(itemView.findViewById<View>(R.id.progress)).thenReturn(progress)
        `when`(itemView.findViewById<View>(R.id.retryButton)).thenReturn(retryButton)

        adapter.apply {
            register(object : RecyctItemBase() {
                override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
                    return recyctViewHolder(inflater, parent) { data, atIndex ->
                        assert(data as Int == atIndex)
                    }
                }
            }.apply { initData = "load more" })
            defaultLoadMore()
            enableLoadMore = true
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
    fun testBindViewHolder_invalid() {
        val initData = data
        val vh = object : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {}

        try {
            initData.forEach { adapter.bindViewHolder(vh, it) }
            assert(false) { "ViewHolder must be RecyctViewHolder" }
        } catch (e: Error) {
            assert(e.message == "ViewHolder is not a RecycHolder")
        }
    }

    @Test
    fun testUpdateWithNoItem() {
        adapter.updateHeader("updateHeader")
        adapter.updateFooter("updateFooter")
    }

    @Test
    fun testBindViewHolder_noCallback() {
        `when`(itemView.findViewById<View>(R.id.progress)).thenReturn(mock(View::class.java))
        `when`(itemView.findViewById<View>(R.id.retryButton)).thenReturn(mock(View::class.java))

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
            enableLoadMore = true
            defaultLoadMore { }
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

    @Test
    fun testBindFooter_noData() {
        var isBind = false
        data.clear()
        adapter.registerFooter(object : RecyctItemBase() {
            override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
                return object : RecyctViewHolder(inflater, parent, 0) {
                    override fun bind(data: Any, atIndex: Int) {
                        isBind = data as Boolean
                        assert(context == context)
                    }
                }
            }

        }, true)
        adapter.bindViewHolder(adapter.createViewHolder(parent, RecyctAdapter.TYPE_FOOTER), 0)
        assert(isBind)
    }

    @Test
    fun testOthers_isLoadMoreFail() {
        invokePropertyDelegateFunctions(adapter, adapter::class.java.getDeclaredField("isLoadMoreFail\$delegate"))
        invokePropertyDelegateFunctions(adapter, adapter::class.java.getDeclaredField("enableLoadMore\$delegate"))
    }

    @Test
    fun testOthers_itemDataPair() {
        adapter::class.java.getDeclaredMethod("itemDataPair", Int::class.java, Function1::class.java).run {
            isAccessible = true
            invoke(adapter, 0, { _: Int -> RecyctAdapter.TYPE_HEADER })
            invoke(adapter, 0, { _: Int -> RecyctAdapter.TYPE_FOOTER })
        }
    }

    @Test
    fun testOthers_viewHolder() {
        val vh = recyctViewHolder(inflater, parent) { _, _ -> }
        val delegateField = RecyctViewHolder::class.java.getDeclaredField("clickDelegate\$delegate")
        delegateField.javaClass.methods.map {
            it.isAccessible = true
        }
        delegateField.isAccessible = true
        val lambdaFunction = delegateField.get(vh)
        lambdaFunction.javaClass.declaredFields.map { field ->
            field.isAccessible = true
            val fieldInstance = field.get(lambdaFunction)
            fieldInstance?.javaClass?.declaredMethods?.map {
                it.isAccessible = true
                if (it.parameterTypes.isNotEmpty()) {
                    when (it.parameterTypes[0].simpleName) {
                        "Object" -> it.invoke(fieldInstance, { _: Any, _: Int -> })
                        "Function2" -> it.invoke(fieldInstance, null)
                        else -> {
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testOthers_createViewHolder() {
        adapter.apply {
            register(mock(RecyctItemBase::class.java))
        }
        try {
            adapter.onCreateViewHolder(parent, RecyctAdapter.TYPE_DEFAULT)
        } catch (e: Error) {
            assert(e.message == "No RecyctItem registered.")
        }
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testOthers_emptyDataCollections() {
        val field = adapter::class.java.getDeclaredField("dataGroup")
        val modifiersField = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
        field.isAccessible = true
        field.set(adapter, arrayOf<List<Any>>())
        adapter.itemCount
    }

    @Test()
    fun testOthers_RecyctItemBaseConstructor() {
        object : RecyctItemBase() {
            override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
                return object : RecyctViewHolder(inflater, parent, 0) {
                    override fun bind(data: Any, atIndex: Int) {}
                }
            }
        }
    }

    private fun invokePropertyDelegateFunctions(parentInstance: Any, delegateField: Field) {
        delegateField.javaClass.methods.map {
            it.isAccessible = true
        }
        delegateField.isAccessible = true
        val lambdaFunction = delegateField.get(parentInstance)
        lambdaFunction.javaClass.declaredFields.map { field ->
            field.isAccessible = true
            val fieldInstance = field.get(lambdaFunction)
            if (fieldInstance !is Boolean) {
                fieldInstance.javaClass.declaredMethods.map {
                    it.isAccessible = true
                    when (it.name) {
                        "getName",
                        "getOwner",
                        "getSignature" -> it.invoke(fieldInstance)
                        else -> ""
                    }
                }
            }
        }
    }

    private fun defaultRecyctItem(): RecyctItem = object : RecyctItemBase() {
        override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
            return recyctViewHolder(inflater, parent)
        }
    }

    private fun recyctViewHolder(inflater: LayoutInflater, parent: ViewGroup, bindAssertions: (data: Any?, atIndex: Int) -> Unit = { _, _ -> }): RecyctViewHolder {
        return object : RecyctViewHolder(inflater, parent, 0) {
            override fun bind(data: Any, atIndex: Int) {
                bindAssertions(data, atIndex)
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
        observableClazz = androidx.recyclerview.widget.RecyclerView::class.java.declaredClasses
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