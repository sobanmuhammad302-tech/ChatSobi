package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SobiCyan
import com.example.ui.theme.SobiGreen
import com.example.ui.theme.SobiNeonPink
import com.example.ui.theme.SobiViolet

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val surfaceColor = if (isDark) {
        Color(0x38131929)
    } else {
        Color(0xFFFFFFFF)
    }

    val borderBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0x5500D4EC),
                Color(0x228B5CF6),
                Color(0x10FFFFFF)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFFE2E8F0),
                Color(0xFFCBD5E1)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(surfaceColor)
            .border(1.dp, borderBrush, RoundedCornerShape(cornerRadius))
            .padding(14.dp)
    ) {
        content()
    }
}

/**
 * Clean Mathematical Formula Formatter
 * Eliminates raw LaTeX symbols, unknown escape codes, and displays standard clean Unicode
 */
object FormulaFormatter {
    fun cleanFormula(raw: String): String {
        var clean = raw
            .replace("\\frac{", "(")
            .replace("}{", " / ")
            .replace("}", ")")
            .replace("\\sqrt{", "√(")
            .replace("\\sqrt", "√")
            .replace("\\pm", "±")
            .replace("\\cdot", " · ")
            .replace("\\times", " × ")
            .replace("\\div", " ÷ ")
            .replace("\\int", "∫ ")
            .replace("\\sum", "∑ ")
            .replace("\\Delta", "Δ")
            .replace("\\pi", "π")
            .replace("\\theta", "θ")
            .replace("\\alpha", "α")
            .replace("\\beta", "β")
            .replace("\\lambda", "λ")
            .replace("\\infty", "∞")
            .replace("\\approx", "≈")
            .replace("\\neq", "≠")
            .replace("\\leq", "≤")
            .replace("\\geq", "≥")
            .replace("\\to", " → ")
            .replace("\\rightarrow", " → ")
            .replace("$$", "")
            .replace("$", "")
            .replace("\\", "")
            .trim()

        // Replace common powers
        clean = clean
            .replace("^2", "²")
            .replace("^3", "³")
            .replace("^4", "⁴")
            .replace("^n", "ⁿ")
            .replace("^x", "ˣ")
            .replace("_0", "₀")
            .replace("_1", "₁")
            .replace("_2", "₂")
            .replace("_n", "ₙ")

        return clean
    }
}

@Composable
fun MathFormulaCard(
    formula: String,
    modifier: Modifier = Modifier,
    title: String = "MATHEMATICAL FORMULA"
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val cleanedFormula = remember(formula) { FormulaFormatter.cleanFormula(formula) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
            .border(
                1.5.dp,
                if (isDark) SobiCyan.copy(alpha = 0.5f) else Color(0xFF0284C7).copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = if (isDark) SobiCyan else Color(0xFF0284C7),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = if (isDark) SobiCyan else Color(0xFF0284C7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Formula", cleanedFormula))
                    Toast.makeText(context, "Formula copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy formula",
                    tint = if (isDark) Color.LightGray else Color(0xFF475569),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cleanedFormula,
                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun EngineStatusBadge(
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val bgBrush = if (isOffline) {
        Brush.horizontalGradient(
            listOf(
                SobiGreen.copy(alpha = if (isDark) 0.25f else 0.15f),
                SobiCyan.copy(alpha = if (isDark) 0.2f else 0.12f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                SobiViolet.copy(alpha = if (isDark) 0.25f else 0.15f),
                SobiNeonPink.copy(alpha = if (isDark) 0.2f else 0.12f)
            )
        )
    }

    val iconColor = if (isOffline) SobiGreen else (if (isDark) SobiCyan else Color(0xFF0284C7))
    val text = if (isOffline) "Offline Engine (0ms)" else "Cloud Gemini 3.5"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(1.dp, iconColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isOffline) Icons.Default.OfflineBolt else Icons.Default.CloudDone,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun NeuralPulsingCore(
    modifier: Modifier = Modifier,
    isSpeaking: Boolean = false,
    size: Dp = 140.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (isSpeaking) 0.95f else 0.98f,
        targetValue = if (isSpeaking) 1.15f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreScale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 4000 else 9000, easing = LinearEasing)
        ),
        label = "coreRotate"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ripple
        Box(
            modifier = Modifier
                .size(size * 1.1f)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SobiCyan.copy(alpha = if (isSpeaking) 0.45f else 0.22f),
                            SobiViolet.copy(alpha = if (isSpeaking) 0.28f else 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Rotating gradient ring
        Box(
            modifier = Modifier
                .size(size * 0.9f)
                .rotate(rotation)
                .clip(CircleShape)
                .border(
                    width = 2.5.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            SobiCyan,
                            SobiViolet,
                            SobiNeonPink,
                            SobiCyan
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Center orb
        Box(
            modifier = Modifier
                .size(size * 0.72f)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            SobiCyan.copy(alpha = 0.9f),
                            SobiViolet.copy(alpha = 0.95f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Neural Core",
                tint = Color.White,
                modifier = Modifier.size(size * 0.35f)
            )
        }
    }
}

@Composable
fun AudioVisualizerWaveform(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    barCount: Int = 18,
    barColor: Color = SobiCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")
    val animVal by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "animWave"
    )

    Row(
        modifier = modifier
            .height(36.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val phase = (i * 0.35f) % 1f
            val baseHeight = if (isActive) {
                val wave = (Math.sin(animVal * Math.PI * 2 + phase * Math.PI) + 1.0) / 2.0
                (10 + (wave * 26)).dp
            } else {
                6.dp
            }

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(baseHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                barColor,
                                SobiViolet
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun CodeBlockViewer(
    code: String,
    modifier: Modifier = Modifier,
    language: String = "syntax"
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF090D16) else Color(0xFF0F172A))
            .border(
                1.dp,
                if (isDark) Color(0x3300D4EC) else Color(0xFF334155),
                RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF131A2A) else Color(0xFF1E293B))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.uppercase(),
                color = SobiCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
                    Toast.makeText(context, "Copied code snippet", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(28.dp)
                    .testTag("copy_code_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = Color.LightGray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Text(
            text = code,
            color = Color(0xFFF1F5F9),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 18.sp,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
fun InteractiveSplitSlider(
    modifier: Modifier = Modifier,
    beforeContent: @Composable () -> Unit,
    afterContent: @Composable () -> Unit
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .height(240.dp)
    ) {
        val widthPx = constraints.maxWidth.toFloat()

        // After layer (full background)
        Box(modifier = Modifier.fillMaxSize()) {
            afterContent()
            Text(
                text = "ENHANCED (4K AI)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color(0xCC009AB2), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Before layer (clipped to left of split)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = splitFraction)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
        ) {
            beforeContent()
            Text(
                text = "ORIGINAL",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Divider Line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .align(Alignment.CenterStart)
                .padding(start = (maxWidth * splitFraction))
                .background(Color.White)
        )

        // Draggable Handle
        Box(
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.CenterStart)
                .padding(start = (maxWidth * splitFraction) - 18.dp)
                .clip(CircleShape)
                .background(SobiCyan)
                .border(2.dp, Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newFraction = (splitFraction + (dragAmount.x / widthPx)).coerceIn(0.05f, 0.95f)
                        splitFraction = newFraction
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⇆",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
