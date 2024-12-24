package com.yandex.browser.rtm

import com.yandex.browser.rtm.builder.RTMErrorBuilder
import com.yandex.browser.rtm.builder.RTMEventBuilder

class RTMLib {

    companion object {
        fun appHostStaticsBuilder(): AppHostStaticsBuilder {
            return AppHostStaticsBuilder()
        }

        fun builder(projectName: String, version: String, uploadScheduler: RTMUploadScheduler): Builder {
            return Builder()
        }

        fun initializeAppHostStatics(builder: AppHostStaticsBuilder) {
            // do nothing
        }

        fun uploadEventAndWaitResult(eventPayload: String): RTMUploadResult {
            return RTMUploadResult(
                httpResultCode = 200
            )
        }
    }

    fun newErrorBuilder(message: String): RTMErrorBuilder {
        return RTMErrorBuilder()
    }

    fun newEventBuilder(name: String, value: String?): RTMEventBuilder {
        return RTMEventBuilder()
    }

    class AppHostStaticsBuilder {
        fun loggerDelegate(loggerDelegate: LoggerDelegate): AppHostStaticsBuilder {
            return this
        }
    }

    class Builder {
        fun userIdProvider(userIdProvider: Provider<String?>): Builder {
            return this
        }

        fun build(): RTMLib {
            return RTMLib()
        }
    }
}
