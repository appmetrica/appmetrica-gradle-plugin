package io.appmetrica.analytics.gradle.common

import com.google.gson.annotations.SerializedName

data class BuildInfo(
    @SerializedName("build_id") val buildId: String,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("mapping_type") val mappingType: MappingType,
    @SerializedName("split_version_codes") val splitVersionCodes: Set<Int>
)

enum class MappingType {
    PROGUARD, R8
}
