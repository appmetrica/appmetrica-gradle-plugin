package io.appmetrica.analytics.gradle.common

import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import java.util.Locale

/* ktlint-disable appmetrica-rules:no-top-level-members */
@Suppress("SwallowedException")
fun <T : Task> TaskContainer.findNamed(name: String, type: Class<T>): TaskProvider<T>? = try {
    this.named(name, type)
} catch (u: UnknownDomainObjectException) {
    null
}

fun String.uppercaseFirstChar(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
/* ktlint-enable appmetrica-rules:no-top-level-members */
