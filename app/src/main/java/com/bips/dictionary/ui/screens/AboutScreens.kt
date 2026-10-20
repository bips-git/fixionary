package com.bips.dictionary.ui.screens

import androidx.annotation.RawRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bips.dictionary.R
import com.bips.dictionary.ui.components.AccentTile
import com.bips.dictionary.ui.components.DictScreen
import com.bips.dictionary.ui.components.SectionHeader
import com.bips.dictionary.ui.components.accentGlyph
import com.bips.dictionary.ui.components.accentWash
import com.bips.dictionary.ui.theme.Accents

private const val REPO_URL = "https://github.com/bips-git/fixionary"

/** One licence or attribution notice shown in the app. Add an entry here when a new source or library is used. */
private class Notice(
    val id: String,
    val title: String,
    val subtitle: String,
    val licence: String,
    val accent: Color,
    @RawRes val text: Int,
)

private val dataNotices = listOf(
    Notice("oewn", "Open English WordNet", "Definitions, examples, synonyms", "CC BY 4.0", Accents.Green, R.raw.licence_oewn),
    Notice("wordnet", "Princeton WordNet", "Original WordNet 3.0 copyright notice", "WordNet 3.0", Accents.Rose, R.raw.licence_wordnet),
    Notice("wiktionary", "Wiktionary", "Origins, extra words, word forms", "CC BY-SA 4.0", Accents.Blue, R.raw.licence_wiktionary),
    Notice("wordfreq", "wordfreq", "Word-frequency ranking", "CC BY-SA 4.0", Accents.Blue, R.raw.licence_wordfreq),
)
private val softwareNotices = listOf(
    Notice("gpl", "GNU General Public License", "Fixionary's own licence", "GPL-3.0+", Accents.Violet, R.raw.licence_gpl3),
    Notice("apache", "Android and Kotlin libraries", "Jetpack, Compose, Material icons, Kotlin", "Apache-2.0", Accents.Amber, R.raw.licence_apache),
    Notice("inter", "Inter", "Interface typeface", "OFL-1.1", Accents.Teal, R.raw.licence_ofl_inter),
    Notice("manrope", "Manrope", "Heading typeface", "OFL-1.1", Accents.Teal, R.raw.licence_ofl_manrope),
)
private val allNotices = dataNotices + softwareNotices

// The hero is always dark and glowing, in both themes, so its colours are fixed.
private val HeroGradient = listOf(Color(0xFF14123A), Color(0xFF3B2A8F), Color(0xFF0C7C93))
private val Neon = Color(0xFF7CF4D4)
private val NeonViolet = Color(0xFF9D7BFF)

@Composable
fun AboutScreen(onBack: () -> Unit, onOpenFeatures: () -> Unit, onOpenNotice: (String) -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    // Links are handed to the user's browser. That needs no INTERNET permission in this app.
    val openUrl: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } }

    DictScreen(title = "About", onBack = onBack) {
        Hero(version, onOpenFeatures)

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LinkChip("Source code ↗", Icons.Outlined.Code, Accents.Blue) { openUrl(REPO_URL) }
            LinkChip("Report an issue ↗", Icons.Outlined.BugReport, Accents.Rose) { openUrl("$REPO_URL/issues") }
        }

        SectionHeader("Privacy")
        NoticeCard(
            title = "Nothing leaves your phone",
            subtitle = "No internet permission, no ads, no analytics, no accounts. Favourites and history stay on this device.",
            chip = "Always on",
            accent = Accents.Green,
            icon = Icons.Outlined.Lock,
            onClick = onOpenFeatures,
        )

        SectionHeader("Dictionary data")
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            dataNotices.forEach { n ->
                NoticeCard(n.title, n.subtitle, n.licence, n.accent, Icons.AutoMirrored.Outlined.MenuBook) { onOpenNotice(n.id) }
            }
        }

        SectionHeader("Licences")
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            softwareNotices.forEach { n ->
                val icon = if (n.id == "apache" || n.id == "gpl") Icons.Outlined.Gavel else Icons.Outlined.TextFields
                NoticeCard(n.title, n.subtitle, n.licence, n.accent, icon) { onOpenNotice(n.id) }
            }
        }

        CopyrightCard()
        Text(
            "Definitions are provided as published by their sources and come with no warranty. " +
                "Pronunciation uses your phone's own text-to-speech engine.",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Hero(version: String, onOpenFeatures: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(HeroGradient))
            .drawBehind {
                // Two soft glows: neon top right, violet bottom left.
                val a = Offset(size.width * 0.92f, size.height * 0.05f)
                drawCircle(Brush.radialGradient(listOf(Neon.copy(alpha = 0.38f), Color.Transparent), center = a, radius = size.width * 0.55f), radius = size.width * 0.55f, center = a)
                val b = Offset(0f, size.height)
                drawCircle(Brush.radialGradient(listOf(NeonViolet.copy(alpha = 0.40f), Color.Transparent), center = b, radius = size.width * 0.50f), radius = size.width * 0.50f, center = b)
            }
            .border(1.dp, Color.White.copy(alpha = 0.20f), shape)
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(60.dp).clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = Neon, modifier = Modifier.size(32.dp))
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text("Fixionary", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    if (version.isNotEmpty()) {
                        Box(
                            Modifier.padding(top = 4.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                        ) {
                            Text("v$version", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = Neon)
                        }
                    }
                }
            }
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) { append("A Fixed Dictionary.") }
                    append(" ")
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.88f))) {
                        append("Fixes what dictionary apps got wrong.")
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                "An English dictionary that works completely offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 4.dp),
            )
            FlowRow(
                Modifier.padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Offline", "Open source", "No ads", "No tracking").forEach { GlassChip(it, onOpenFeatures) }
            }
        }
    }
}

/** Frosted pill with a small neon "on" dot. */
@Composable
private fun GlassChip(text: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(Neon))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
private fun LinkChip(label: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape)
            .background(accentWash(accent))
            .clickable(onClickLabel = "Open in browser", role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = accentGlyph(accent), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = accentGlyph(accent))
    }
}

/** A tappable card: coloured icon tile, title and subtitle, a licence chip, and a chevron. */
@Composable
private fun NoticeCard(
    title: String,
    subtitle: String,
    chip: String,
    accent: Color,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AccentTile(icon, accent)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    Modifier.padding(top = 8.dp).clip(CircleShape).background(accentWash(accent))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        chip,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentGlyph(accent),
                    )
                }
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CopyrightCard() {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Copyright © 2026 Bipul Das", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
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
                modifier = Modifier.padding(top = 6.dp),
            )
        }
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
