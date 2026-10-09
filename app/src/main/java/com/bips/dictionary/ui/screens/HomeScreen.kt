package com.bips.dictionary.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bips.dictionary.data.SearchHit
import com.bips.dictionary.data.WordEntry
import com.bips.dictionary.ui.components.DictGroup
import com.bips.dictionary.ui.components.DictRow
import com.bips.dictionary.ui.components.DictScreen
import com.bips.dictionary.ui.components.SearchPill
import com.bips.dictionary.ui.components.SectionHeader
import com.bips.dictionary.ui.components.WordOfTheDayCard
import com.bips.dictionary.ui.theme.Accents

@Composable
fun HomeScreen(
    wordOfTheDay: WordEntry?,
    recents: List<SearchHit>,
    onOpenSettings: () -> Unit,
    onSearch: () -> Unit,
    onOpenWord: (String) -> Unit,
    onOpenFavourites: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    DictScreen(
        title = "Fixionary",
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        },
    ) {
        SearchPill(onClick = onSearch)
        Spacer(Modifier.height(16.dp))
        wordOfTheDay?.let { WordOfTheDayCard(it, onClick = { onOpenWord(it.headword) }) }

        SectionHeader("Library")
        DictGroup(listOf(0, 1)) { i ->
            if (i == 0) DictRow(
                title = "Favourites", subtitle = "Words you starred",
                icon = Icons.Outlined.StarBorder, accent = Accents.Rose, onClick = onOpenFavourites,
            ) else DictRow(
                title = "History", subtitle = "Everything you looked up",
                icon = Icons.Outlined.History, accent = Accents.Teal, onClick = onOpenHistory,
            )
        }

        SectionHeader("Recent")
        if (recents.isEmpty()) {
            Text(
                "Words you look up will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            DictGroup(recents) { entry ->
                DictRow(
                    title = entry.headword,
                    subtitle = "${entry.pos} · ${entry.definition}",
                    icon = Icons.Outlined.History,
                    accent = Accents.Teal,
                    onClick = { onOpenWord(entry.headword) },
                )
            }
        }
    }
}
