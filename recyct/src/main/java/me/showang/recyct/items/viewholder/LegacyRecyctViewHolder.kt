package me.showang.recyct.items.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import me.showang.recyct.didSetNullable
import me.showang.recyct.items.RecyctItem
import kotlin.properties.ObservableProperty

abstract class LegacyRecyctViewHolder(
    @LayoutRes resId: Int,
    inflater: LayoutInflater,
    parent: ViewGroup?,
) : RecyctViewHolder(inflater.inflate(resId, parent, false)) {

    protected fun <T : View> id(@IdRes resId: Int) =
        object : ObservableProperty<T>(itemView.findViewById<T>(resId)) {}
}