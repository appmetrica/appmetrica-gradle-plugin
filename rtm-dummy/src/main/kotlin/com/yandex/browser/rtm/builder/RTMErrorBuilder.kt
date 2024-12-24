package com.yandex.browser.rtm.builder

import com.yandex.browser.rtm.RTMBaseBuilder

class RTMErrorBuilder : RTMBaseBuilder() {

    fun setStackTrace(throwable: Throwable?): RTMErrorBuilder {
        return this
    }
}
