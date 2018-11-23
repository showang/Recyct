package me.showang.recyct.example

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.showang.recyct.RecyctAdapter
import me.showang.recyct.RecyctViewHolder
import me.showang.recyct.items.RecyctItemBase
import org.jetbrains.anko.textColor
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val data1: List<Int> by inject("source1")
    private val data2: List<String> by inject("source2")
    private val data3: List<String> by inject("source3")

    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val adapter get() = recycler?.adapter as? RecyctAdapter
    private var viewTypeDelegate: (Int) -> Int = { index ->
        when {
            index < data1.size -> 0
            else -> 1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recycler.adapter = object : RecyctAdapter(data1, data2) {
            override fun customViewHolderTypes(dataIndex: Int): Int = viewTypeDelegate(dataIndex)
        }.apply {
            registerHeader(HeaderItem(), -1, onHeaderClick)
            registerFooter(FooterItem(), 100, onFooterClick)
            register(MyRecyctItem("A", Color.RED), 0, toast("Type A"))
            register(MyRecyctItem("B", Color.LTGRAY), 1, toast("Type B"))
            sectionsByGroup(SectionTitleItem(), listOf("Section 1", "Section 2"))
            enableLoadMore = true
            defaultLoadMore(::onLoadMore)
        }
    }

    private fun onLoadMore() {
        uiScope.launch {
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
                appendDataGroup(data3, "Section 3")
                notifyDataAppended(data3.size)
            }
        }
    }

    private fun toast(type: String): (Any, Int) -> Unit = { data, index ->
        Toast.makeText(this, "$type contain data $data at item index: $index.", Toast.LENGTH_LONG).show()
    }

    private val onHeaderClick: (Any, Int) -> Unit = { data, _ ->
        (data as? Int)?.let { adapter?.updateHeader(data + 1) }
    }

    private val onFooterClick: (Any, Int) -> Unit = { data, _ ->
        (data as? Int)?.let { adapter?.updateFooter(data - 1) }
    }

}

class HeaderItem : RecyctItemBase() {
    override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
        return object : RecyctViewHolder(inflater, parent, R.layout.item_header) {
            private val textView: TextView = itemView.findViewById(R.id.headerText)
            override fun bind(data: Any, atIndex: Int) {
                textView.text = itemView.context.getString(R.string.label_header, data.toString())
            }
        }
    }
}

class FooterItem : RecyctItemBase() {
    override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
        return object : RecyctViewHolder(inflater, parent, R.layout.item_footer) {
            private val textView: TextView = itemView.findViewById(R.id.footerText)
            override fun bind(data: Any, atIndex: Int) {
                textView.text = itemView.context.getString(R.string.label_footer, data.toString())
            }
        }
    }
}

class SectionTitleItem : RecyctItemBase() {
    override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder = object : RecyctViewHolder(inflater, parent, R.layout.item_section_title) {
        val textView: TextView = itemView.findViewById(R.id.sectionTitle_titleText)
        override fun bind(data: Any, atIndex: Int) {
            (data as? String)?.let {
                textView.text = it
            }
            itemView.setOnClickListener {
                Toast.makeText(it.context, "Section title at group index: $atIndex", Toast.LENGTH_LONG).show()
            }
        }
    }
}

class MyRecyctItem(private val type: String, private val color: Int) : RecyctItemBase() {

    override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
        return object : RecyctViewHolder(inflater, parent, R.layout.item_basic) {

            private val contentText: TextView = itemView.findViewById(R.id.contentText)
            private val labelText: TextView = itemView.findViewById(R.id.typeLabelText)

            override fun bind(data: Any, atIndex: Int) {
                val dataString = when (data) {
                    is Int -> data.toString()
                    is String -> data
                    else -> "Unknown"
                }
                labelText.text = itemView.context.getString(R.string.label_type, type)
                labelText.textColor = color
                contentText.text = itemView.context.getString(R.string.label_item, dataString)
            }

        }
    }

}