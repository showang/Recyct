package me.showang.recyct.example

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.showang.recyct.RecyctAdapter
import me.showang.recyct.RecyctViewHolder
import me.showang.recyct.items.RecyctItemBase
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val dataSource2: MutableList<String> by inject("source2")

    private val adapter: ExampleAdapter by inject()

    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recycler.adapter = adapter.apply {
            registerHeader(HeaderItem(), -1, ::onHeaderClick)
            registerFooter(FooterItem(), 100, ::onFooterClick)
            enableLoadMore = true
            defaultLoadMore {
                uiScope.launch {
                    delay(3000)
                    adapter.enableLoadMore = false
                    val newData = ('a'..'z').map { it.toString() }
                    dataSource2.addAll(newData)
                    adapter.notifyDataAppended(newData.size)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun onHeaderClick(data: Any, index: Int) {
        (data as? Int)?.let { adapter.updateHeader(data + 1) }
    }

    private fun onFooterClick(data: Any, index: Int) {
        (data as? Int)?.let { adapter.updateFooter(data - 1) }
    }

}

class ExampleAdapter(private val dataSource1: List<Int>,
                     private val dataSource2: List<String>,
                     toastDelegate: (String) -> ((Any, Int) -> Unit))
    : RecyctAdapter(dataSource1, dataSource2) {

    companion object {
        private const val TYPE_A = 0
        private const val TYPE_B = 1
    }

    init {
        register(MyRecyctItemA(), TYPE_A, toastDelegate("Type A"))
        register(MyRecyctItemB(), TYPE_B, toastDelegate("Type B"))
        sectionsByGroup(SectionTitleItem(), listOf("Section 1", "Section 2"))
    }

    override fun customViewHolderTypes(dataIndex: Int): Int {
//        return when {
//            dataIndex < dataSource1.size + 10
//                    || dataIndex > dataSource1.size + 25 -> TYPE_A
//            else -> TYPE_B
//        }
        return when {
            dataIndex < dataSource1.size -> TYPE_A.also { println("TYPE_A on $dataIndex") }
            else -> TYPE_B.also { println("TYPE_B on $dataIndex") }
        }
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
        }
    }
}

class MyRecyctItemA : RecyctItemBase() {

    override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
        return object : RecyctViewHolder(inflater, parent, R.layout.item_basic) {

            private val contentText: TextView = itemView.findViewById(R.id.contentText)

            override fun bind(data: Any, atIndex: Int) {
                val dataString = when (data) {
                    is Int -> data.toString()
                    is String -> data
                    else -> "Unknown"
                }
                contentText.text = itemView.context.getString(R.string.label_item, "TypeA", dataString)
            }

        }
    }

}

class MyRecyctItemB : RecyctItemBase() {

    override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
        return object : RecyctViewHolder(inflater, parent, R.layout.item_basic) {

            init {
                itemView.background = ColorDrawable(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
            }

            private val contentText: TextView = itemView.findViewById(R.id.contentText)

            override fun bind(data: Any, atIndex: Int) {
                when (data) {
                    is String -> contentText.text = itemView.context.getString(R.string.label_item, "TypeB", data)
                }
            }

        }
    }

}