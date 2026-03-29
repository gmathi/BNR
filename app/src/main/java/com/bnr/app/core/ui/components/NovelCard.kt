package com.bnr.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bnr.app.R
import com.bnr.app.domain.model.Novel

@Composable
fun NovelCard(
    novel: Novel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inLibrary: Boolean = false,
    newChaptersCount: Int = 0
) {
    Card(
        modifier = modifier
            .width(130.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            Box {
                AsyncImage(
                    model = novel.coverUrl,
                    contentDescription = stringResource(R.string.cd_novel_cover),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                )
                if (inLibrary) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                                shape = RoundedCornerShape(bottomStart = 8.dp)
                            )
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = stringResource(R.string.cd_in_library),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (newChaptersCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(topEnd = 8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+$newChaptersCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (novel.author.isNotBlank()) {
                    Text(
                        text = novel.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
