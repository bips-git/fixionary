package com.bips.dictionary.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bips.dictionary.data.EtymCognate
import com.bips.dictionary.data.EtymStep
import com.bips.dictionary.data.parseEtymology
import com.bips.dictionary.ui.theme.Accents

/**
 * The "Origin" card: the lineage as a vertical timeline (one coloured step per ancestor language),
 * short notes, and the cognates folded away behind a toggle. Plain-prose origins from an older
 * database are shown as a single paragraph.
 */
@Composable
fun EtymologyCard(origin: String, modifier: Modifier = Modifier) {
    val e = remember(origin) { parseEtymology(origin) }
    Surface(
        shape = groupedShape(0, 1),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (e.legacy != null) {
            Text(e.legacy, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            return@Surface
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
            e.steps.forEachIndexed { i, step -> StepRow(step, last = i == e.steps.lastIndex) }
            e.notes.forEach { note ->
                Text(
                    note,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = if (e.steps.isEmpty()) 0.dp else 12.dp),
                )
            }
            if (e.cognates.isNotEmpty()) CognatesBlock(e.cognates, origin)
        }
    }
}

@Composable
private fun StepRow(step: EtymStep, last: Boolean) {
    val accent = langAccent(step.lang)
    val strong = accentGlyph(accent)
    val soft = accentWash(accent)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.height(IntrinsicSize.Min)) {
        // The rail: a dot per step joined to the next one by a thin line.
        Column(Modifier.width(22.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(5.dp))
            Box(Modifier.size(10.dp).clip(CircleShape).background(strong))
            if (!last) Box(Modifier.padding(top = 4.dp).width(2.dp).weight(1f).background(soft))
        }
        Column(Modifier.padding(start = 8.dp, bottom = if (last) 0.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step.lang.isNotEmpty()) {
                    Text(
                        step.lang.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.9.sp),
                        color = strong,
                    )
                }
                relLabel(step.rel)?.let {
                    Text("  ·  $it", style = MaterialTheme.typography.labelSmall, color = muted)
                }
            }
            step.terms.firstOrNull()?.let { main ->
                Text(termText(main, strong, muted), style = MaterialTheme.typography.titleMedium)
            }
            if (step.terms.size > 1) {
                Text(
                    "also " + step.terms.drop(1).joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = muted,
                )
            }
            if (step.gloss.isNotEmpty()) {
                Text("“${step.gloss}”", style = MaterialTheme.typography.bodyMedium, color = muted)
            }
        }
    }
}

@Composable
private fun CognatesBlock(cognates: List<EtymCognate>, key: String) {
    var open by rememberSaveable(key) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "cognateChevron")
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val colors = cognates.map { accentGlyph(langAccent(it.lang)) }
    Column(Modifier.padding(top = 12.dp)) {
        DisableSelection {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = if (open) "Hide cognates" else "Show cognates") { open = !open }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Cognates", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("  ${cognates.size}", style = MaterialTheme.typography.labelLarge, color = muted)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = muted, modifier = Modifier.rotate(rotation))
            }
        }
        AnimatedVisibility(open) {
            Text(
                cognateText(cognates, colors, muted),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
            )
        }
    }
}

private fun cognateText(items: List<EtymCognate>, colors: List<Color>, muted: Color): AnnotatedString = buildAnnotatedString {
    items.forEachIndexed { i, c ->
        if (i > 0) withStyle(SpanStyle(color = muted)) { append("   ·   ") }
        if (c.lang.isNotEmpty()) {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors[i])) { append(c.lang) }
            append(" ")
        }
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(c.terms.joinToString(", ")) }
        if (c.gloss.isNotEmpty()) withStyle(SpanStyle(color = muted)) { append(" “${c.gloss}”") }
    }
}

/** The main form in bold italic, any "(transliteration)" after it in a lighter weight. */
private fun termText(term: String, strong: Color, muted: Color): AnnotatedString = buildAnnotatedString {
    val cut = term.indexOf(" (")
    val head = if (cut < 0) term else term.substring(0, cut)
    withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = strong)) { append(head) }
    if (cut >= 0) {
        withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal, color = muted)) {
            append(term.substring(cut))
        }
    }
}

private fun relLabel(rel: String): String? = when (rel) {
    "borrowed" -> "borrowed"
    "inherited" -> "inherited"
    "learned" -> "learned borrowing"
    "derived" -> "derived"
    "via" -> "via"
    "ultimately" -> "ultimately"
    "probably" -> "probably"
    "possibly" -> "possibly"
    "perhaps" -> "perhaps"
    else -> null
}

/** A colour per language family, so the eras are easy to tell apart at a glance. */
private fun langAccent(lang: String): Color {
    fun has(vararg words: String) = words.any { lang.contains(it) }
    return when {
        lang.startsWith("Proto") -> Accents.Amber
        has("Greek") -> Accents.Violet
        has("Latin", "French", "Norman", "Italian", "Spanish", "Portuguese", "Occitan", "Catalan", "Romanian", "Provençal") -> Accents.Blue
        has("English", "Scots") -> Accents.Teal
        has("German", "Norse", "Dutch", "Frisian", "Gothic", "Swedish", "Danish", "Norwegian", "Icelandic", "Yiddish", "Saxon", "Frankish") -> Accents.Green
        else -> Accents.Rose
    }
}
