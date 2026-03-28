package com.bnr.app.data.local.saf

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.bnr.app.domain.model.ChapterContent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages chapter file storage via the Storage Access Framework (SAF).
 *
 * File layout inside the user-chosen root tree:
 *   <root>/novels/<novelId>/<chapterId>.txt
 *
 * The first line of each .txt file is the chapter title.
 * Subsequent lines are paragraphs (one per line, blank line between paragraphs).
 * The last two lines (prefixed with "PREV:" and "NEXT:") are navigation URLs.
 */
@Singleton
class SafStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Writes chapter content to the user-chosen folder.
     * Returns the persisted document URI string on success, null on failure.
     */
    suspend fun writeChapterContent(
        rootUriString: String,
        novelId: String,
        chapterId: String,
        content: ChapterContent
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val rootUri = Uri.parse(rootUriString)
            val novelsDir = getOrCreateDir(DocumentFile.fromTreeUri(context, rootUri)!!, "novels")
                ?: return@runCatching null
            val novelDir = getOrCreateDir(novelsDir, sanitizeName(novelId))
                ?: return@runCatching null

            val fileName = "${sanitizeName(chapterId)}.txt"
            // Delete old version if exists
            novelDir.findFile(fileName)?.delete()

            val file = novelDir.createFile("text/plain", sanitizeName(chapterId))
                ?: return@runCatching null

            context.contentResolver.openOutputStream(file.uri)?.bufferedWriter()?.use { writer ->
                writer.write(content.title)
                writer.newLine()
                writer.write("---")
                writer.newLine()
                content.paragraphs.forEach { para ->
                    writer.write(para)
                    writer.newLine()
                    writer.write("")
                    writer.newLine()
                }
                writer.write("PREV:${content.previousChapterUrl ?: ""}")
                writer.newLine()
                writer.write("NEXT:${content.nextChapterUrl ?: ""}")
            }

            file.uri.toString()
        }.getOrNull()
    }

    /**
     * Reads a previously downloaded chapter from its stored URI string.
     * Returns null if the file is missing or inaccessible.
     */
    suspend fun readChapterContent(fileUriString: String): ChapterContent? =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(fileUriString)
                val lines = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.readLines()
                    ?: return@runCatching null

                if (lines.isEmpty()) return@runCatching null

                val title = lines.firstOrNull() ?: ""
                val separatorIdx = lines.indexOf("---")
                val contentStart = if (separatorIdx >= 0) separatorIdx + 1 else 1

                val navPrev = lines.lastOrNull { it.startsWith("PREV:") }
                    ?.removePrefix("PREV:")?.takeIf { it.isNotEmpty() }
                val navNext = lines.lastOrNull { it.startsWith("NEXT:") }
                    ?.removePrefix("NEXT:")?.takeIf { it.isNotEmpty() }

                val contentEnd = lines.indexOfLast { it.startsWith("PREV:") || it.startsWith("NEXT:") }
                    .takeIf { it > 0 } ?: lines.size

                val paragraphs = lines.subList(contentStart, contentEnd)
                    .filter { it.isNotBlank() }

                // Extract chapterId from URI filename
                val chapterId = uri.lastPathSegment?.removeSuffix(".txt") ?: ""

                ChapterContent(
                    chapterId = chapterId,
                    title = title,
                    paragraphs = paragraphs,
                    previousChapterUrl = navPrev,
                    nextChapterUrl = navNext
                )
            }.getOrNull()
        }

    /**
     * Checks whether the persisted tree URI still has write permission.
     */
    fun hasValidPermission(uriString: String): Boolean {
        val uri = Uri.parse(uriString)
        return context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isWritePermission
        }
    }

    /**
     * Returns a human-readable display name for the chosen folder URI.
     */
    fun getFolderDisplayName(uriString: String): String? {
        return runCatching {
            val uri = Uri.parse(uriString)
            DocumentFile.fromTreeUri(context, uri)?.name
        }.getOrNull()
    }

    private fun getOrCreateDir(parent: DocumentFile, name: String): DocumentFile? {
        return parent.findFile(name)?.takeIf { it.isDirectory }
            ?: parent.createDirectory(name)
    }

    private fun sanitizeName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(100)
}
