package com.bnr.app.core.backup

import com.google.gson.Gson
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.data.local.db.dao.ChapterDao
import com.bnr.app.data.local.db.dao.NovelDao
import com.bnr.app.data.local.db.dao.ReaderSettingsDao
import com.bnr.app.data.local.db.entity.ChapterEntity
import com.bnr.app.data.local.db.entity.NovelEntity
import com.bnr.app.data.local.db.entity.NovelReaderSettingsEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DriveBackupManagerTest {

    private val novelDao: NovelDao = mockk(relaxed = true)
    private val chapterDao: ChapterDao = mockk(relaxed = true)
    private val readerSettingsDao: ReaderSettingsDao = mockk(relaxed = true)
    private val appPreferences: AppPreferences = mockk(relaxed = true)
    private val okHttpClient: OkHttpClient = mockk()
    private val gson = Gson()
    private val context: android.content.Context = mockk(relaxed = true)

    private lateinit var manager: DriveBackupManager

    private val testNovel = NovelEntity(
        id          = "src::novel1",
        sourceId    = "src",
        url         = "https://example.com/novel1",
        title       = "Test Novel",
        author      = "Author",
        coverUrl    = "",
        description = "Desc",
        genres      = "[]",
        status      = "ONGOING",
        inLibrary   = true
    )

    private val testChapter = ChapterEntity(
        id            = "ch1",
        novelId       = "src::novel1",
        sourceId      = "src",
        url           = "https://example.com/ch1",
        title         = "Chapter 1",
        chapterNumber = 1f,
        uploadedAt    = null
    )

    private val testSettings = NovelReaderSettingsEntity(novelId = "src::novel1")

    @Before
    fun setUp() {
        every { appPreferences.preferredSourceId } returns flowOf("src")
        every { appPreferences.readerFontSize }     returns flowOf(16f)
        every { appPreferences.readerTheme }        returns flowOf("system")
        every { appPreferences.updateIntervalHours } returns flowOf(6)

        manager = DriveBackupManager(
            context            = context,
            novelDao           = novelDao,
            chapterDao         = chapterDao,
            readerSettingsDao  = readerSettingsDao,
            appPreferences     = appPreferences,
            okHttpClient       = okHttpClient,
            gson               = gson
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun mockHttpCall(responseCode: Int, responseBody: String): Call {
        val call = mockk<Call>()
        val request = Request.Builder().url("https://www.googleapis.com/").build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(responseCode)
            .message("OK")
            .body(responseBody.toResponseBody("application/json".toMediaType()))
            .build()
        every { call.execute() } returns response
        every { okHttpClient.newCall(any()) } returns call
        return call
    }

    // ── backup tests ──────────────────────────────────────────────────────────

    @Test
    fun `backup collects library novels from dao`() = runTest {
        every { novelDao.getLibraryNovels() } returns flowOf(listOf(testNovel))
        every { chapterDao.getChaptersByNovelId(testNovel.id) } returns flowOf(listOf(testChapter))
        every { readerSettingsDao.getSettingsByNovelId(testNovel.id) } returns flowOf(testSettings)
        mockHttpCall(200, """{"id":"file123"}""")

        manager.backup("token", "user@example.com")

        coVerify { novelDao.getLibraryNovels() }
    }

    @Test
    fun `backup serializes to json and uploads`() = runTest {
        every { novelDao.getLibraryNovels() } returns flowOf(listOf(testNovel))
        every { chapterDao.getChaptersByNovelId(testNovel.id) } returns flowOf(listOf(testChapter))
        every { readerSettingsDao.getSettingsByNovelId(testNovel.id) } returns flowOf(testSettings)

        val requestSlot = slot<Request>()
        val call = mockk<Call>()
        val request = Request.Builder().url("https://www.googleapis.com/").build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""{"id":"file123"}""".toResponseBody("application/json".toMediaType()))
            .build()
        every { call.execute() } returns response
        every { okHttpClient.newCall(capture(requestSlot)) } returns call

        manager.backup("my_token", "user@example.com")

        val capturedRequest = requestSlot.captured
        assertTrue(capturedRequest.header("Authorization") == "Bearer my_token")
        assertTrue(capturedRequest.url.toString().contains("uploadType=multipart"))
    }

    @Test
    fun `backup saves last backup time to preferences`() = runTest {
        every { novelDao.getLibraryNovels() } returns flowOf(listOf(testNovel))
        every { chapterDao.getChaptersByNovelId(testNovel.id) } returns flowOf(emptyList())
        every { readerSettingsDao.getSettingsByNovelId(testNovel.id) } returns flowOf(testSettings)
        mockHttpCall(200, """{"id":"file123"}""")

        manager.backup("token", "user@example.com")

        coVerify { appPreferences.setLastBackupTimeMs(any()) }
    }

    @Test
    fun `backup returns BackupInfo with correct novelCount`() = runTest {
        val novels = listOf(testNovel, testNovel.copy(id = "src::novel2"))
        every { novelDao.getLibraryNovels() } returns flowOf(novels)
        every { chapterDao.getChaptersByNovelId(any()) } returns flowOf(emptyList())
        every { readerSettingsDao.getSettingsByNovelId(any()) } returns flowOf(null)
        mockHttpCall(200, """{"id":"file456"}""")

        val result = manager.backup("token", "user@example.com")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().novelCount)
    }

    @Test
    fun `backup returns failure when Drive upload fails`() = runTest {
        every { novelDao.getLibraryNovels() } returns flowOf(listOf(testNovel))
        every { chapterDao.getChaptersByNovelId(any()) } returns flowOf(emptyList())
        every { readerSettingsDao.getSettingsByNovelId(any()) } returns flowOf(null)
        mockHttpCall(403, """{"error":"Forbidden"}""")

        val result = manager.backup("bad_token", "user@example.com")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Drive upload failed") == true)
    }

    // ── restore tests ─────────────────────────────────────────────────────────

    @Test
    fun `restore downloads and parses json`() = runTest {
        val backupData = BackupData(
            exportedAt     = 1_000L,
            novels         = listOf(testNovel),
            chapters       = listOf(testChapter),
            readerSettings = listOf(testSettings),
            preferences    = emptyMap()
        )
        val backupJson = gson.toJson(backupData)
        val listJson   = """{"files":[{"id":"file123","modifiedTime":"2024-01-01T00:00:00Z"}]}"""

        var callCount = 0
        every { okHttpClient.newCall(any()) } answers {
            val dummyRequest = Request.Builder().url("https://www.googleapis.com/").build()
            val body = if (callCount++ == 0) listJson else backupJson
            val response = Response.Builder()
                .request(dummyRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
            val call = mockk<Call>()
            every { call.execute() } returns response
            call
        }

        val result = manager.restore("token")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
    }

    @Test
    fun `restore calls upsertNovel for each novel with inLibrary=true`() = runTest {
        val backupData = BackupData(
            exportedAt     = 1_000L,
            novels         = listOf(testNovel.copy(inLibrary = false)),
            chapters       = emptyList(),
            readerSettings = emptyList(),
            preferences    = emptyMap()
        )
        val backupJson = gson.toJson(backupData)
        val listJson   = """{"files":[{"id":"file123","modifiedTime":"2024-01-01T00:00:00Z"}]}"""

        var callCount = 0
        every { okHttpClient.newCall(any()) } answers {
            val dummyRequest = Request.Builder().url("https://www.googleapis.com/").build()
            val body = if (callCount++ == 0) listJson else backupJson
            val response = Response.Builder()
                .request(dummyRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
            val call = mockk<Call>()
            every { call.execute() } returns response
            call
        }

        manager.restore("token")

        coVerify { novelDao.upsertNovel(match { it.inLibrary }) }
    }

    @Test
    fun `restore returns failure when no backup found`() = runTest {
        val emptyListJson = """{"files":[]}"""
        mockHttpCall(200, emptyListJson)

        val result = manager.restore("token")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("No backup found") == true)
    }

    @Test
    fun `findBackupFileId returns null when files array is empty`() = runTest {
        mockHttpCall(200, """{"files":[]}""")

        // Trigger restore which calls findBackupFileId internally
        val result = manager.restore("token")

        assertTrue(result.isFailure)
    }
}
