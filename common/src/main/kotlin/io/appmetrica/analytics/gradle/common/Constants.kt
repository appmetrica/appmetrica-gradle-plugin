package io.appmetrica.analytics.gradle.common

const val APPMETRICA_PLUGIN = "appmetrica"
const val APPMETRICA_NDK_PLUGIN = "io.appmetrica.analytics:analytics-ndk-crashes:3.0.0"
const val MAPPING_UPLOAD_URL = "https://api.appmetrica.yandex.ru/management/v1/application/crash/proguard/upload"
const val SYMBOLS_UPLOAD_URL = "https://api.appmetrica.yandex.ru/management/v1/application/crash/android_native/upload"
const val UPLOAD_FAILED_TEMPLATE = "Archive was not uploaded since '%s'. You can manually upload file %s to AppMetrica"
const val MAX_RETRY_COUNT = 3

const val APPMETRICA_ARTIFACT = "io.appmetrica.analytics:analytics" // since looking for any dependency like this
const val APPMETRICA_LEGACY_ARTIFACT = "com.yandex.android:mobmetricalib:" // since looking exactly for this dependency
const val USING_TWO_APPMETRICAS_MESSAGE =
    "Using both $APPMETRICA_ARTIFACT and $APPMETRICA_LEGACY_ARTIFACT is not recommended"

const val RTM_PROJECT_NAME = "appmetrica-build-plugin"
const val RTM_VERSION = "1.2.0"

const val LEGACY_RES_VALUE_PREFIX = "com.yandex.android.appmetrica"
const val RES_VALUE_PREFIX = "io.appmetrica.analytics"

const val LEGACY_BUILD_ID_KEY = "$LEGACY_RES_VALUE_PREFIX.build_id"
const val BUILD_ID_KEY = "$RES_VALUE_PREFIX.build_id"

const val LEGACY_IS_OFFLINE_KEY = "$LEGACY_RES_VALUE_PREFIX.is_offline"
const val IS_OFFLINE_KEY = "$RES_VALUE_PREFIX.is_offline"

const val LEGACY_NDK_ENABLE_KEY = "$LEGACY_RES_VALUE_PREFIX.ndk.enable"
const val NDK_ENABLE_KEY = "$RES_VALUE_PREFIX.ndk.enable"

const val REASON_NOT_COMPATIBLE_WITH_CONFIGURATION_CACHE = "Not supported in current version"
