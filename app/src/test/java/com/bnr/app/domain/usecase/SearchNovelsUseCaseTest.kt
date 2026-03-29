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
class SearchNovelsUseCaseTest {

    private lateinit var repository: NovelRepository
    private lateinit var useCase: SearchNovelsUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = SearchNovelsUseCase(repository)
    }

    // -------------------------------------------------------------------------
    // Correct param delegation
    // -------------------------------------------------------------------------

    @Test
    fun `delegates to repository searchNovels with all correct params`() = runTest {
        val sourceId = "com.source.test"
        val query = "isekai"
        val page = 3
        val filters = mapOf("genre" to "Action", "status" to "ONGOING")
        val novels = listOf(makeNovel())

        coEvery {
            repository.searchNovels(sourceId, query, page, filters)
        } returns SourceResult.Success(novels)

        useCase(sourceId, query, page, filters)

        coVerify(exactly = 1) { repository.searchNovels(sourceId, query, page, filters) }
    }

    @Test
    fun `uses default page 1 when page not specified`() = runTest {
        coEvery {
            repository.searchNovels(any(), any(), 1, any())
        } returns SourceResult.Success(emptyList())

        useCase("com.source.test", "cultivation")

        coVerify(exactly = 1) { repository.searchNovels(any(), any(), 1, any()) }
    }

    // -------------------------------------------------------------------------
    // Empty filters (default)
    // -------------------------------------------------------------------------

    @Test
    fun `passes empty filters map when filters not specified`() = runTest {
        coEvery {
            repository.searchNovels(any(), any(), any(), emptyMap())
        } returns SourceResult.Success(emptyList())

        useCase("com.source.test", "dragon")

        coVerify(exactly = 1) { repository.searchNovels(any(), any(), any(), emptyMap()) }
    }

    @Test
    fun `passes explicit empty filters map correctly`() = runTest {
        coEvery {
            repository.searchNovels(any(), any(), any(), emptyMap())
        } returns SourceResult.Success(emptyList())

        useCase("com.source.test", "hero", filters = emptyMap())

        coVerify(exactly = 1) { repository.searchNovels(any(), any(), any(), emptyMap()) }
    }

    // -------------------------------------------------------------------------
    // Success path
    // -------------------------------------------------------------------------

    @Test
    fun `returns Success with matching novel list`() = runTest {
        val novels = listOf(
            makeNovel(id = "src::novel-a", title = "Isekai Adventure"),
            makeNovel(id = "src::novel-b", title = "Isekai Hero")
        )
        coEvery {
            repository.searchNovels(any(), any(), any(), any())
        } returns SourceResult.Success(novels)

        val result = useCase("com.source.test", "isekai")

        assertTrue(result is SourceResult.Success)
        assertEquals(novels, (result as SourceResult.Success).data)
    }

    @Test
    fun `returns Success with empty list when no results found`() = runTest {
        coEvery {
            repository.searchNovels(any(), any(), any(), any())
        } returns SourceResult.Success(emptyList())

        val result = useCase("com.source.test", "xyznotexist")

        assertTrue(result is SourceResult.Success)
        assertTrue((result as SourceResult.Success).data.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Error path
    // -------------------------------------------------------------------------

    @Test
    fun `returns Error when repository returns ParseException`() = runTest {
        val error = SourceResult.Error(SourceException.ParseException("Unexpected HTML"))
        coEvery { repository.searchNovels(any(), any(), any(), any()) } returns error

        val result = useCase("com.source.test", "isekai")

        assertTrue(result is SourceResult.Error)
        assertTrue((result as SourceResult.Error).exception is SourceException.ParseException)
    }

    @Test
    fun `returns Error when repository returns NetworkException`() = runTest {
        val error = SourceResult.Error(SourceException.NetworkException("No internet"))
        coEvery { repository.searchNovels(any(), any(), any(), any()) } returns error

        val result = useCase("com.source.test", "isekai")

        assertTrue(result is SourceResult.Error)
        assertTrue((result as SourceResult.Error).exception is SourceException.NetworkException)
    }

    @Test
    fun `propagates the exact SourceResult returned by repository`() = runTest {
        val novels = listOf(makeNovel())
        val repoResult = SourceResult.Success(novels)
        coEvery { repository.searchNovels(any(), any(), any(), any()) } returns repoResult

        val result = useCase("com.source.test", "isekai")

        assertEquals(repoResult, result)
    }
}
