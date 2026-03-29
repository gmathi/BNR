package com.bnr.app.source.impl

import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.ChapterContent
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.model.NovelStatus
import com.bnr.app.source.FilterOption
import com.bnr.app.source.Source
import com.bnr.app.source.SourceException
import com.bnr.app.source.SourceFilter
import com.bnr.app.source.SourceResult
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject

/**
 * Reference implementation of [Source].
 *
 * ⚠️  This is a STUB — all selectors are illustrative placeholders.
 * Replace the CSS selectors and URL patterns to match your actual target site.
 *
 * To add a real source:
 * 1. Copy this file and rename the class.
 * 2. Fill in [id], [name], [baseUrl], [language].
 * 3. Replace all selector strings (marked with TODO) with site-specific values.
 * 4. Add `@Binds @IntoSet` in SourceModule.kt.
 */
class ExampleSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) : Source {

    // ── Identity ──────────────────────────────────────────────────────────────

    override val id       = "com.source.example"
    override val name     = "Example Novels"
    override val baseUrl  = "https://example-novel-site.com"
    override val language = "en"
    override val requiresCloudflareBypass = false

    // ── Popular novels ────────────────────────────────────────────────────────

    override suspend fun getPopularNovels(page: Int): SourceResult<List<Novel>> =
        runCatching {
            val doc = fetchDoc("$baseUrl/popular?page=$page")
            // TODO: replace selector with actual site CSS selector
            doc.select(".novel-item").map { el ->
                Novel(
                    id       = buildNovelId(el.attr("data-slug")),
                    sourceId = id,
                    url      = absUrl(el.selectFirst("a")?.attr("href")),
                    title    = el.selectFirst(".title")?.text() ?: "Unknown",
                    author   = el.selectFirst(".author")?.text() ?: "",
                    coverUrl = el.selectFirst("img")?.attr("abs:src") ?: "",
                    description = "",
                    genres   = emptyList(),
                    status   = NovelStatus.UNKNOWN
                )
            }
        }.toSourceResult()

    // ── Search ────────────────────────────────────────────────────────────────

    override suspend fun searchNovels(
        query: String,
        page: Int,
        filters: Map<String, String>
    ): SourceResult<List<Novel>> = runCatching {
        // TODO: adjust search URL pattern for target site
        val url = buildString {
            append("$baseUrl/search?q=${query.encodeUrl()}&page=$page")
            filters.forEach { (k, v) -> append("&$k=$v") }
        }
        val doc = fetchDoc(url)
        // TODO: replace selector
        doc.select(".search-result").map { el ->
            Novel(
                id       = buildNovelId(el.attr("data-id")),
                sourceId = id,
                url      = absUrl(el.selectFirst("a")?.attr("href")),
                title    = el.selectFirst(".title")?.text() ?: "Unknown",
                author   = el.selectFirst(".author")?.text() ?: "",
                coverUrl = el.selectFirst("img")?.attr("abs:src") ?: "",
                description = el.selectFirst(".description")?.text() ?: "",
                genres   = emptyList(),
                status   = NovelStatus.UNKNOWN
            )
        }
    }.toSourceResult()

    // ── Novel detail ──────────────────────────────────────────────────────────

    override suspend fun getNovelDetail(novelUrl: String): SourceResult<Novel> =
        runCatching {
            val doc = fetchDoc(novelUrl)
            // TODO: replace all selectors below
            val title  = doc.selectFirst("h1.novel-title")?.text() ?: "Unknown"
            val author = doc.selectFirst(".author-name")?.text() ?: ""
            val cover  = doc.selectFirst(".novel-cover img")?.attr("abs:src") ?: ""
            val desc   = doc.selectFirst(".novel-desc")?.text() ?: ""
            val genres = doc.select(".genre-tag").map { it.text() }
            val statusText = doc.selectFirst(".novel-status")?.text() ?: ""
            val status = when {
                statusText.contains("ongoing", ignoreCase = true)   -> NovelStatus.ONGOING
                statusText.contains("completed", ignoreCase = true) -> NovelStatus.COMPLETED
                statusText.contains("hiatus", ignoreCase = true)    -> NovelStatus.HIATUS
                else                                                 -> NovelStatus.UNKNOWN
            }
            val slug = novelUrl.trimEnd('/').substringAfterLast('/')
            Novel(
                id          = buildNovelId(slug),
                sourceId    = id,
                url         = novelUrl,
                title       = title,
                author      = author,
                coverUrl    = cover,
                description = desc,
                genres      = genres,
                status      = status
            )
        }.toSourceResult()

