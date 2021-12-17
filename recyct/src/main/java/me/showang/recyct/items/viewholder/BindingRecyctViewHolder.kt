package me.showang.recyct.items.viewholder

import androidx.viewbinding.ViewBinding
import me.showang.recyct.items.RecyctItem

abstract class BindingRecyctViewHolder<Binding : ViewBinding>(
    protected val binding: Binding,
    parentItem: RecyctItem
) : RecyctViewHolder(binding.root, parentItem)
