package com.manta.common

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

@Suppress("UNCHECKED_CAST")
@BindingAdapter("submitList")
fun RecyclerView.bindSubmitList(itemList: List<Any>?) {
    (adapter as? ListAdapter<Any, *>)?.submitList(itemList)
}

@BindingAdapter("networkImage")
fun ImageView.setNetWorkImage(uri: String?) {
    Glide.with(context).load(uri).into(this)
}

@BindingAdapter("isVisible")
fun View.setIsVisible(isVisible: Boolean) {
    visibility = if (isVisible) {
        View.VISIBLE
    } else {
        View.INVISIBLE
    }
}


@BindingAdapter("isGone")
fun View.setIsGone(isGone: Boolean) {
    visibility = if (isGone) {
        View.VISIBLE
    } else {
        View.GONE
    }
}