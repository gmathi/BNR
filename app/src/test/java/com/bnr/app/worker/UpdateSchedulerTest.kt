package com.bnr.app.worker

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UpdateSchedulerTest {

    private val workManager: WorkManager = mockk(relaxed = true)
    private lateinit var scheduler: UpdateScheduler

    @Before
    fun setUp() {
        scheduler = UpdateScheduler(workManager)
    }

    // ── schedule enqueues unique periodic work with correct name ──────────────

    @Test
    fun `schedule enqueues unique periodic work with correct name`() {
        scheduler.schedule(6)

        verify {
            workManager.enqueueUniquePeriodicWork(
                UpdateScheduler.WORK_NAME,
                any(),
                any()
            )
        }
    }

    // ── schedule uses ExistingPeriodicWorkPolicy.UPDATE ───────────────────────

    @Test
    fun `schedule uses ExistingPeriodicWorkPolicy UPDATE`() {
        val policySlot = slot<ExistingPeriodicWorkPolicy>()

        every {
            workManager.enqueueUniquePeriodicWork(
                any(),
                capture(policySlot),
                any()
            )
        } returns mockk(relaxed = true)

        scheduler.schedule(6)

        assertEquals(ExistingPeriodicWorkPolicy.UPDATE, policySlot.captured)
    }

    // ── schedule sets CONNECTED network constraint ────────────────────────────

    @Test
    fun `schedule sets CONNECTED network constraint`() {
        val requestSlot = slot<PeriodicWorkRequest>()

        every {
            workManager.enqueueUniquePeriodicWork(
                any(),
                any(),
                capture(requestSlot)
            )
        } returns mockk(relaxed = true)

        scheduler.schedule(6)

        val constraints: Constraints = requestSlot.captured.workSpec.constraints
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }

    // ── cancel cancels work by name ───────────────────────────────────────────

    @Test
    fun `cancel cancels work by name`() {
        scheduler.cancel()

        verify { workManager.cancelUniqueWork(UpdateScheduler.WORK_NAME) }
    }

    // ── schedule uses correct work name constant ──────────────────────────────

    @Test
    fun `WORK_NAME constant has expected value`() {
        assertEquals("novel_background_updates", UpdateScheduler.WORK_NAME)
    }

    // ── multiple calls to schedule replace existing work ─────────────────────

    @Test
    fun `schedule called twice invokes enqueueUniquePeriodicWork twice`() {
        scheduler.schedule(6)
        scheduler.schedule(12)

        verify(exactly = 2) {
            workManager.enqueueUniquePeriodicWork(
                UpdateScheduler.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                any()
            )
        }
    }

    // ── schedule with different intervals still sets CONNECTED constraint ─────

    @Test
    fun `schedule with 24 hour interval still sets CONNECTED network constraint`() {
        val requestSlot = slot<PeriodicWorkRequest>()

        every {
            workManager.enqueueUniquePeriodicWork(
                any(),
                any(),
                capture(requestSlot)
            )
        } returns mockk(relaxed = true)

        scheduler.schedule(24)

        val constraints: Constraints = requestSlot.captured.workSpec.constraints
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }

    // ── network constraint is not UNMETERED or NOT_REQUIRED ──────────────────

    @Test
    fun `schedule does not require unmetered network`() {
        val requestSlot = slot<PeriodicWorkRequest>()

        every {
            workManager.enqueueUniquePeriodicWork(
                any(),
                any(),
                capture(requestSlot)
            )
        } returns mockk(relaxed = true)

        scheduler.schedule(6)

        val constraints: Constraints = requestSlot.captured.workSpec.constraints
        assertTrue(
            "Should allow metered connections",
            constraints.requiredNetworkType != NetworkType.UNMETERED
        )
    }

    // ── repeatInterval matches hours parameter ────────────────────────────────

    @Test
    fun `schedule sets repeatInterval matching the hours parameter`() {
        val requestSlot = slot<PeriodicWorkRequest>()

        every {
            workManager.enqueueUniquePeriodicWork(
                any(),
                any(),
                capture(requestSlot)
            )
        } returns mockk(relaxed = true)

        val hours = 8
        scheduler.schedule(hours)

        val expectedMs = hours * 60L * 60L * 1_000L
        assertEquals(
            "intervalDuration should equal hours * 3_600_000 ms",
            expectedMs,
            requestSlot.captured.workSpec.intervalDuration
        )
    }

    @Test
    fun `schedule with 24 hours sets repeatInterval to 24 hours in milliseconds`() {
        val requestSlot = slot<PeriodicWorkRequest>()

        every {
            workManager.enqueueUniquePeriodicWork(
                any(),
                any(),
                capture(requestSlot)
            )
        } returns mockk(relaxed = true)

        scheduler.schedule(24)

        val expectedMs = 24 * 60L * 60L * 1_000L
        assertEquals(expectedMs, requestSlot.captured.workSpec.intervalDuration)
    }
}
