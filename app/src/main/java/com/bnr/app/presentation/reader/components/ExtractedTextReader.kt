package com.bnr.app.presentation.reader.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExtractedTextReader(
    paragraphs: List<String>,
    fontSize: Float,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(paragraphs) { paragraph ->
            Text(
                text = paragraph,
                style = TextStyle(
                    fontSize = fontSize.sp,
                    color = textColor,
                    lineHeight = (fontSize * 1.7f).sp
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}
