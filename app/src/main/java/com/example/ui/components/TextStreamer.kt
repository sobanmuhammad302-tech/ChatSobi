package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SobiCyan
import kotlinx.coroutines.delay

/**
 * TextStreamer: Displays AI responses character-by-character to simulate
 * a natural 'thinking and typing' animation effect (ChatGPT / Claude style).
 *
 * Supports:
 * - Character-by-character animated progression.
 * - Blinking cursor ('▋') in brand cyan while typing.
 * - Interactive tap-to-skip: Tapping reveals full text immediately.
 * - Completion callback [onStreamComplete].
 */
@Composable
fun TextStreamer(
    fullText: String,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    typingSpeedMs: Long = 14L,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    enableCursor: Boolean = true,
    onStreamComplete: () -> Unit = {}
) {
    if (!animate || fullText.isEmpty()) {
        Text(
            text = fullText,
            style = style,
            color = color,
            modifier = modifier
        )
        return
    }

    var displayedCharCount by remember(fullText) { mutableIntStateOf(0) }
    var isTypingComplete by remember(fullText) { mutableStateOf(false) }

    // Blinking cursor transition
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    // Character-by-character streamer loop
    LaunchedEffect(fullText) {
        displayedCharCount = 0
        isTypingComplete = false

        // Start typing loop
        while (displayedCharCount < fullText.length) {
            // Adaptive burst: Advance faster for longer texts or code lines for realistic natural typing
            val remaining = fullText.length - displayedCharCount
            val step = when {
                remaining > 400 -> 3
                remaining > 150 -> 2
                else -> 1
            }
            displayedCharCount = (displayedCharCount + step).coerceAtMost(fullText.length)
            delay(typingSpeedMs)
        }

        isTypingComplete = true
        onStreamComplete()
    }

    val currentText = fullText.take(displayedCharCount)

    Box(
        modifier = modifier
            .testTag("text_streamer")
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Tap to skip typing animation and immediately display full response
                if (!isTypingComplete) {
                    displayedCharCount = fullText.length
                    isTypingComplete = true
                    onStreamComplete()
                }
            }
    ) {
        Text(
            text = buildAnnotatedString {
                append(currentText)
                if (enableCursor && !isTypingComplete) {
                    withStyle(
                        SpanStyle(
                            color = SobiCyan.copy(alpha = cursorAlpha),
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(" ▋")
                    }
                }
            },
            style = style,
            color = color
        )
    }
}
