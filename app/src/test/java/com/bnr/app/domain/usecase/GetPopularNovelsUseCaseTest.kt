package com.bnr.app.domain.usecase

import com.bnr.app.domain.repository.NovelRepository
import com.bnr.app.source.SourceException
import com.bnr.app.source.SourceResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GetPopularNovelsUseCaseTest {

    private lateinit var repository: NovelRepository
    private lateinit var useCase: GetPopularNovelsUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetPopularNovelsUseCase(repository)
    }

    // -------------------------------------------------------------------------
    // Delegation with correct params
    // -------------------------------------------------------------------------

    @Test
    fun `delegates to repository getPopularNovels with correct sourceId and page`() = runTest {
        val sourceId = "com.source.test"
        val page = 2
        val novels = listOf(makeNovel(), makeNovel(id = "src::slug2", url = "/novel/slug2"))

        coEvery { repository.getPopularNovels(sourceId, page) } returns SourceResult.Success(novels)

        useCase(sourceId, page)

        coVerify(exactly = 1) { repository.getPopularNovels(sourceId, page) }
    }

    // -------------------------------------------------------------------------
    // Success path
    // -------------------------------------------------------------------------

    @Test
    fun `returns Success with novel list when repository succeeds`() = runTest {
        val novels = listOf(makeNovel(), makeNovel(id = "src::other", title = "Second Novel"))
        coEvery { repository.getPopularNovels(any(), any()) } returns SourceResult.Success(novels)

        val result = useCase("com.source.test", 1)

        assertTrue(result is SourceResult.Success)
        assertEquals(novels, (result as SourceResult.Success).data)
    }

    @Test
    fun `returns Success with empty list when repository returns empty page`() = runTest {
        coEvery { repository.getPopularNovels(any(), any()) } returns SourceResult.Success(emptyList())

        val result = useCase("com.source.test", 99)

        assertTrue(result is SourceResult.Success)
        assertTrue((result as SourceResult.Success).data.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Error path
    // -------------------------------------------------------------------------

    @Test
    fun `returns Error when repository returns NetworkException`() = runTest {
        val error = SourceResult.Error(SourceException.NetworkException("Unreachable"))
        coEvery { repository.getPopularNovels(any(), any()) } returns error

        val result = useCase("com.source.test", 1)

        assertTrue(result is SourceResult.Error)
        assertTrue((result as SourceResult.Error).exception is SourceException.NetworkException)
    }

    @Test
    fun `returns Error when repository returns RateLimitException`() = runTest {
        val error = SourceResult.Error(SourceException.RateLimitException("Too many requests"))
        coEvery { repository.getPopularNovels(any(), any()) } returns error

        val result = useCase("com.source.test", 1)

        assertTrue(result is SourceResult.Error)
        assertTrue((result as SourceResult.Error).exception is SourceException.RateLimitException)
    }

    @Test
    fun `passes through the exact SourceResult returned by repository`() = runTest {
        val novels = listOf(makeNovel())
        val repoResult = SourceResult.Success(novels)
        coEvery { repository.getPopularNovels(any(), any()) } returns repoResult

        val result = useCase("com.source.test", 1)

        assertEquals(repoResult, result)
    }
}
