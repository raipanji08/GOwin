package com.panjirai0110.shared.data

fun interface Subscription {
    fun dispose()

    companion object {
        val None = Subscription {}
    }
}
