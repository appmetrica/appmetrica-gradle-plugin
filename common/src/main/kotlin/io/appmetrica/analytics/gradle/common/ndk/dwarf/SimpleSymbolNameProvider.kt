package io.appmetrica.analytics.gradle.common.ndk.dwarf

class SimpleSymbolNameProvider(private val symbolName: String?) : SymbolNameProvider {

    override fun getSymbolName() = symbolName
}
