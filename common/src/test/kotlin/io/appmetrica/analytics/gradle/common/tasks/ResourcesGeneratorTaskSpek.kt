package io.appmetrica.analytics.gradle.common.tasks

import com.google.gson.Gson
import io.appmetrica.analytics.gradle.common.BUILD_ID_KEY
import io.appmetrica.analytics.gradle.common.BuildInfo
import io.appmetrica.analytics.gradle.common.IS_OFFLINE_KEY
import io.appmetrica.analytics.gradle.common.LEGACY_BUILD_ID_KEY
import io.appmetrica.analytics.gradle.common.LEGACY_IS_OFFLINE_KEY
import io.appmetrica.analytics.gradle.common.LEGACY_NDK_ENABLE_KEY
import io.appmetrica.analytics.gradle.common.MappingType
import io.appmetrica.analytics.gradle.common.NDK_ENABLE_KEY
import io.appmetrica.analytics.gradle.common.utils.Log
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

object ResourcesGeneratorTaskSpek : Spek({

    beforeGroup {
        Log.setLogger(ProjectBuilder.builder().build().logger)
    }

    describe("taskAction") {
        it("generates build info file with correct structure") {
            val project = ProjectBuilder.builder().build()
            val buildDir = project.layout.buildDirectory.dir("test").get().asFile.apply { mkdirs() }
            val infoFile = File(buildDir, "info.txt")
            val resDir = File(buildDir, "res")

            val task = project.tasks.create("genRes", ResourcesGeneratorTask::class.java) {
                it.versionName.set("2.0.0")
                it.versionCode.set(42)
                it.mappingType.set(MappingType.R8)
                it.splitVersionCodes.set(setOf(1, 2))
                it.offline.set(false)
                it.ndkEnable.set(true)
                it.buildInfoFile.set(infoFile)
                it.resDirectory.set(resDir)
            }

            task.taskAction()

            val buildInfo = Gson().fromJson(infoFile.readText(), BuildInfo::class.java)
            assertThat(buildInfo.buildId).isNotEmpty()
            assertThat(buildInfo.versionName).isEqualTo("2.0.0")
            assertThat(buildInfo.versionCode).isEqualTo(42)
            assertThat(buildInfo.mappingType).isEqualTo(MappingType.R8)
            assertThat(buildInfo.splitVersionCodes).containsExactlyInAnyOrder(1, 2)
        }

        it("generates resource xml with build id and flags") {
            val project = ProjectBuilder.builder().build()
            val buildDir = project.layout.buildDirectory.dir("test2").get().asFile.apply { mkdirs() }
            val resDir = File(buildDir, "res")

            val task = project.tasks.create("genRes2", ResourcesGeneratorTask::class.java) {
                it.versionName.set("1.0")
                it.versionCode.set(1)
                it.mappingType.set(MappingType.PROGUARD)
                it.splitVersionCodes.set(emptySet())
                it.offline.set(true)
                it.ndkEnable.set(false)
                it.buildInfoFile.set(File(buildDir, "info.txt"))
                it.resDirectory.set(resDir)
            }

            task.taskAction()

            val resourceXml = File(resDir, "values/appmetrica_resources.xml").readText()
            assertThat(resourceXml).contains("<string name=\"$LEGACY_BUILD_ID_KEY\">")
            assertThat(resourceXml).contains("<string name=\"$BUILD_ID_KEY\">")
            assertThat(resourceXml).contains("<bool name=\"$LEGACY_IS_OFFLINE_KEY\">true</bool>")
            assertThat(resourceXml).contains("<bool name=\"$IS_OFFLINE_KEY\">true</bool>")
            assertThat(resourceXml).contains("<bool name=\"$LEGACY_NDK_ENABLE_KEY\">false</bool>")
            assertThat(resourceXml).contains("<bool name=\"$NDK_ENABLE_KEY\">false</bool>")
        }

        it("generates keep resources file") {
            val project = ProjectBuilder.builder().build()
            val buildDir = project.layout.buildDirectory.dir("test3").get().asFile.apply { mkdirs() }
            val resDir = File(buildDir, "res")

            val task = project.tasks.create("genRes3", ResourcesGeneratorTask::class.java) {
                it.versionName.set("1.0")
                it.versionCode.set(1)
                it.mappingType.set(MappingType.PROGUARD)
                it.splitVersionCodes.set(emptySet())
                it.offline.set(false)
                it.ndkEnable.set(false)
                it.buildInfoFile.set(File(buildDir, "info.txt"))
                it.resDirectory.set(resDir)
            }

            task.taskAction()

            val keepFile = File(resDir, "raw/keep_appmetrica_resources.xml")
            assertThat(keepFile).exists()
            val content = keepFile.readText()
            assertThat(content).contains("tools:keep=")
            assertThat(content).contains("com.yandex.android.appmetrica")
            assertThat(content).contains("io.appmetrica.analytics")
        }

        it("handles null versionName and versionCode gracefully") {
            val project = ProjectBuilder.builder().build()
            val buildDir = project.layout.buildDirectory.dir("test4").get().asFile.apply { mkdirs() }
            val infoFile = File(buildDir, "info.txt")

            val task = project.tasks.create("genRes4", ResourcesGeneratorTask::class.java) {
                it.versionName.set(null as String?)
                it.versionCode.set(null as Int?)
                it.mappingType.set(MappingType.R8)
                it.splitVersionCodes.set(emptySet())
                it.offline.set(false)
                it.ndkEnable.set(false)
                it.buildInfoFile.set(infoFile)
                it.resDirectory.set(File(buildDir, "res"))
            }

            task.taskAction()

            val buildInfo = Gson().fromJson(infoFile.readText(), BuildInfo::class.java)
            assertThat(buildInfo.versionName).isEmpty()
            assertThat(buildInfo.versionCode).isEqualTo(0)
        }

        it("uses consistent build id across info file and resource file") {
            val project = ProjectBuilder.builder().build()
            val buildDir = project.layout.buildDirectory.dir("test5").get().asFile.apply { mkdirs() }
            val infoFile = File(buildDir, "info.txt")
            val resDir = File(buildDir, "res")

            val task = project.tasks.create("genRes5", ResourcesGeneratorTask::class.java) {
                it.versionName.set("1.0")
                it.versionCode.set(1)
                it.mappingType.set(MappingType.PROGUARD)
                it.splitVersionCodes.set(emptySet())
                it.offline.set(false)
                it.ndkEnable.set(false)
                it.buildInfoFile.set(infoFile)
                it.resDirectory.set(resDir)
            }

            task.taskAction()

            val buildInfo = Gson().fromJson(infoFile.readText(), BuildInfo::class.java)
            val resourceXml = File(resDir, "values/appmetrica_resources.xml").readText()
            assertThat(resourceXml).contains(buildInfo.buildId)
        }
    }
})
