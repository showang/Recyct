package me.showang.recyct.items

abstract class RecyctItemBase : RecyctItem {

    override var initData: Any? = null

    override var clickDelegate: ((Any, Int) -> Unit)? = null

}