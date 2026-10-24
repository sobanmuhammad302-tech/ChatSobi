package com.example.ui.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.SobiCyan

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KaTeXMathView(
    latexFormula: String,
    modifier: Modifier = Modifier,
    title: String = "KATEX MATHEMATICAL FORMULATION",
    isDark: Boolean = MaterialTheme.colorScheme.background.red < 0.5f
) {
    val context = LocalContext.current
    val cleanedUnicode = remember(latexFormula) { FormulaFormatter.cleanFormula(latexFormula) }

    // Prepare clean LaTeX string for KaTeX renderer
    val escapedLatex = remember(latexFormula) {
        latexFormula
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "")
            .trim()
    }

    val textColor = if (isDark) "#38BDF8" else "#0369A1"
    val htmlData = remember(escapedLatex, isDark) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css">
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js"></script>
            <style>
                * { -webkit-tap-highlight-color: transparent; }
                body {
                    margin: 0;
                    padding: 8px 12px;
                    background-color: transparent;
                    color: $textColor;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    overflow-x: auto;
                    overflow-y: hidden;
                }
                .katex { font-size: 1.22em !important; font-weight: 500; }
                .katex-display { margin: 0 !important; }
            </style>
        </head>
        <body>
            <div id="math-container"></div>
            <script>
                function renderMath() {
                    try {
                        if (typeof katex !== 'undefined') {
                            katex.render(`$escapedLatex`, document.getElementById('math-container'), {
                                displayMode: true,
                                throwOnError: false
                            });
                        } else {
                            setTimeout(renderMath, 150);
                        }
                    } catch (e) {
                        document.getElementById('math-container').innerText = `$escapedLatex`;
                    }
                }
                document.addEventListener("DOMContentLoaded", renderMath);
                window.onload = renderMath;
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
            .border(
                1.5.dp,
                if (isDark) SobiCyan.copy(alpha = 0.5f) else Color(0xFF0284C7).copy(alpha = 0.45f),
                RoundedCornerShape(14.dp)
            )
            .testTag("katex_math_card")
    ) {
        // Header Bar
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
                    imageVector = Icons.Default.Functions,
                    contentDescription = "KaTeX Math",
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
                    clipboard.setPrimaryClip(ClipData.newPlainText("Math Formula", cleanedUnicode))
                    Toast.makeText(context, "Copied math formula to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = if (isDark) Color.LightGray else Color(0xFF475569),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // KaTeX Interactive High-Fidelity WebView Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp, max = 130.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(0) // Transparent background
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL("https://cdn.jsdelivr.net", htmlData, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL("https://cdn.jsdelivr.net", htmlData, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 130.dp)
            )
        }

        // Clean Unicode Accessible Sub-label
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF090D16) else Color(0xFFE8EEF8))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = "Formula: $cleanedUnicode",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                maxLines = 2
            )
        }
    }
}
