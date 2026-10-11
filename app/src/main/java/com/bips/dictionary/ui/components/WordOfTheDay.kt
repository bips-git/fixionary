package com.bips.dictionary.ui.components

import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bips.dictionary.data.WordEntry
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private class Palette(val a: Color, val b: Color, val c: Color)

// Cool, deep colours (no reds) so white text stays readable in light and dark themes. One palette per word.
private val palettes = listOf(
    Palette(Color(0xFF3730A3), Color(0xFF2563EB), Color(0xFF0E7490)),   // app indigo, blue, cyan
    Palette(Color(0xFF0F766E), Color(0xFF0891B2), Color(0xFF2563EB)),   // teal, cyan, blue
    Palette(Color(0xFF1E40AF), Color(0xFF0369A1), Color(0xFF0F766E)),   // deep blue, sky, teal
    Palette(Color(0xFF065F46), Color(0xFF0F766E), Color(0xFF1D4ED8)),   // green, teal, blue
)

/** False when the phone's animation scale is 0 or battery saver is on: the card then stays still. */
@Composable
private fun rememberMotionAllowed(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f &&
            context.getSystemService(PowerManager::class.java)?.isPowerSaveMode != true
    }
}

/**
 * Word of the day: a slowly shifting gradient with soft drifting glows and a faint initial behind the text.
 * Both loops only change values read while drawing, so nothing is recomposed per frame, and they are
 * skipped entirely when motion is off.
 */
@Composable
fun WordOfTheDayCard(entry: WordEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val first = entry.senses.first()
    val palette = remember(entry.headword) { palettes[(entry.headword.hashCode() and Int.MAX_VALUE) % palettes.size] }
    val motion = rememberMotionAllowed()

    val phase: State<Float> = if (motion) {
        rememberInfiniteTransition(label = "wotd-phase").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "phase",
        )
    } else rememberUpdatedState(0.4f)
    val drift: State<Float> = if (motion) {
        rememberInfiniteTransition(label = "wotd-drift").animateFloat(
            initialValue = 0f, targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing)),
            label = "drift",
        )
    } else rememberUpdatedState(0.6f)

    val enter = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(Unit) { if (motion) enter.animateTo(1f, tween(550, easing = FastOutSlowInEasing)) }

    // Press feedback: a white ripple over the gradient (the click sits after drawBehind so the ripple is drawn
    // on top of it) plus a small spring-back scale.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && motion) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = palette.b,
        contentColor = Color.White,
        modifier = modifier.fillMaxWidth().graphicsLayer {
            alpha = enter.value
            translationY = (1f - enter.value) * 24.dp.toPx()
            scaleX = pressScale
            scaleY = pressScale
        },
    ) {
        Box(
            Modifier.drawBehind {
                val w = size.width
                val h = size.height
                val p = phase.value
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(palette.a, palette.b, palette.c),
                        start = Offset(w * (p - 0.4f), 0f),
                        end = Offset(w * (p + 0.8f), h * 1.4f),
                        tileMode = TileMode.Mirror,
                    ),
                )
                val d = drift.value
                fun glow(cx: Float, cy: Float, radius: Float, alpha: Float) {
                    val center = Offset(cx, cy)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = alpha), Color.Transparent), center, radius),
                        radius = radius, center = center,
                    )
                }
                glow(w * 0.85f + 24.dp.toPx() * cos(d), h * 0.15f + 18.dp.toPx() * sin(d), 130.dp.toPx(), 0.28f)
                glow(w * 0.10f + 20.dp.toPx() * cos(d + 2.1f), h * 0.95f + 16.dp.toPx() * sin(d + 2.1f), 110.dp.toPx(), 0.22f)
                glow(w * 0.55f + 16.dp.toPx() * cos(d + 4.2f), h * 0.55f + 12.dp.toPx() * sin(d + 4.2f), 70.dp.toPx(), 0.14f)
            }.clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White),
                role = Role.Button,
                onClick = onClick,
            ),
        ) {
            Text(
                entry.headword.first().uppercase(),
                fontSize = 160.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.10f),
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 12.dp, y = 30.dp),
            )
            Column(Modifier.padding(20.dp)) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f), contentColor = Color.White) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Word of the day", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    entry.headword,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f), contentColor = Color.White) {
                        Text(first.pos, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                    }
                    entry.ipa?.let {
                        Spacer(Modifier.width(10.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    first.definition,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.95f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                first.example?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "“$it”",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f), contentColor = Color.White) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Open ${entry.headword}",
                            modifier = Modifier.padding(8.dp).size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
