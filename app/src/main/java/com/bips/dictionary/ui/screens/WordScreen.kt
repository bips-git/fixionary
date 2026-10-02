package com.bips.dictionary.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.bips.dictionary.data.Entry
import com.bips.dictionary.ui.components.DictGroup
import com.bips.dictionary.ui.components.DictScreen
import com.bips.dictionary.ui.components.SectionHeader
import com.bips.dictionary.ui.components.WordChips
import com.bips.dictionary.ui.theme.Accents

@Composable
fun WordScreen(
    requested: String,
    entry: Entry?,
    onBack: () -> Unit,
    onOpenWord: (String) -> Unit,
) {
    if (entry == null) {
        DictScreen(title = requested, onBack = onBack) {
            Text(
                "No entry for “$requested” yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        return
    }

    // Local only in Phase 1; favourites move to the user database in Phase 6.
    var favourite by rememberSaveable(entry.headword) { mutableStateOf(false) }

    DictScreen(
        title = entry.headword,
        onBack = onBack,
        actions = {
            IconButton(onClick = { favourite = !favourite }) {
                Icon(
                    if (favourite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (favourite) "Remove from favourites" else "Add to favourites",
                    tint = if (favourite) Accents.Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(entry.pos, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                entry.ipa,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionHeader("Meanings")
        // One group per part of speech once entries carry several senses; sample data has one each.
        DictGroup(listOf(entry)) { e ->
            ListItem(
                headlineContent = { Text(e.definition, style = MaterialTheme.typography.bodyLarge) },
                supportingContent = {
                    Text("“${e.example}”", fontStyle = FontStyle.Italic)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        if (entry.synonyms.isNotEmpty()) {
            SectionHeader("Synonyms")
            WordChips(entry.synonyms, Accents.Green, onOpenWord)
        }
        if (entry.antonyms.isNotEmpty()) {
            SectionHeader("Antonyms")
            WordChips(entry.antonyms, Accents.Amber, onOpenWord)
        }
    }
}