    // ── Chapter list ──────────────────────────────────────────────────────────

    override suspend fun getChapterList(novelUrl: String): SourceResult<List<Chapter>> =
        runCatching {
            val doc = fetchDoc(novelUrl)
            val novelSlug = novelUrl.trimEnd('/').substringAfterLast('/')
            val novelId = buildNovelId(novelSlug)
            // TODO: replace selector; handle paginated chapter lists
            doc.select(".chapter-list li").mapIndexed { index, el ->
                val chapterUrl = absUrl(el.selectFirst("a")?.attr("href"))
                val chapterTitle = el.selectFirst("a")?.text() ?: "Chapter ${index + 1}"
                val chapterId = "${novelId}::${index}"
                Chapter(
                    id            = chapterId,
                    novelId       = novelId,
                    sourceId      = id,
                    url           = chapterUrl,
                    title         = chapterTitle,
                    chapterNumber = (index + 1).toFloat(),
                    uploadedAt    = null
                )
            }
        }.toSourceResult()

    // ── Chapter content ───────────────────────────────────────────────────────

    override suspend fun getChapterContent(chapterUrl: String): SourceResult<ChapterContent> =
        runCatching {
            val doc = fetchDoc(chapterUrl)
            // TODO: replace selector with the actual content container
            val contentEl = doc.selectFirst(".chapter-content")
                ?: throw SourceException.ParseException("Content element not found")

            // Strip scripts, ads, navigation elements
            contentEl.select("script, style, .ads, nav, .navigation").remove()

            val title = doc.selectFirst("h2.chapter-title")?.text()
                ?: doc.title()

            val paragraphs = contentEl.select("p")
                .map { it.text().trim() }
                .filter { it.isNotBlank() }

            // TODO: adjust prev/next link selectors
            val prevUrl = doc.selectFirst("a.prev-chapter")?.attr("abs:href")
            val nextUrl = doc.selectFirst("a.next-chapter")?.attr("abs:href")

            // Derive chapterId from URL
            val chapterId = chapterUrl.trimEnd('/').substringAfterLast('/')

            ChapterContent(
                chapterId            = chapterId,
                title                = title,
                paragraphs           = paragraphs,
                previousChapterUrl   = prevUrl?.takeIf { it.isNotBlank() },
                nextChapterUrl       = nextUrl?.takeIf { it.isNotBlank() }
            )
        }.toSourceResult()

    // ── Filters ───────────────────────────────────────────────────────────────

    override suspend fun getAvailableFilters(): List<SourceFilter> = listOf(
        SourceFilter(
            key = "status",
            label = "Status",
            options = listOf(
                FilterOption("", "All"),
                FilterOption("ongoing", "Ongoing"),
                FilterOption("completed", "Completed"),
                FilterOption("hiatus", "Hiatus")
            )
        ),
        SourceFilter(
            key = "sort",
            label = "Sort By",
            options = listOf(
                FilterOption("popular", "Popular"),
                FilterOption("latest", "Latest Update"),
                FilterOption("new", "Newly Added")
            )
        )
    )

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun fetchDoc(url: String): Document {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        val body = response.body?.string()
            ?: throw SourceException.NetworkException("Empty response body for $url")
        if (!response.isSuccessful) {
            throw SourceException.NetworkException("HTTP ${response.code} for $url")
        }
        return Jsoup.parse(body, url)
    }

    private fun buildNovelId(slug: String): String = "$id::$slug"

    private fun absUrl(href: String?): String {
        if (href == null) return ""
        return if (href.startsWith("http")) href else "$baseUrl/$href".replace("//", "/")
            .replace("$baseUrl/https:", "https:")
            .replace("$baseUrl/http:", "http:")
    }

    private fun String.encodeUrl(): String =
        java.net.URLEncoder.encode(this, "UTF-8")

    private fun <T> Result<T>.toSourceResult(): SourceResult<T> =
        fold(
            onSuccess = { SourceResult.Success(it) },
            onFailure = { throwable ->
                val ex = when (throwable) {
                    is SourceException -> throwable
                    is java.net.UnknownHostException,
                    is java.net.SocketTimeoutException ->
                        SourceException.NetworkException(throwable.message ?: "Network error", throwable)
                    else ->
                        SourceException.ParseException(throwable.message ?: "Unknown error", throwable)
                }
                SourceResult.Error(ex)
            }
        )
}
