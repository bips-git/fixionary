package com.bips.dictionary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bips.dictionary.data.SearchHit
import com.bips.dictionary.ui.components.DictLazyScreen
import com.bips.dictionary.ui.components.DictRow
import com.bips.dictionary.ui.components.groupedShape
import com.bips.dictionary.ui.theme.Accents
import kotlinx.coroutines.launch

/** Removal with an undo snackbar, shared by Favourites and History. */
@Composable
private fun rememberUndoRemoval(
    onRemove: (String) -> Unit,
    onRestore: (SearchHit) -> Unit,
): Triple<SnackbarHostState, (SearchHit) -> Unit, Map<String, Int>> {
    val snackbar = remember { SnackbarHostState() }
    // Bumped on every removal so a restored row gets a fresh identity and never inherits the swiped-away state.
    val generations = remember { mutableStateMapOf<String, Int>() }
    val scope = rememberCoroutineScope()
    val remove: (SearchHit) -> Unit = { hit ->
        generations[hit.headword] = (generations[hit.headword] ?: 0) + 1
        onRemove(hit.headword)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "Removed ${hit.headword}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onRestore(hit)
        }
    }
    return Triple(snackbar, remove, generations)
}

@Composable
fun FavouritesScreen(
    items: List<SearchHit>,
    onBack: () -> Unit,
    onOpenWord: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRestore: (SearchHit) -> Unit,
) {
    val (snackbar, remove, generations) = rememberUndoRemoval(onRemove, onRestore)
    DictLazyScreen(
        title = "Favourites",
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbar) },
    ) {
        if (items.isEmpty()) item { EmptyNote("Tap the star on a word to save it here.") }
        else savedWords(items, favourites = true, generations = generations, onOpenWord = onOpenWord, onRemove = remove)
    }
}

@Composable
fun HistoryScreen(
    items: List<SearchHit>,
    onBack: () -> Unit,
    onOpenWord: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRestore: (SearchHit) -> Unit,
    onClear: () -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val (snackbar, remove, generations) = rememberUndoRemoval(onRemove, onRestore)

    DictLazyScreen(
        title = "History",
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbar) },
        actions = {
            if (items.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear history")
                }
            }
        },
    ) {
        if (items.isEmpty()) item { EmptyNote("Words you look up will appear here.") }
        else savedWords(items, favourites = false, generations = generations, onOpenWord = onOpenWord, onRemove = remove)
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

@OptIn(ExperimentalMaterial3Api::class)
private fun LazyListScope.savedWords(
    items: List<SearchHit>,
    favourites: Boolean,
    generations: Map<String, Int>,
    onOpenWord: (String) -> Unit,
    onRemove: (SearchHit) -> Unit,
) {
    val label = if (favourites) "Remove from favourites" else "Remove from history"
    itemsIndexed(items, key = { _, hit -> "${hit.headword}#${generations[hit.headword] ?: 0}" }) { index, hit ->
        val shape = groupedShape(index, items.size)
        val dismiss = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) { onRemove(hit); true } else false
            },
        )
        // Swipe left to remove. Rows slide into place when one goes.
        SwipeToDismissBox(
            state = dismiss,
            enableDismissFromStartToEnd = false,
            modifier = Modifier.animateItem(),
            backgroundContent = {
                if (dismiss.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(end = 24.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            },
        ) {
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                DictRow(
                    title = hit.headword,
                    subtitle = "${hit.pos} · ${hit.definition}",
                    icon = if (favourites) Icons.Outlined.StarBorder else Icons.Outlined.History,
                    accent = if (favourites) Accents.Rose else Accents.Teal,
                    onClick = { onOpenWord(hit.headword) },
                    trailing = {
                        IconButton(onClick = { onRemove(hit) }) {
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
