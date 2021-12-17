package me.showang.recyct

import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

fun <T> didSet(initialValue: T, delegate: (T) -> Unit) = object : ObservableProperty<T>(initialValue) {
    override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = delegate(newValue)
}

fun <T> didSetNullable(delegate: (T?) -> Unit) = didSet<T?>(null, delegate)