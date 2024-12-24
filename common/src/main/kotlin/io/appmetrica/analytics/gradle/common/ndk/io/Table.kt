package io.appmetrica.analytics.gradle.common.ndk.io

import kotlin.math.max

class Table(
    private val data: List<Map<String, Any>>,
    private val columnKeys: List<String> = data.firstOrNull()?.keys?.toList() ?: emptyList()
) {

    fun print(printer: (String) -> Unit) {
        printer(firstSeparator())
        printer(header())
        printer(separator())
        data.forEach {
            printer(row(columnKeys.map { key -> it.getValue(key) }))
        }
        printer(lastSeparator())
    }

    private enum class ColumnAlignment {
        LEFT, CENTER, RIGHT
    }

    private val columnSizes =
        columnKeys.map { key -> max(key.length, data.maxOfOrNull { "${it[key]}".length } ?: 0) + 2 }

    private fun firstSeparator() =
        "┌${columnSizes.joinToString("┬") { "─".repeat(it) }}┐"

    private fun separator() =
        "├${columnSizes.joinToString("┼") { "─".repeat(it) }}┤"

    private fun lastSeparator() =
        "└${columnSizes.joinToString("┴") { "─".repeat(it) }}┘"

    private fun header() =
        "│${columnKeys.mapIndexed { index, key -> column(index, key, ColumnAlignment.CENTER) }.joinToString("│")}│"

    private fun row(rowData: List<Any>) =
        "│${rowData.mapIndexed { index, data -> column(index, data, ColumnAlignment.LEFT) }.joinToString("│")}│"

    private fun column(index: Int, columnDate: Any, alignment: ColumnAlignment) = when (alignment) {
        ColumnAlignment.LEFT -> " $columnDate${" ".repeat(columnSizes[index] - "$columnDate".length - 1)}"
        ColumnAlignment.CENTER -> {
            val halfSpace = (columnSizes[index] - "$columnDate".length) / 2
            "${" ".repeat(halfSpace)}$columnDate${" ".repeat(columnSizes[index] - "$columnDate".length - halfSpace)}"
        }
        ColumnAlignment.RIGHT -> "${" ".repeat(columnSizes[index] - "$columnDate".length - 1)}$columnDate "
    }
}
