package me.showang.recyct.example

import android.app.Application
import android.widget.Toast
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.startKoin

class ExampleApp : Application() {

    private val appModule = module {
        single("source1") { (0..10).toMutableList() }
        single("source2") { ('a'..'z').map { data -> data.toString() }.toMutableList() }
        factory { ExampleAdapter(get("source1"), get("source2"), ::toast) }
    }

    override fun onCreate() {
        super.onCreate()
        startKoin(listOf(appModule))
    }

    private fun toast(type: String): (Any, Int) -> Unit {
        return { data, index ->
            Toast.makeText(this, "$type contain data $data at $index.", Toast.LENGTH_LONG).show()
        }
    }
}
