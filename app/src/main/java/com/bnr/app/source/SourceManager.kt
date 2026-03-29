package com.bnr.app.source

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceManager @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards Source>
) {
    private val sourceMap: Map<String, Source> by lazy {
        sources.associateBy { it.id }
    }

    fun getSource(id: String): Source =
        sourceMap[id] ?: throw IllegalArgumentException("Unknown source id: $id")

    fun getAllSources(): List<Source> = sources.sortedBy { it.name }

    fun getSourceOrNull(id: String): Source? = sourceMap[id]
}
