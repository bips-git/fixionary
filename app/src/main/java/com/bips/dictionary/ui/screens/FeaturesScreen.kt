package com.bips.dictionary.ui.screens

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bips.dictionary.ui.components.DictGroup
import com.bips.dictionary.ui.components.DictRow
import com.bips.dictionary.ui.components.DictScreen
import com.bips.dictionary.ui.components.accentWash
import com.bips.dictionary.ui.theme.Accents
import kotlin.math.roundToInt

private val Neon = Color(0xFF7CF4D4)
private val HeroGradient = listOf(Color(0xFF14123A), Color(0xFF3B2A8F), Color(0xFF0C7C93))

private class Feature(val title: String, val detail: String, val icon: ImageVector, val accent: Color)

private val features = listOf(
    Feature("Offline", "Every word lives on your phone. Works in flight mode.", Icons.Outlined.WifiOff, Accents.Blue),
    Feature("No ads", "Nothing to scroll past, ever.", Icons.Outlined.Block, Accents.Rose),
    Feature("No tracking", "No analytics, no telemetry, no crash reporters.", Icons.Outlined.VisibilityOff, Accents.Violet),
    Feature("No accounts", "No sign-up, no login. Favourites and history stay on this device.", Icons.Outlined.PersonOff, Accents.Teal),
    Feature("Open source", "GPL-3.0-or-later. Read it, change it, share it.", Icons.Outlined.Code, Accents.Amber),
    Feature("No premium tier", "Every feature is free. Nothing is locked.", Icons.Outlined.MoneyOff, Accents.Green),
    Feature("Light on storage", "One compact install, nothing extra to download.", Icons.Outlined.SdStorage, Accents.Blue),
    Feature("Modern look", "Material 3 with light, dark and AMOLED themes.", Icons.Outlined.AutoAwesome, Accents.Rose),
)

@Composable
fun FeaturesScreen(onBack: () -> Unit) {
    DictScreen(title = "Built in", onBack = onBack) {
        ShieldHeader(features.size)
        Spacer(Modifier.height(16.dp))
        DictGroup(features) { f ->
            DictRow(
                title = f.title,
                subtitle = f.detail,
                icon = f.icon,
                accent = f.accent,
                modifier = Modifier
                    .background(accentWash(f.accent))
                    .semantics(mergeDescendants = true) { stateDescription = "Always on" },
            )
        }
    }
}

/**
 * A ring that sweeps to full while the counter runs up to "8/8", then a soft glow keeps pulsing
 * behind it. Plays once each time the screen opens.
 */
@Composable
private fun ShieldHeader(count: Int) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
    val pulse = rememberInfiniteTransition(label = "shieldPulse")
    val glow by pulse.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(HeroGradient))
            .border(1.dp, Color.White.copy(alpha = 0.20f), shape)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    Brush.radialGradient(listOf(Neon.copy(alpha = glow * progress.value), Color.Transparent)),
                    radius = size.minDimension / 2,
                )
                val stroke = 9.dp.toPx()
                val inset = stroke / 2 + 8.dp.toPx()
                val topLeft = Offset(inset, inset)
                val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
                drawArc(Color.White.copy(alpha = 0.16f), -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                drawArc(Neon, -90f, 360f * progress.value, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = Neon, modifier = Modifier.size(22.dp))
                Text(
                    "${(progress.value * count).roundToInt()}/$count",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
        Column(Modifier.padding(start = 20.dp)) {
            Text("All protections on", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "Built into the app. None of these can be switched off.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
