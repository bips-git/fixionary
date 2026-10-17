package com.bips.dictionary.ui.screens

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
fun AboutScreen(onBack: () -> Unit, onOpenFeatures: () -> Unit, onOpenNotice: (String) -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    DictScreen(title = "About", onBack = onBack) {
        Text(
            buildAnnotatedString {
                append("Fixionary")
                withStyle(
                    SpanStyle(
                        fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) { append("  $version") }
            },
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        )
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                    append("A Fixed Dictionary.")
                }
                append(" ")
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal)) {
                    append("Fixes what dictionary apps got wrong.")
                }
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
        )
        Text(
            "An English dictionary that works completely offline.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
        )

        Text(
            "Copyright © 2026 Bipul Das",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp),
        )
        Text(
            buildAnnotatedString {
                append("Free software: you can redistribute it and/or modify it under the terms of the ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) {
                    append("GNU General Public License")
                }
                append(", version 3 or (at your option) any later version. It comes with ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) {
                    append("ABSOLUTELY NO WARRANTY")
                }
                append(".")
            },
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
        )

        SectionHeader("Privacy")
        DictGroup(listOf(0)) {
            DictRow(
                title = "Nothing leaves your phone",
                subtitle = "No internet permission, no ads, no analytics, no accounts. Favourites and history stay on this device.",
                icon = Icons.Outlined.Lock,
                accent = Accents.Green,
                onClick = onOpenFeatures,
            )
        }

        SectionHeader("Dictionary data")
        DictGroup(dataNotices) { n ->
            DictRow(
                title = n.title, subtitle = n.subtitle, icon = Icons.AutoMirrored.Outlined.MenuBook,
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
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
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
