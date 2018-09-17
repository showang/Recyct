package me.showang.recyct.items

abstract class RecyctItemBase(override var initData: Any? = null) : RecyctItem {

    override var clickDelegate: ((Any, Int) -> Unit)? = null

}