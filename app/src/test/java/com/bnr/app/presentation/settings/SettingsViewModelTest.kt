package com.bnr.app.presentation.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.bnr.app.core.backup.BackupInfo
import com.bnr.app.core.backup.DriveBackupManager
import com.bnr.app.core.datastore.AppPreferences
import com.bnr.app.data.local.saf.SafStorageManager
import com.bnr.app.domain.model.TtsVoice
import com.bnr.app.presentation.MainDispatcherRule
import com.bnr.app.source.SourceManager
import com.bnr.app.tts.TtsManager
import com.bnr.app.worker.UpdateScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val instantTask = InstantTaskExecutorRule()

    private val appPreferences: AppPreferences = mockk(relaxed = true)
    private val safStorageManager: SafStorageManager = mockk(relaxed = true)
    private val sourceManager: SourceManager = mockk(relaxed = true)
    private val ttsManager: TtsManager = mockk(relaxed = true)
    private val updateScheduler: UpdateScheduler = mockk(relaxed = true)
    private val driveBackupManager: DriveBackupManager = mockk(relaxed = true)

    private fun createViewModel(): SettingsViewModel {
        every { sourceManager.getAllSources() } returns emptyList()
        every { appPreferences.downloadFolderUri }        returns flowOf(null)
        every { appPreferences.preferredSourceId }        returns flowOf(null)
        every { appPreferences.defaultTtsVoiceId }        returns flowOf(null)
        every { appPreferences.backgroundUpdatesEnabled } returns flowOf(true)
        every { appPreferences.updateIntervalHours }      returns flowOf(6)
        every { appPreferences.driveBackupEnabled }       returns flowOf(false)
        every { appPreferences.lastBackupTimeMs }         returns flowOf(null)
        every { appPreferences.driveAccountEmail }        returns flowOf(null)
        every { appPreferences.appTheme }                 returns flowOf("system")
        coEvery { ttsManager.getAvailableVoices() }       returns emptyList()

        return SettingsViewModel(
            appPreferences     = appPreferences,
            safStorageManager  = safStorageManager,
            sourceManager      = sourceManager,
            ttsManager         = ttsManager,
            updateScheduler    = updateScheduler,
            driveBackupManager = driveBackupManager
        )
    }

    // ── Background updates ────────────────────────────────────────────────────

    @Test
    fun `backgroundUpdatesEnabled initial value from preferences`() =
        runTest(UnconfinedTestDispatcher()) {
            every { appPreferences.backgroundUpdatesEnabled } returns flowOf(false)

            val vm = createViewModel()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.backgroundUpdatesEnabled)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onBackgroundUpdatesToggled true calls updateScheduler schedule`() =
        runTest(UnconfinedTestDispatcher()) {
            every { appPreferences.updateIntervalHours } returns flowOf(6)

            val vm = createViewModel()
            vm.onBackgroundUpdatesToggled(true)

            coVerify { appPreferences.setBackgroundUpdatesEnabled(true) }
            verify { updateScheduler.schedule(6) }
        }

    @Test
    fun `onBackgroundUpdatesToggled false calls updateScheduler cancel`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            vm.onBackgroundUpdatesToggled(false)

            coVerify { appPreferences.setBackgroundUpdatesEnabled(false) }
            verify { updateScheduler.cancel() }
        }

    @Test
    fun `onUpdateIntervalChanged saves to preferences and reschedules when enabled`() =
        runTest(UnconfinedTestDispatcher()) {
            every { appPreferences.backgroundUpdatesEnabled } returns flowOf(true)

            val vm = createViewModel()
            vm.onUpdateIntervalChanged(12)

            coVerify { appPreferences.setUpdateIntervalHours(12) }
            verify { updateScheduler.schedule(12) }
        }

    @Test
    fun `onUpdateIntervalChanged saves to preferences but does not reschedule when disabled`() =
        runTest(UnconfinedTestDispatcher()) {
            every { appPreferences.backgroundUpdatesEnabled } returns flowOf(false)

            val vm = createViewModel()
            vm.onUpdateIntervalChanged(12)

            coVerify { appPreferences.setUpdateIntervalHours(12) }
            verify(exactly = 0) { updateScheduler.schedule(any()) }
        }

    // ── Drive backup ──────────────────────────────────────────────────────────

    @Test
    fun `driveBackupEnabled initial value from preferences`() =
        runTest(UnconfinedTestDispatcher()) {
            every { appPreferences.driveBackupEnabled } returns flowOf(true)

            val vm = createViewModel()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.driveBackupEnabled)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onBackupNow sets isBackingUp true then false after completion`() =
        runTest(UnconfinedTestDispatcher()) {
            val fakeInfo = BackupInfo(
                fileId       = "file123",
                timestamp    = 1_000L,
                novelCount   = 1,
                accountEmail = "user@example.com"
            )
            coEvery { driveBackupManager.backup(any(), any()) } returns Result.success(fakeInfo)

            val vm = createViewModel()

            vm.uiState.test {
                // consume initial state
                awaitItem()

                vm.onBackupNow("token", "user@example.com")

                // After completion (UnconfinedTestDispatcher runs coroutines eagerly)
                val finalState = awaitItem()
                assertFalse(finalState.isBackingUp)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onBackupNow updates lastBackupTime on success`() =
        runTest(UnconfinedTestDispatcher()) {
            val fakeTimestamp = 1_700_000_000_000L
            val fakeInfo = BackupInfo(
                fileId       = "file123",
                timestamp    = fakeTimestamp,
                novelCount   = 3,
                accountEmail = "user@example.com"
            )
            coEvery { driveBackupManager.backup(any(), any()) } returns Result.success(fakeInfo)

            val vm = createViewModel()

            vm.uiState.test {
                awaitItem() // initial

                vm.onBackupNow("token", "user@example.com")

                val updated = awaitItem()
                assertEquals(fakeTimestamp, updated.lastBackupTime)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onRestoreFromDrive sets isRestoring true then false`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { driveBackupManager.restore(any()) } returns Result.success(5)

            val vm = createViewModel()

            vm.uiState.test {
                awaitItem() // initial

                vm.onRestoreFromDrive("token")

                val finalState = awaitItem()
                assertFalse(finalState.isRestoring)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onBackupNow sets backupError on failure`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { driveBackupManager.backup(any(), any()) } returns
                Result.failure(RuntimeException("Upload failed"))

            val vm = createViewModel()

            vm.uiState.test {
                awaitItem() // initial

                vm.onBackupNow("bad_token", "user@example.com")

                val errorState = awaitItem()
                assertFalse(errorState.isBackingUp)
                assertNotNull(errorState.backupError)
                assertTrue(errorState.backupError!!.contains("Upload failed"))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onRestoreFromDrive sets backupError on failure`() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { driveBackupManager.restore(any()) } returns
                Result.failure(RuntimeException("No backup found"))

            val vm = createViewModel()

            vm.uiState.test {
                awaitItem() // initial

                vm.onRestoreFromDrive("token")

                val errorState = awaitItem()
                assertFalse(errorState.isRestoring)
                assertNotNull(errorState.backupError)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── App theme ─────────────────────────────────────────────────────────────

    @Test
    fun `appTheme initial value is system`() =
        runTest(UnconfinedTestDispatcher()) {
            every { appPreferences.appTheme } returns flowOf("system")

            val vm = createViewModel()

            vm.uiState.test {
                val state = awaitItem()
                assertEquals("system", state.appTheme)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onAppThemeChanged saves to preferences`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            vm.onAppThemeChanged("dark")

            coVerify { appPreferences.setAppTheme("dark") }
        }
}
