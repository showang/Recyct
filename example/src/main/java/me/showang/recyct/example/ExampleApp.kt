package me.showang.recyct.example

import android.app.Application
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.startKoin

class ExampleApp : Application() {

    private val appModule = module {
        single("source1") { (0..10).toMutableList() }
        single("source2") { ('a'..'z').map { data -> data.toString() } }
        single("source3") { ('A'..'Z').map { data -> data.toString() } }
    }

    override fun onCreate() {
        super.onCreate()
        startKoin(listOf(appModule))
    }

}
