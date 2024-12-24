package io.appmetrica.analytics.gradle.common

import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

fun <T : Task> TaskContainer.findNamed(name: String, type: Class<T>): TaskProvider<T>? = try {
    this.named(name, type)
} catch (u: UnknownDomainObjectException) {
    null
}
