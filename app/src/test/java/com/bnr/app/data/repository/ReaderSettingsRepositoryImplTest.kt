package com.bnr.app.data.repository

import app.cash.turbine.test
import com.bnr.app.data.local.db.dao.ReaderSettingsDao
import com.bnr.app.data.local.db.entity.NovelReaderSettingsEntity
import com.bnr.app.domain.model.NovelReaderSettings
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReaderSettingsRepositoryImplTest {

    private val readerSettingsDao: ReaderSettingsDao = mockk(relaxUnitFun = true)

    private lateinit var repository: ReaderSettingsRepositoryImpl

    // ── shared fixtures ───────────────────────────────────────────────────────

    private val novelId = "src::novel-slug"

    private val settingsEntity = NovelReaderSettingsEntity(
        novelId = novelId,
        readerModeEnabled = false,
        fontSize = 18f,
        ttsVoiceId = "voice-en-us",
        ttsEngineId = "com.google.tts"
    )

    @Before
    fun setUp() {
        repository = ReaderSettingsRepositoryImpl(readerSettingsDao)
    }

    // ── getReaderSettings ─────────────────────────────────────────────────────

    @Test
    fun `getReaderSettings emits mapped domain settings when dao returns entity`() =
        runTest(UnconfinedTestDispatcher()) {
            every { readerSettingsDao.getSettingsByNovelId(novelId) } returns flowOf(settingsEntity)

            repository.getReaderSettings(novelId).test {
                val settings = awaitItem()
                assertEquals(novelId, settings.novelId)
                assertEquals(false, settings.readerModeEnabled)
                assertEquals(18f, settings.fontSize)
                assertEquals("voice-en-us", settings.ttsVoiceId)
                assertEquals("com.google.tts", settings.ttsEngineId)
                awaitComplete()
            }
        }

    @Test
    fun `getReaderSettings emits default NovelReaderSettings when dao returns null`() =
        runTest(UnconfinedTestDispatcher()) {
            every { readerSettingsDao.getSettingsByNovelId(novelId) } returns flowOf(null)

            repository.getReaderSettings(novelId).test {
                val settings = awaitItem()
                assertEquals(novelId, settings.novelId)
                // Verify all defaults match those declared in NovelReaderSettings
                assertTrue(settings.readerModeEnabled)
                assertEquals(16f, settings.fontSize)
                assertNull(settings.ttsVoiceId)
                assertEquals("android", settings.ttsEngineId)
                awaitComplete()
            }
        }

    @Test
    fun `getReaderSettings emits default settings equal to NovelReaderSettings(novelId) constructor`() =
        runTest(UnconfinedTestDispatcher()) {
            every { readerSettingsDao.getSettingsByNovelId(novelId) } returns flowOf(null)

            repository.getReaderSettings(novelId).test {
                val settings = awaitItem()
                assertEquals(NovelReaderSettings(novelId = novelId), settings)
                awaitComplete()
            }
        }

    @Test
    fun `getReaderSettings emits each update from the dao Flow`() =
        runTest(UnconfinedTestDispatcher()) {
            val updatedEntity = settingsEntity.copy(fontSize = 22f)
            every { readerSettingsDao.getSettingsByNovelId(novelId) } returns
                flowOf(settingsEntity, updatedEntity)

            repository.getReaderSettings(novelId).test {
                val first = awaitItem()
                assertEquals(18f, first.fontSize)

                val second = awaitItem()
                assertEquals(22f, second.fontSize)

                awaitComplete()
            }
        }

    // ── upsertReaderSettings ──────────────────────────────────────────────────

    @Test
    fun `upsertReaderSettings calls dao upsertSettings with correct entity`() =
        runTest(UnconfinedTestDispatcher()) {
            val domainSettings = NovelReaderSettings(
                novelId = novelId,
                readerModeEnabled = false,
                fontSize = 18f,
                ttsVoiceId = "voice-en-us",
                ttsEngineId = "com.google.tts"
            )

            repository.upsertReaderSettings(domainSettings)

            coVerify(exactly = 1) {
                readerSettingsDao.upsertSettings(
                    match { entity ->
                        entity.novelId == novelId &&
                            entity.readerModeEnabled == false &&
                            entity.fontSize == 18f &&
                            entity.ttsVoiceId == "voice-en-us" &&
                            entity.ttsEngineId == "com.google.tts"
                    }
                )
            }
        }

    @Test
    fun `upsertReaderSettings maps default settings to entity correctly`() =
        runTest(UnconfinedTestDispatcher()) {
            val domainSettings = NovelReaderSettings(novelId = novelId)

            repository.upsertReaderSettings(domainSettings)

            coVerify(exactly = 1) {
                readerSettingsDao.upsertSettings(
                    match { entity ->
                        entity.novelId == novelId &&
                            entity.readerModeEnabled == true &&
                            entity.fontSize == 16f &&
                            entity.ttsVoiceId == null &&
                            entity.ttsEngineId == "android"
                    }
                )
            }
        }

    @Test
    fun `upsertReaderSettings with null ttsVoiceId passes null to entity`() =
        runTest(UnconfinedTestDispatcher()) {
            val domainSettings = NovelReaderSettings(
                novelId = novelId,
                ttsVoiceId = null
            )

            repository.upsertReaderSettings(domainSettings)

            coVerify(exactly = 1) {
                readerSettingsDao.upsertSettings(
                    match { entity -> entity.ttsVoiceId == null }
                )
            }
        }
}
