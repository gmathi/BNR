package com.bnr.app.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bnr.app.R
import com.bnr.app.domain.model.Chapter
import com.bnr.app.domain.model.DownloadState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChapterItem(
    chapter: Chapter,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (chapter.isRead)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                chapter.uploadedAt?.let { millis ->
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(millis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DownloadButton(
                state = chapter.downloadState,
                onDownloadClick = onDownloadClick
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun DownloadButton(
    state: DownloadState,
    onDownloadClick: () -> Unit
) {
    when (state) {
        DownloadState.NOT_DOWNLOADED -> IconButton(onClick = onDownloadClick) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = stringResource(R.string.download_not_downloaded),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DownloadState.QUEUED -> IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.HourglassBottom,
                contentDescription = stringResource(R.string.download_queued),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
        DownloadState.DOWNLOADING -> CircularProgressIndicator(
            modifier = Modifier
                .padding(12.dp)
                .size(24.dp),
            strokeWidth = 2.dp
        )
        DownloadState.DOWNLOADED -> Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = stringResource(R.string.download_downloaded),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(12.dp)
        )
        DownloadState.FAILED -> IconButton(onClick = onDownloadClick) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.download_failed),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
