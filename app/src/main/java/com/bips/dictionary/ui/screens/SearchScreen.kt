package com.bips.dictionary.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.bips.dictionary.ui.components.SectionHeader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.bips.dictionary.data.DictionaryRepository
import com.bips.dictionary.data.SearchHit
import com.bips.dictionary.ui.components.DictRow
import com.bips.dictionary.ui.components.groupedShape

@Composable
fun SearchScreen(repo: DictionaryRepository, onBack: () -> Unit, onOpenWord: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    // LaunchedEffect cancels the previous lookup when the query changes, so only the latest one lands.
    var results by remember { mutableStateOf<List<SearchHit>?>(null) }
    var byMeaning by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(query, byMeaning) {
        results = if (byMeaning) repo.searchMeaning(query) else repo.search(query)
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f).focusRequester(focus),
                    singleLine = true,
                    placeholder = { Text(if (byMeaning) "Describe the word" else "Search words") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Search,
                    ),
                    // Enter (soft or hardware keyboard) opens the top hit.
                    keyboardActions = KeyboardActions(onSearch = {
                        results?.firstOrNull()?.let { onOpenWord(it.headword) }
                    }),
                )
            }

            if (repo.hasMeaningIndex) {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = !byMeaning, onClick = { byMeaning = false }, label = { Text("Words") })
                    FilterChip(selected = byMeaning, onClick = { byMeaning = true }, label = { Text("Meanings") })
                }
            }

            val shown = results
            if (shown == null) {
                // first lookup still running
            } else if (shown.isEmpty()) {
                Text(
                    when {
                        byMeaning && query.isBlank() -> "Describe what you are looking for, for example “fear of heights”."
                        byMeaning -> "No definitions match “${query.trim()}”. Try fewer or different words."
                        else -> "No matches for “${query.trim()}”. Check the spelling."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val exact = shown.filter { !it.fuzzy }
                    val similar = shown.filter { it.fuzzy }
                    itemsIndexed(exact, key = { _, e -> e.headword }) { index, entry ->
                        ResultRow(entry, index, exact.size, onOpenWord, Modifier.animateItem())
                    }
                    if (similar.isNotEmpty()) {
                        item(key = "did-you-mean", contentType = "header") {
                            SectionHeader(if (exact.isEmpty()) "Did you mean" else "Similar spellings")
                        }
                        itemsIndexed(similar, key = { _, e -> "~" + e.headword }) { index, entry ->
                            ResultRow(entry, index, similar.size, onOpenWord, Modifier.animateItem())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    entry: SearchHit,
    index: Int,
    count: Int,
    onOpenWord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = groupedShape(index, count),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        DictRow(
            title = entry.headword,
            subtitle = (entry.formNote?.let { it.replaceFirstChar { c -> c.uppercase() } + " · " } ?: "") + "${entry.pos} · ${entry.definition}",
            onClick = { onOpenWord(entry.headword) },
        )
    }
}
