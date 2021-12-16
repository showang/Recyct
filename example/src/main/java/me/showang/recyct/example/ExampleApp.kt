package me.showang.recyct.example

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

class ExampleApp : Application() {

    private val dataModule = module {
        single(named("source1")) { (0..10).toMutableList() }
        single(named("source2")) { ('a'..'z').map { data -> data.toString() } }
        single(named("source3")) { ('A'..'Z').map { data -> data.toString() } }
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ExampleApp)
            modules(dataModule)
        }
    }

}
