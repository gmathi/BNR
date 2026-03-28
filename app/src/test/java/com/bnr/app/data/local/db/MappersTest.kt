package com.bnr.app.data.local.db

import com.bnr.app.data.local.db.entity.ChapterEntity
import com.bnr.app.data.local.db.entity.NovelEntity
import com.bnr.app.data.local.db.entity.NovelReaderSettingsEntity
import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.DownloadState
import com.bnr.app.domain.model.Novel
import com.bnr.app.domain.model.NovelReaderSettings
import com.bnr.app.domain.model.NovelStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    // ═══════════════════════════════════════════════════════════════════════════
    // Novel
    // ═══════════════════════════════════════════════════════════════════════════

    private fun buildNovel(
        genres: List<String> = listOf("Fantasy", "Action"),
        status: NovelStatus = NovelStatus.ONGOING,
        inLibrary: Boolean = true,
        lastReadChapterId: String? = "ch-42",
        addedToLibraryAt: Long? = 1_700_000_000L,
        chapterCount: Int = 99
    ) = Novel(
        id = "com.source.rr::the-wandering-inn",
        sourceId = "com.source.rr",
        url = "https://royalroad.com/fiction/1/the-wandering-inn",
        title = "The Wandering Inn",
        author = "pirateaba",
        coverUrl = "https://example.com/cover.jpg",
        description = "A story about an inn.",
        genres = genres,
        status = status,
        inLibrary = inLibrary,
        lastReadChapterId = lastReadChapterId,
        addedToLibraryAt = addedToLibraryAt,
        chapterCount = chapterCount
    )

    @Test
    fun `Novel_toEntity maps all fields correctly`() {
        val novel = buildNovel()
        val entity = novel.toEntity()

        assertEquals(novel.id, entity.id)
        assertEquals(novel.sourceId, entity.sourceId)
        assertEquals(novel.url, entity.url)
        assertEquals(novel.title, entity.title)
        assertEquals(novel.author, entity.author)
        assertEquals(novel.coverUrl, entity.coverUrl)
        assertEquals(novel.description, entity.description)
        assertEquals(novel.status.name, entity.status)
        assertEquals(novel.inLibrary, entity.inLibrary)
        assertEquals(novel.lastReadChapterId, entity.lastReadChapterId)
        assertEquals(novel.addedToLibraryAt, entity.addedToLibraryAt)
        assertEquals(novel.chapterCount, entity.chapterCount)
    }

    @Test
    fun `Novel_toEntity serialises genres list to JSON string`() {
        val novel = buildNovel(genres = listOf("Fantasy", "Action"))
        val entity = novel.toEntity()
        assertEquals("""["Fantasy","Action"]""", entity.genres)
    }

    @Test
    fun `Novel_toEntity serialises empty genres to bracket pair`() {
        val entity = buildNovel(genres = emptyList()).toEntity()
        assertEquals("[]", entity.genres)
    }

    @Test
    fun `NovelEntity_toDomain maps all fields correctly`() {
        val entity = NovelEntity(
            id = "com.source.rr::the-wandering-inn",
            sourceId = "com.source.rr",
            url = "https://royalroad.com/fiction/1/the-wandering-inn",
            title = "The Wandering Inn",
            author = "pirateaba",
            coverUrl = "https://example.com/cover.jpg",
            description = "A story about an inn.",
            genres = """["Fantasy","Action"]""",
            status = "ONGOING",
            inLibrary = true,
            lastReadChapterId = "ch-42",
            addedToLibraryAt = 1_700_000_000L,
            chapterCount = 99
        )
        val novel = entity.toDomain()

        assertEquals(entity.id, novel.id)
        assertEquals(entity.sourceId, novel.sourceId)
        assertEquals(entity.url, novel.url)
        assertEquals(entity.title, novel.title)
        assertEquals(entity.author, novel.author)
        assertEquals(entity.coverUrl, novel.coverUrl)
        assertEquals(entity.description, novel.description)
        assertEquals(listOf("Fantasy", "Action"), novel.genres)
        assertEquals(NovelStatus.ONGOING, novel.status)
        assertEquals(entity.inLibrary, novel.inLibrary)
        assertEquals(entity.lastReadChapterId, novel.lastReadChapterId)
        assertEquals(entity.addedToLibraryAt, novel.addedToLibraryAt)
        assertEquals(entity.chapterCount, novel.chapterCount)
    }

    @Test
    fun `Novel round-trip toEntity then toDomain equals original`() {
        val original = buildNovel()
        val roundTripped = original.toEntity().toDomain()
        assertEquals(original, roundTripped)
    }

    @Test
    fun `Novel round-trip preserves all NovelStatus values`() {
        NovelStatus.values().forEach { status ->
            val novel = buildNovel(status = status)
            val roundTripped = novel.toEntity().toDomain()
            assertEquals(status, roundTripped.status)
        }
    }

    @Test
    fun `Novel round-trip with empty genres list`() {
        val original = buildNovel(genres = emptyList())
        val entity = original.toEntity()
        assertEquals("[]", entity.genres)
        val roundTripped = entity.toDomain()
        assertEquals(emptyList<String>(), roundTripped.genres)
        assertEquals(original, roundTripped)
    }

    @Test
    fun `Novel round-trip with genres containing double-quote characters`() {
        val genresWithQuotes = listOf("""Sci"Fi""", """Isekai "World" Genre""")
        val original = buildNovel(genres = genresWithQuotes)
        val roundTripped = original.toEntity().toDomain()
        assertEquals(genresWithQuotes, roundTripped.genres)
    }

    @Test
    fun `Novel round-trip with null optional fields`() {
        val original = buildNovel(lastReadChapterId = null, addedToLibraryAt = null)
        val roundTripped = original.toEntity().toDomain()
        assertNull(roundTripped.lastReadChapterId)
        assertNull(roundTripped.addedToLibraryAt)
        assertEquals(original, roundTripped)
    }

    @Test
    fun `Novel round-trip with inLibrary false`() {
        val original = buildNovel(inLibrary = false)
        assertEquals(false, original.toEntity().toDomain().inLibrary)
    }

    @Test
    fun `NovelEntity_toDomain parses blank genres string as empty list`() {
        val entity = NovelEntity(
            id = "x", sourceId = "s", url = "u", title = "t", author = "a",
            coverUrl = "", description = "", genres = "   ", status = "UNKNOWN"
        )
        assertEquals(emptyList<String>(), entity.toDomain().genres)
    }

    @Test
    fun `NovelEntity_toDomain parses bracket-pair genres string as empty list`() {
        val entity = NovelEntity(
            id = "x", sourceId = "s", url = "u", title = "t", author = "a",
            coverUrl = "", description = "", genres = "[]", status = "COMPLETED"
        )
        assertEquals(emptyList<String>(), entity.toDomain().genres)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Chapter
    // ═══════════════════════════════════════════════════════════════════════════

    private fun buildChapter(
        downloadState: DownloadState = DownloadState.NOT_DOWNLOADED,
        isRead: Boolean = false,
        uploadedAt: Long? = 1_710_000_000L,
        localFilePath: String? = null
    ) = Chapter(
        id = "com.source.rr::the-wandering-inn::ch-1",
        novelId = "com.source.rr::the-wandering-inn",
        sourceId = "com.source.rr",
        url = "https://royalroad.com/fiction/1/chapter/1",
        title = "First Steps",
        chapterNumber = 1.0f,
        uploadedAt = uploadedAt,
        isRead = isRead,
        downloadState = downloadState,
        localFilePath = localFilePath
    )

    @Test
    fun `Chapter_toEntity maps all fields correctly`() {
        val chapter = buildChapter(
            downloadState = DownloadState.DOWNLOADED,
            isRead = true,
            localFilePath = "/data/chapters/ch-1.txt"
        )
        val entity = chapter.toEntity()

        assertEquals(chapter.id, entity.id)
        assertEquals(chapter.novelId, entity.novelId)
        assertEquals(chapter.sourceId, entity.sourceId)
        assertEquals(chapter.url, entity.url)
        assertEquals(chapter.title, entity.title)
        assertEquals(chapter.chapterNumber, entity.chapterNumber)
        assertEquals(chapter.uploadedAt, entity.uploadedAt)
        assertEquals(chapter.isRead, entity.isRead)
        assertEquals(chapter.localFilePath, entity.localFilePath)
    }

    @Test
    fun `Chapter_toEntity stores downloadState as enum name string`() {
        DownloadState.values().forEach { state ->
            val entity = buildChapter(downloadState = state).toEntity()
            assertEquals(state.name, entity.downloadState)
        }
    }

    @Test
    fun `ChapterEntity_toDomain parses downloadState from string`() {
        DownloadState.values().forEach { state ->
            val entity = ChapterEntity(
                id = "id", novelId = "nid", sourceId = "sid",
                url = "url", title = "title", chapterNumber = 1f,
                uploadedAt = null, downloadState = state.name
            )
            assertEquals(state, entity.toDomain().downloadState)
        }
    }

    @Test
    fun `Chapter round-trip toEntity then toDomain equals original`() {
        val original = buildChapter(
            downloadState = DownloadState.DOWNLOADED,
            isRead = true,
            localFilePath = "/data/chapters/ch-1.txt"
        )
        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun `Chapter round-trip for all DownloadState values`() {
        DownloadState.values().forEach { state ->
            val original = buildChapter(downloadState = state)
            val roundTripped = original.toEntity().toDomain()
            assertEquals(state, roundTripped.downloadState)
            assertEquals(original, roundTripped)
        }
    }

    @Test
    fun `Chapter round-trip with null uploadedAt`() {
        val original = buildChapter(uploadedAt = null)
        val roundTripped = original.toEntity().toDomain()
        assertNull(roundTripped.uploadedAt)
        assertEquals(original, roundTripped)
    }

    @Test
    fun `Chapter round-trip with null localFilePath`() {
        val original = buildChapter(localFilePath = null)
        assertNull(original.toEntity().toDomain().localFilePath)
    }

    @Test
    fun `Chapter toEntity uses NOT_DOWNLOADED as default downloadState string`() {
        val entity = ChapterEntity(
            id = "id", novelId = "nid", sourceId = "sid",
            url = "url", title = "title", chapterNumber = 1f, uploadedAt = null
            // downloadState defaults to "NOT_DOWNLOADED" per entity declaration
        )
        assertEquals(DownloadState.NOT_DOWNLOADED, entity.toDomain().downloadState)
    }

    @Test
    fun `Chapter supports fractional chapterNumber`() {
        val original = buildChapter().copy(chapterNumber = 12.5f)
        val roundTripped = original.toEntity().toDomain()
        assertEquals(12.5f, roundTripped.chapterNumber)
        assertEquals(original, roundTripped)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NovelReaderSettings
    // ═══════════════════════════════════════════════════════════════════════════

    private fun buildSettings(
        novelId: String = "com.source.rr::the-wandering-inn",
        readerModeEnabled: Boolean = true,
        fontSize: Float = 18f,
        ttsVoiceId: String? = "voice-en-US-1",
        ttsEngineId: String = "android"
    ) = NovelReaderSettings(
        novelId = novelId,
        readerModeEnabled = readerModeEnabled,
        fontSize = fontSize,
        ttsVoiceId = ttsVoiceId,
        ttsEngineId = ttsEngineId
    )

    @Test
    fun `NovelReaderSettings_toEntity maps all fields correctly`() {
        val settings = buildSettings()
        val entity = settings.toEntity()

        assertEquals(settings.novelId, entity.novelId)
        assertEquals(settings.readerModeEnabled, entity.readerModeEnabled)
        assertEquals(settings.fontSize, entity.fontSize)
        assertEquals(settings.ttsVoiceId, entity.ttsVoiceId)
        assertEquals(settings.ttsEngineId, entity.ttsEngineId)
    }

    @Test
    fun `NovelReaderSettingsEntity_toDomain maps all fields correctly`() {
        val entity = NovelReaderSettingsEntity(
            novelId = "com.source.rr::the-wandering-inn",
            readerModeEnabled = false,
            fontSize = 20f,
            ttsVoiceId = "voice-en-GB-2",
            ttsEngineId = "google"
        )
        val settings = entity.toDomain()

        assertEquals(entity.novelId, settings.novelId)
        assertEquals(entity.readerModeEnabled, settings.readerModeEnabled)
        assertEquals(entity.fontSize, settings.fontSize)
        assertEquals(entity.ttsVoiceId, settings.ttsVoiceId)
        assertEquals(entity.ttsEngineId, settings.ttsEngineId)
    }

    @Test
    fun `NovelReaderSettings round-trip toEntity then toDomain equals original`() {
        val original = buildSettings()
        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun `NovelReaderSettings round-trip with readerModeEnabled false`() {
        val original = buildSettings(readerModeEnabled = false)
        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun `NovelReaderSettings round-trip with null ttsVoiceId`() {
        val original = buildSettings(ttsVoiceId = null)
        val roundTripped = original.toEntity().toDomain()
        assertNull(roundTripped.ttsVoiceId)
        assertEquals(original, roundTripped)
    }

    @Test
    fun `NovelReaderSettings round-trip with custom ttsEngineId`() {
        val original = buildSettings(ttsEngineId = "google")
        assertEquals("google", original.toEntity().toDomain().ttsEngineId)
    }

    @Test
    fun `NovelReaderSettings round-trip with non-default fontSize`() {
        val original = buildSettings(fontSize = 24f)
        assertEquals(24f, original.toEntity().toDomain().fontSize)
    }

    @Test
    fun `NovelReaderSettingsEntity default values match domain model defaults`() {
        val entity = NovelReaderSettingsEntity(novelId = "n")
        val settings = entity.toDomain()
        assertTrue(settings.readerModeEnabled)
        assertEquals(16f, settings.fontSize)
        assertNull(settings.ttsVoiceId)
        assertEquals("android", settings.ttsEngineId)
    }
}
