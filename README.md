[![](https://jitpack.io/v/showang/Recyct.svg)](https://jitpack.io/#showang/Recyct) [![Build Status](https://travis-ci.org/showang/Recyct.svg?branch=master)](https://travis-ci.org/showang/Recyct) [![Codacy Badge](https://app.codacy.com/project/badge/Grade/5a316ef6e3b64f4b90ab4fd2c265fc66)](https://www.codacy.com/gh/showang/Recyct/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=showang/Recyct&amp;utm_campaign=Badge_Grade) [![codecov](https://codecov.io/gh/showang/Recyct/branch/master/graph/badge.svg)](https://codecov.io/gh/showang/Recyct)

# Recyct
A library which helps you build recycler view easier.

# Usage
There are two cases.

## General Case
Create reuseable RecyctItems and register to RecyctAdapter.

### Adapter
```kotlin
val recycler: RecyclerView

recycler.adapter = RecyctAdapter(dataSource1, dataSource2, ... ).apply {
    register(MyRecyctItem()) { /* (Optional) itemView click callback. */ }
    
    //Header & Footer
    registerHeader(HeaderItem(), headerData, ::onHeaderClick)
    registerFooter(FooterItem(), footerData, ::onFooterClick)
    
    //Load more feature
    loadMoreEnabled = true
    defaultLoadMore { /* Load more actions. */ }
}
```

### RecyctItem
```kotlin
class MyRecyctItem : RecyctItemBase() {

    override fun create(inflater: LayoutInflater, parent: ViewGroup): RecyctViewHolder {
        return object : RecyctViewHolder(inflater, parent, R.layout.item_example) {
	
            private val textView: TextView by id(R.id.exampleText)
            
            override fun bind(data: Any, atIndex: Int) {
                textView.text = context.getString(R.string.label_example, data.toString())
            }
        }
    }
}
```

## Multi ViewHolder Types
Create custom adapter to handle multiple view holder type.
```kotlin
class ExampleAdapter(private val dataSource1: List<Int>,
                     private val dataSource2: List<String>): RecyctAdapter(dataSource1, dataSource2) {

    companion object {
        private const val TYPE_A = 0
        private const val TYPE_B = 1
    }

    init {
        register(MyRecyctItemA(), TYPE_A)
        register(MyRecyctItemB(), TYPE_B)
    }

    override fun customViewHolderTypes(dataIndex: Int): Int {
        return when {
            dataIndex < dataSource1.size + 10 -> TYPE_A
            else -> TYPE_B
        }
    }

}
```

# How to
To get a Git project into your build:

## Step 1. Add the JitPack repository to your build file

gradle
maven
sbt
leiningen
Add it in your root build.gradle at the end of repositories:
```gradle
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
## Step 2. Add the dependency
```gradle
	dependencies {
	        implementation 'com.github.showang:Recyct:0.0.2'
	}
```
