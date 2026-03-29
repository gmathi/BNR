package com.bnr.app.domain.model

data class ChapterContent(
    val chapterId: String,
    val title: String,
    val paragraphs: List<String>,        // cleaned text paragraphs
    val previousChapterUrl: String?,
    val nextChapterUrl: String?
)
