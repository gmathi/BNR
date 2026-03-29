package com.bnr.app.presentation.navigation

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Explore : Screen("explore")
    object Settings : Screen("settings")

    object NovelDetail : Screen("novel_detail/{sourceId}/{encodedUrl}") {
        fun createRoute(sourceId: String, novelUrl: String): String {
            val encoded = URLEncoder.encode(novelUrl, StandardCharsets.UTF_8.toString())
            return "novel_detail/$sourceId/$encoded"
        }
    }

    object Reader : Screen("reader/{chapterId}") {
        fun createRoute(chapterId: String): String {
            val encoded = URLEncoder.encode(chapterId, StandardCharsets.UTF_8.toString())
            return "reader/$encoded"
        }
    }
}

fun String.decodeUrl(): String =
    URLDecoder.decode(this, StandardCharsets.UTF_8.toString())
