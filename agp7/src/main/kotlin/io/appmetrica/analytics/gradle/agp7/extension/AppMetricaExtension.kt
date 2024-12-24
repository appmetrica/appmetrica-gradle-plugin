package io.appmetrica.analytics.gradle.agp7.extension

import com.android.build.gradle.api.ApplicationVariant
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.util.ConfigureUtil
import java.io.File

open class AppMetricaExtension {

    var enable: (ApplicationVariant) -> Boolean = { it.buildType.name == "release" }
    var postApiKey: (ApplicationVariant) -> String = { "" }
    var offline: (ApplicationVariant) -> Boolean = { false }
    var mappingFile: ((ApplicationVariant) -> File?)? = null
    var allowTwoAppMetricas: (ApplicationVariant) -> Boolean = { false }
    var enableAnalytics: Boolean = true

    var ndk = AppMetricaNdkExtension()

    fun ndk(closure: Closure<Any>) = ConfigureUtil.configure(closure, ndk)

    fun ndk(action: Action<in AppMetricaNdkExtension>) = action.execute(ndk)

    fun setPostApiKey(postApiKey: String) {
        this.postApiKey = { postApiKey }
    }

    fun setMappingBuildTypes(mappingBuildTypes: List<String>) {
        this.enable = { mappingBuildTypes.contains(it.buildType.name) }
    }

    fun setOffline(isOffline: Boolean) {
        this.offline = { isOffline }
    }
}
