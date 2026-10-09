package com.bips.dictionary.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxWidth
import com.bips.dictionary.ui.components.DictLazyScreen
import com.bips.dictionary.ui.components.groupedShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.bips.dictionary.ui.components.WordSkeleton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.bips.dictionary.data.WordEntry
import com.bips.dictionary.data.WordLoad
import com.bips.dictionary.ui.components.DictGroup
import com.bips.dictionary.ui.components.DictScreen
import com.bips.dictionary.ui.components.SectionHeader
import com.bips.dictionary.ui.components.WordChips
import com.bips.dictionary.ui.theme.Accents

private const val MAX_CHIPS = 30

@Composable
fun WordScreen(
    requested: String,
    load: WordLoad,
    onBack: () -> Unit,
    onOpenWord: (String) -> Unit,
    onViewed: (WordEntry) -> Unit,
    canSpeak: Boolean,
    onSpeak: (String) -> Unit,
    isFavourite: Boolean,
    onToggleFavourite: (WordEntry) -> Unit,
) {
    when (load) {
        WordLoad.Loading -> DictScreen(title = requested, onBack = onBack) { WordSkeleton() }
        WordLoad.NotFound -> DictScreen(title = requested, onBack = onBack) {
            Text(
                "No entry for “$requested”.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        is WordLoad.Found -> WordContent(load.entry, onBack, onOpenWord, onViewed, canSpeak, onSpeak, isFavourite, onToggleFavourite)
    }
}

@Composable
private fun WordContent(
    entry: WordEntry,
    onBack: () -> Unit,
    onOpenWord: (String) -> Unit,
    onViewed: (WordEntry) -> Unit,
    canSpeak: Boolean,
    onSpeak: (String) -> Unit,
    favourite: Boolean,
    onToggleFavourite: (WordEntry) -> Unit,
) {
    LaunchedEffect(entry.headword) { onViewed(entry) }


    DictLazyScreen(
        title = entry.headword,
        onBack = onBack,
        actions = {
            if (canSpeak) {
                IconButton(onClick = { onSpeak(entry.headword) }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = "Pronounce ${entry.headword}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val haptic = LocalHapticFeedback.current
            val starScale = remember { Animatable(1f) }
            var firstFrame by remember { mutableStateOf(true) }
            LaunchedEffect(favourite) {
                // Skip the first pass so opening an already-starred word does not bounce.
                if (firstFrame) { firstFrame = false; return@LaunchedEffect }
                starScale.snapTo(0.6f)
                starScale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
            }
            val starTint by animateColorAsState(
                if (favourite) Accents.Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "starTint",
            )
            IconButton(onClick = {
                haptic.performHapticFeedback(if (favourite) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                onToggleFavourite(entry)
            }) {
                Icon(
                    if (favourite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (favourite) "Remove from favourites" else "Add to favourites",
                    tint = starTint,
                    modifier = Modifier.graphicsLayer { scaleX = starScale.value; scaleY = starScale.value },
                )
            }
        },
    ) {
        item {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.senses.map { it.pos }.distinct().joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                entry.ipa?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // One group per part of speech, senses numbered within it.
        entry.senses.groupBy { it.pos }.forEach { (pos, senses) ->
            item(key = "pos-$pos") { SectionHeader(pos.replaceFirstChar { it.uppercase() }) }
            itemsIndexed(senses, key = { i, _ -> "$pos-$i" }) { i, sense ->
                Surface(
                    shape = groupedShape(i, senses.size),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ListItem(
                        leadingContent = {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        },
                        headlineContent = { Text(sense.definition, style = MaterialTheme.typography.bodyLarge) },
                        supportingContent = sense.example?.let { ex -> { Text("“$ex”", fontStyle = FontStyle.Italic) } },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }

        chipSection("Synonyms", entry.synonyms, Accents.Green, onOpenWord)
        chipSection("Antonyms", entry.antonyms, Accents.Amber, onOpenWord)
        chipSection("Broader terms", entry.broader, Accents.Violet, onOpenWord)
        chipSection("Narrower terms", entry.narrower, Accents.Blue, onOpenWord)
        chipSection("Related", entry.related, Accents.Teal, onOpenWord)
    }
}

private fun LazyListScope.chipSection(title: String, words: List<String>, accent: Color, onOpenWord: (String) -> Unit) {
    if (words.isEmpty()) return
    item(key = "hdr-$title") { SectionHeader(title) }
    item(key = "chips-$title") { WordChips(words.take(MAX_CHIPS), accent, onOpenWord) }
}
