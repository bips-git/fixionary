package com.bips.dictionary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bips.dictionary.data.WordEntry

@Composable
fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Search words",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Tap opens the word. Long-press (when [onWordLongClick] is set) runs a secondary action such as copy;
 * the system already gives a long-press haptic.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordChips(
    words: List<String>,
    accent: Color,
    onWordClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onWordLongClick: ((String) -> Unit)? = null,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        words.forEach { word ->
            Box(Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(accentWash(accent))
                        .combinedClickable(
                            role = Role.Button,
                            onClick = { onWordClick(word) },
                            onLongClickLabel = "Copy",
                            onLongClick = onWordLongClick?.let { { it(word) } },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        word,
                        style = MaterialTheme.typography.labelLarge,
                        color = accentGlyph(accent),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}
