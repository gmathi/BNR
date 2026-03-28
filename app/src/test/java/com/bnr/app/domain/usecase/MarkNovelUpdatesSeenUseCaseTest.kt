package com.bnr.app.domain.usecase

import com.bnr.app.domain.repository.NovelUpdateRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class MarkNovelUpdatesSeenUseCaseTest {

    private val repository: NovelUpdateRepository = mockk(relaxUnitFun = true)

    private lateinit var useCase: MarkNovelUpdatesSeenUseCase

    @Before
    fun setUp() {
        useCase = MarkNovelUpdatesSeenUseCase(repository)
    }

    // ── invoke ────────────────────────────────────────────────────────────────

    @Test
    fun `invoke calls repository markAsSeen with correct novelId`() = runTest {
        val novelId = "src::slug"

        useCase(novelId)

        coVerify(exactly = 1) { repository.markAsSeen(novelId) }
    }
}
