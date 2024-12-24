package com.yandex.browser.rtm

interface LoggerDelegate {

    fun d(tag: String, msg: String)

    fun e(tag: String, msg: String, throwable: Throwable?)

    fun w(tag: String, msg: String, throwable: Throwable?)
}
