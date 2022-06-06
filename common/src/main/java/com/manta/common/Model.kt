package com.manta.common

interface Model<T> {
    fun areItemsTheSame(other: T): Boolean
    fun areContentsTheSame(other: T): Boolean
    fun bindingVariableId() : Int
}