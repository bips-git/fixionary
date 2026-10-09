package com.bips.dictionary.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxWidth
import com.bips.dictionary.ui.components.DictLazyScreen
import com.bips.dictionary.ui.components.groupedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bips.dictionary.data.SearchHit
import com.bips.dictionary.ui.components.DictGroup
import com.bips.dictionary.ui.components.DictRow
import com.bips.dictionary.ui.components.DictScreen
import com.bips.dictionary.ui.theme.Accents

@Composable
fun FavouritesScreen(
    items: List<SearchHit>,
    onBack: () -> Unit,
    onOpenWord: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    DictLazyScreen(title = "Favourites", onBack = onBack) {
        if (items.isEmpty()) item { EmptyNote("Tap the star on a word to save it here.") }
        else savedWords(items, favourites = true, onOpenWord = onOpenWord, onRemove = onRemove)
    }
}

@Composable
fun HistoryScreen(
    items: List<SearchHit>,
    onBack: () -> Unit,
    onOpenWord: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    DictLazyScreen(
        title = "History",
        onBack = onBack,
        actions = {
            if (items.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear history")
                }
            }
        },
    ) {
        if (items.isEmpty()) item { EmptyNote("Words you look up will appear here.") }
        else savedWords(items, favourites = false, onOpenWord = onOpenWord, onRemove = onRemove)
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("This removes all ${items.size} words from your history. Favourites are not affected.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; onClear() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

private fun LazyListScope.savedWords(
    items: List<SearchHit>,
    favourites: Boolean,
    onOpenWord: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val label = if (favourites) "Remove from favourites" else "Remove from history"
    itemsIndexed(items, key = { _, hit -> hit.headword }) { index, hit ->
        Surface(
            shape = groupedShape(index, items.size),
            color = MaterialTheme.colorScheme.surfaceContainer,
            // Rows slide into place when one is removed.
            modifier = Modifier.fillMaxWidth().animateItem(),
        ) {
            DictRow(
                title = hit.headword,
                subtitle = "${hit.pos} · ${hit.definition}",
                icon = if (favourites) Icons.Outlined.StarBorder else Icons.Outlined.History,
                accent = if (favourites) Accents.Rose else Accents.Teal,
                onClick = { onOpenWord(hit.headword) },
                trailing = {
                    IconButton(onClick = { onRemove(hit.headword) }) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "$label: ${hit.headword}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
