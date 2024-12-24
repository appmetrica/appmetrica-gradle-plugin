package com.yandex.browser.rtm

fun interface Provider<T> {

    fun get(): T
}
