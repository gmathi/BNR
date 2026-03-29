package com.bnr.app.domain.usecase

import app.cash.turbine.test
import com.bnr.app.domain.repository.DownloadRepository
import com.bnr.app.domain.repository.ReaderSettingsRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DownloadAndReaderSettingsUseCaseTest {

    private lateinit var downloadRepository: DownloadRepository
    private lateinit var readerSettingsRepository: ReaderSettingsRepository

    private lateinit var downloadChapterUseCase: DownloadChapterUseCase
    private lateinit var getReaderSettingsUseCase: GetReaderSettingsUseCase
    private lateinit var updateReaderSettingsUseCase: UpdateReaderSettingsUseCase

    @Before
    fun setUp() {
        downloadRepository = mockk()
        readerSettingsRepository = mockk()

        downloadChapterUseCase = DownloadChapterUseCase(downloadRepository)
        getReaderSettingsUseCase = GetReaderSettingsUseCase(readerSettingsRepository)
        updateReaderSettingsUseCase = UpdateReaderSettingsUseCase(readerSettingsRepository)
    }

    // =========================================================================
    // DownloadChapterUseCase
    // =========================================================================

    @Test
    fun `DownloadChapter calls enqueueDownload with correct chapterId`() = runTest {
        val chapter = makeChapter(id = "novel::5")
        coJustRun {
            downloadRepository.enqueueDownload(
                chapterId = chapter.id,
                chapterUrl = chapter.url,
                novelId = chapter.novelId,
                sourceId = chapter.sourceId
            )
        }

        downloadChapterUseCase(chapter)

        coVerify(exactly = 1) {
            downloadRepository.enqueueDownload(
                chapterId = chapter.id,
                chapterUrl = chapter.url,
                novelId = chapter.novelId,
                sourceId = chapter.sourceId
            )
        }
    }

    @Test
    fun `DownloadChapter passes correct chapterUrl to enqueueDownload`() = runTest {
        val chapter = makeChapter(url = "/novel/slug/chapter-7")
        coJustRun { downloadRepository.enqueueDownload(any(), any(), any(), any()) }

        downloadChapterUseCase(chapter)

        coVerify(exactly = 1) {
            downloadRepository.enqueueDownload(
                chapterId = any(),
                chapterUrl = "/novel/slug/chapter-7",
                novelId = any(),
                sourceId = any()
            )
        }
    }

    @Test
    fun `DownloadChapter passes correct novelId to enqueueDownload`() = runTest {
        val chapter = makeChapter(novelId = "com.source.test::my-novel")
        coJustRun { downloadRepository.enqueueDownload(any(), any(), any(), any()) }

        downloadChapterUseCase(chapter)

        coVerify(exactly = 1) {
            downloadRepository.enqueueDownload(
                chapterId = any(),
                chapterUrl = any(),
                novelId = "com.source.test::my-novel",
                sourceId = any()
            )
        }
    }

    @Test
    fun `DownloadChapter passes correct sourceId to enqueueDownload`() = runTest {
        val chapter = makeChapter(sourceId = "com.custom.source")
        coJustRun { downloadRepository.enqueueDownload(any(), any(), any(), any()) }

        downloadChapterUseCase(chapter)

        coVerify(exactly = 1) {
            downloadRepository.enqueueDownload(
                chapterId = any(),
                chapterUrl = any(),
                novelId = any(),
                sourceId = "com.custom.source"
            )
        }
    }

    @Test
    fun `DownloadChapter maps all four Chapter fields correctly in a single call`() = runTest {
        val chapter = makeChapter(
            id = "novel::12",
            novelId = "src::novel",
            sourceId = "com.source.test",
            url = "/novel/slug/chapter-12"
        )
        coJustRun { downloadRepository.enqueueDownload(any(), any(), any(), any()) }

        downloadChapterUseCase(chapter)

        coVerify(exactly = 1) {
            downloadRepository.enqueueDownload(
                chapterId = "novel::12",
                chapterUrl = "/novel/slug/chapter-12",
                novelId = "src::novel",
                sourceId = "com.source.test"
            )
        }
    }

    // =========================================================================
    // GetReaderSettingsUseCase
    // =========================================================================

    @Test
    fun `GetReaderSettings returns flow emitting settings from repository`() =
        runTest(UnconfinedTestDispatcher()) {
            val settings = makeReaderSettings(novelId = "src::slug")
            every { readerSettingsRepository.getReaderSettings("src::slug") } returns
                flowOf(settings)

            getReaderSettingsUseCase("src::slug").test {
                assertEquals(settings, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `GetReaderSettings passes the novelId to repository correctly`() =
        runTest(UnconfinedTestDispatcher()) {
            val novelId = "com.source.test::another-novel"
            val settings = makeReaderSettings(novelId = novelId)
            every { readerSettingsRepository.getReaderSettings(novelId) } returns flowOf(settings)

            getReaderSettingsUseCase(novelId).test {
                awaitItem()
                awaitComplete()
            }

            // Verify the repository was called with exactly the right novelId
            io.mockk.verify(exactly = 1) {
                readerSettingsRepository.getReaderSettings(novelId)
            }
        }

    @Test
    fun `GetReaderSettings emits default settings when novel is not in DB`() =
        runTest(UnconfinedTestDispatcher()) {
            val novelId = "src::new-novel"
            // Repository emits a default-constructed settings object (no custom overrides)
            val defaultSettings = makeReaderSettings(novelId = novelId)
            every { readerSettingsRepository.getReaderSettings(novelId) } returns
                flowOf(defaultSettings)

            getReaderSettingsUseCase(novelId).test {
                val emitted = awaitItem()
                assertEquals(novelId, emitted.novelId)
                assertEquals(true, emitted.readerModeEnabled)
                assertEquals(16f, emitted.fontSize)
                assertEquals(null, emitted.ttsVoiceId)
                assertEquals("android", emitted.ttsEngineId)
                awaitComplete()
            }
        }

    @Test
    fun `GetReaderSettings emits multiple updates when repository flow emits multiple values`() =
        runTest(UnconfinedTestDispatcher()) {
            val novelId = "src::slug"
            val initial = makeReaderSettings(novelId = novelId, fontSize = 16f)
            val updated = makeReaderSettings(novelId = novelId, fontSize = 20f)
            every { readerSettingsRepository.getReaderSettings(novelId) } returns
                kotlinx.coroutines.flow.flow {
                    emit(initial)
                    emit(updated)
                }

            getReaderSettingsUseCase(novelId).test {
                assertEquals(16f, awaitItem().fontSize)
                assertEquals(20f, awaitItem().fontSize)
                awaitComplete()
            }
        }

    // =========================================================================
    // UpdateReaderSettingsUseCase
    // =========================================================================

    @Test
    fun `UpdateReaderSettings calls repository upsertReaderSettings with the given settings`() =
        runTest {
            val settings = makeReaderSettings(novelId = "src::slug", fontSize = 18f)
            coJustRun { readerSettingsRepository.upsertReaderSettings(settings) }

            updateReaderSettingsUseCase(settings)

            coVerify(exactly = 1) { readerSettingsRepository.upsertReaderSettings(settings) }
        }

    @Test
    fun `UpdateReaderSettings passes the exact settings object to repository`() = runTest {
        val settings = makeReaderSettings(
            novelId = "src::slug",
            readerModeEnabled = false,
            fontSize = 22f,
            ttsVoiceId = "voice-en-us",
            ttsEngineId = "google"
        )
        coJustRun { readerSettingsRepository.upsertReaderSettings(any()) }

        updateReaderSettingsUseCase(settings)

        coVerify(exactly = 1) {
            readerSettingsRepository.upsertReaderSettings(
                withArg { captured ->
                    assertEquals("src::slug", captured.novelId)
                    assertEquals(false, captured.readerModeEnabled)
                    assertEquals(22f, captured.fontSize)
                    assertEquals("voice-en-us", captured.ttsVoiceId)
                    assertEquals("google", captured.ttsEngineId)
                }
            )
        }
    }

    @Test
    fun `UpdateReaderSettings does not call getReaderSettings`() = runTest {
        val settings = makeReaderSettings()
        coJustRun { readerSettingsRepository.upsertReaderSettings(any()) }

        updateReaderSettingsUseCase(settings)

        io.mockk.verify(exactly = 0) { readerSettingsRepository.getReaderSettings(any()) }
    }
}
