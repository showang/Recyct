package me.showang.recyct.example

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.showang.recyct.RecyctAdapter
import me.showang.recyct.example.databinding.*
import me.showang.recyct.items.RecyctItem
import me.showang.recyct.items.viewholder.BindingRecyctViewHolder
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class MainActivity : AppCompatActivity() {

    private val data1: List<Int> by inject(named("source1"))
    private val data2: List<String> by inject(named("source2"))
    private val data3: List<String> by inject(named("source3"))

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private var adapter: RecyctAdapter? = null
    private var viewTypeDelegate: (Int) -> Int = { index ->
        when {
            index < data1.size -> 0
            else -> 1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.apply {
            initRecyclerView()
        }.root)
    }

    private val sectionTitleData = mutableListOf("Section 1", "Section 2")

    private fun ActivityMainBinding.initRecyclerView() {
        recycler.adapter = object : RecyctAdapter(data1, data2) {
            override fun customViewHolderTypes(dataIndex: Int): Int = viewTypeDelegate(dataIndex)
        }.apply {
            registerHeader(HeaderItem(), -1, onHeaderClick)
//            registerFooter(FooterItem(), 100, onFooterClick)
            register(MyRecyctItem("A", Color.RED), 0, toast("Type A"))
            register(MyRecyctItem("B", Color.LTGRAY), 1, toast("Type B"))
            sectionsByGroup(SectionTitleItem()) { index -> sectionTitleData[index] }
            enableLoadMore = true
            defaultLoadMore(::onLoadMore)
            adapter = this
        }
    }

    private var loadMoreJob: Job? = null

    private fun onLoadMore() {
        if (loadMoreJob != null) return
        loadMoreJob = CoroutineScope(Main).launch {
            adapter?.run {
                delay(3000)
                enableLoadMore = false
                register(MyRecyctItem("C", Color.MAGENTA), 2, toast("Type C"))
                viewTypeDelegate = { index ->
                    when {
                        index < data1.size -> 0
                        index < data1.size + data2.size -> 1
                        else -> 2
                    }
                }
                sectionTitleData.add("Section 3")
                appendDataGroup(data3)
                notifyDataAppended(data3.size)
            }
        }
    }

    private fun toast(typeName: String): (Any, Int, Int) -> Unit = { data, dataIndex, itemIndex ->
        Toast.makeText(
            this,
            "$typeName contain $data at data[$dataIndex] and item[$itemIndex].",
            Toast.LENGTH_LONG
        )
            .show()
    }

    private val onHeaderClick: (Any, Int, Int) -> Unit = { data, _, _ ->
        (data as? Int)?.let { adapter?.updateHeader(it + 1) }
    }

    private val onFooterClick: (Any, Int, Int) -> Unit = { data, _, _ ->
        (data as? Int)?.let { adapter?.updateFooter(it - 1) }
    }

}

class HeaderItem : RecyctItem() {
    override fun create(inflater: LayoutInflater, parent: ViewGroup) =
        object : BindingRecyctViewHolder<ItemHeaderBinding>(
            ItemHeaderBinding.inflate(inflater, parent, false)
        ) {
            override fun bind(data: Any, dataIndex: Int, itemIndex: Int) {
                binding.headerText.text =
                    itemView.context.getString(R.string.label_header, data.toString())
            }
        }
}

class FooterItem : RecyctItem() {
    override fun create(inflater: LayoutInflater, parent: ViewGroup) = object :
        BindingRecyctViewHolder<ItemFooterBinding>(
            ItemFooterBinding.inflate(inflater, parent, false)
        ) {
        override fun bind(data: Any, dataIndex: Int, itemIndex: Int) {
            binding.footerText.text =
                itemView.context.getString(R.string.label_footer, data.toString())
        }
    }
}

class SectionTitleItem : RecyctItem() {
    override fun create(inflater: LayoutInflater, parent: ViewGroup) =
        object : BindingRecyctViewHolder<ItemSectionTitleBinding>(
            ItemSectionTitleBinding.inflate(inflater, parent, false)
        ) {
            override fun bind(data: Any, dataIndex: Int, itemIndex: Int) {
                (data as? String)?.let {
                    binding.sectionTitleTitleText.text = it
                }
                itemView.setOnClickListener {
                    Toast.makeText(
                        it.context,
                        "Section title at group index[$dataIndex] item index[$itemIndex]",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
}

class MyRecyctItem(private val type: String, private val color: Int) : RecyctItem() {
    override fun create(inflater: LayoutInflater, parent: ViewGroup) = object :
        BindingRecyctViewHolder<ItemBasicBinding>(
            ItemBasicBinding.inflate(inflater, parent, false)
        ) {
        override fun bind(data: Any, dataIndex: Int, itemIndex: Int) {
            val dataString = when (data) {
                is Int -> data.toString()
                is String -> data
                else -> "Unknown"
            }
            binding.run {
                typeLabelText.text = itemView.context.getString(R.string.label_type, type)
                typeLabelText.setTextColor(color)
                contentText.text = itemView.context.getString(R.string.label_item, dataString)
            }
        }
    }
}
