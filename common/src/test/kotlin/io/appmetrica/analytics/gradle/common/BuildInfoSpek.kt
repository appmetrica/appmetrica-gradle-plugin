package io.appmetrica.analytics.gradle.common

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object BuildInfoSpek : Spek({
    describe("BuildInfo serialization") {
        it("serializes and deserializes with correct JSON keys") {
            val buildInfo = BuildInfo(
                buildId = "test-build-id",
                versionName = "1.0.0",
                versionCode = 42,
                mappingType = MappingType.R8,
                splitVersionCodes = setOf(1, 2, 3)
            )
            val gson = Gson()
            val json = gson.toJson(buildInfo)

            assertThat(json).contains("\"build_id\"")
            assertThat(json).contains("\"version_name\"")
            assertThat(json).contains("\"version_code\"")
            assertThat(json).contains("\"mapping_type\"")
            assertThat(json).contains("\"split_version_codes\"")

            val deserialized = gson.fromJson(json, BuildInfo::class.java)
            assertThat(deserialized).isEqualTo(buildInfo)
        }

        it("serializes empty split version codes") {
            val buildInfo = BuildInfo(
                buildId = "id",
                versionName = "",
                versionCode = 0,
                mappingType = MappingType.PROGUARD,
                splitVersionCodes = emptySet()
            )
            val json = Gson().toJson(buildInfo)
            val deserialized = Gson().fromJson(json, BuildInfo::class.java)

            assertThat(deserialized.splitVersionCodes).isEmpty()
            assertThat(deserialized.mappingType).isEqualTo(MappingType.PROGUARD)
        }
    }
})
