package com.bnr.app.domain.model

data class NovelUpdateInfo(
    val novelId: String,
    val newChapterCount: Int,       // chapters added since user last opened this novel
    val knownChapterCount: Int,     // total chapters as of last check
    val lastCheckedAt: Long,        // epoch millis of last background check
    val lastUpdatedAt: Long?        // epoch millis when new chapters were found, null if never
)
