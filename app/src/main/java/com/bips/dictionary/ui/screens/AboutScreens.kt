package com.bips.dictionary.ui.screens

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bips.dictionary.R
import com.bips.dictionary.ui.components.DictGroup
import com.bips.dictionary.ui.components.DictRow
import com.bips.dictionary.ui.components.DictScreen
import com.bips.dictionary.ui.components.SectionHeader
import com.bips.dictionary.ui.theme.Accents

/** One licence or attribution notice shown in the app. Add a row here when a new source or library is used. */
private class Notice(val id: String, val title: String, val subtitle: String, @RawRes val text: Int)

private val dataNotices = listOf(
    Notice("oewn", "Open English WordNet", "Definitions, examples, synonyms · CC BY 4.0", R.raw.licence_oewn),
    Notice("wordnet", "Princeton WordNet licence", "Original WordNet 3.0 copyright notice", R.raw.licence_wordnet),
    Notice("wiktionary", "Wiktionary", "Origins, extra words, forms · CC BY-SA 4.0", R.raw.licence_wiktionary),
    Notice("wordfreq", "wordfreq", "Word-frequency ranking · CC BY-SA 4.0", R.raw.licence_wordfreq),
)
private val softwareNotices = listOf(
    Notice("gpl", "GNU General Public License v3", "Fixionary's own licence · GPL-3.0-or-later", R.raw.licence_gpl3),
    Notice("apache", "Apache License 2.0", "Android Jetpack, Compose, Material icons, Kotlin", R.raw.licence_apache),
    Notice("inter", "Inter font", "SIL Open Font License 1.1", R.raw.licence_ofl_inter),
    Notice("manrope", "Manrope font", "SIL Open Font License 1.1", R.raw.licence_ofl_manrope),
)
private val allNotices = dataNotices + softwareNotices

@Composable
fun AboutScreen(onBack: () -> Unit, onOpenNotice: (String) -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    DictScreen(title = "About", onBack = onBack) {
        Text(
            "Fixionary $version",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        )
        Text(
            "An English dictionary that works completely offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
        )

        Text(
            "Copyright © 2026 Bipul Das",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        Text(
            "Free software: you can redistribute it and/or modify it under the terms of the GNU General Public " +
                "License, version 3 or (at your option) any later version. It comes with ABSOLUTELY NO WARRANTY.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
        )

        SectionHeader("Privacy")
        DictGroup(listOf(0)) {
            DictRow(
                title = "Nothing leaves your phone",
                subtitle = "No internet permission, no ads, no analytics, no accounts. Favourites and history stay on this device.",
                icon = Icons.Outlined.Lock,
                accent = Accents.Green,
            )
        }

        SectionHeader("Dictionary data")
        DictGroup(dataNotices) { n ->
            DictRow(
                title = n.title, subtitle = n.subtitle, icon = Icons.Outlined.MenuBook,
                accent = Accents.Violet, onClick = { onOpenNotice(n.id) },
            )
        }

        SectionHeader("Licences")
        DictGroup(softwareNotices) { n ->
            DictRow(
                title = n.title, subtitle = n.subtitle,
                icon = if (n.id == "apache" || n.id == "gpl") Icons.Outlined.Gavel else Icons.Outlined.TextFields,
                accent = if (n.id == "apache" || n.id == "gpl") Accents.Blue else Accents.Amber,
                onClick = { onOpenNotice(n.id) },
            )
        }

        Text(
            "Definitions are provided as published by their sources and come with no warranty. " +
                "Pronunciation uses your phone's own text-to-speech engine.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun NoticeScreen(id: String, onBack: () -> Unit) {
    val notice = allNotices.firstOrNull { it.id == id }
    val context = LocalContext.current
    val body = remember(id) {
        notice?.let {
            runCatching { context.resources.openRawResource(it.text).bufferedReader().use { r -> r.readText() } }.getOrNull()
        } ?: "Notice not found."
    }
    DictScreen(title = notice?.title ?: "Licence", onBack = onBack) {
        Text(
            body,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp),
        )
    }
}
