package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CrosshairCanvas(
    config: CrosshairConfig,
    modifier: Modifier = Modifier
) {
    val totalBoxSizeDp = (config.sizeDp * 2f + 20f).coerceAtLeast(60f)
    
    Box(
        modifier = modifier.size(totalBoxSizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(totalBoxSizeDp.dp)
                .offset { IntOffset(config.offsetX, config.offsetY) }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val mainColor = Color(config.color).copy(alpha = config.opacity)
            val outlineColor = Color(config.outlineColor).copy(alpha = config.opacity)

            val strokePx = config.strokeWidthDp.dp.toPx()
            val outlineExtraPx = 1.5f.dp.toPx()
            val outlineStrokePx = strokePx + outlineExtraPx * 2
            val sizePx = (config.sizeDp / 2f).dp.toPx()
            val gapPx = (config.gapDp / 2f).dp.toPx()
            val dotRadiusPx = (config.dotSizeDp / 2f).dp.toPx()

            // Pass 1: Draw outline if enabled
            if (config.hasOutline) {
                drawCrosshairShapes(
                    style = config.style,
                    center = center,
                    mainColor = outlineColor,
                    strokePx = outlineStrokePx,
                    sizePx = sizePx + outlineExtraPx,
                    gapPx = gapPx,
                    dotRadiusPx = if (config.showDot) dotRadiusPx + outlineExtraPx else 0f,
                    showDot = config.showDot
                )
            }

            // Pass 2: Draw main color
            drawCrosshairShapes(
                style = config.style,
                center = center,
                mainColor = mainColor,
                strokePx = strokePx,
                sizePx = sizePx,
                gapPx = gapPx,
                dotRadiusPx = if (config.showDot) dotRadiusPx else 0f,
                showDot = config.showDot
            )
        }
    }
}

private fun DrawScope.drawCrosshairShapes(
    style: CrosshairStyle,
    center: Offset,
    mainColor: Color,
    strokePx: Float,
    sizePx: Float,
    gapPx: Float,
    dotRadiusPx: Float,
    showDot: Boolean
) {
    // 1. Draw center dot if active
    if (showDot && dotRadiusPx > 0f) {
        drawCircle(
            color = mainColor,
            radius = dotRadiusPx,
            center = center
        )
    }

    // 2. Draw style specific reticle
    when (style) {
        CrosshairStyle.CLASSIC_CROSS -> {
            // Left line
            drawLine(mainColor, Offset(center.x - gapPx - sizePx, center.y), Offset(center.x - gapPx, center.y), strokePx, StrokeCap.Round)
            // Right line
            drawLine(mainColor, Offset(center.x + gapPx, center.y), Offset(center.x + gapPx + sizePx, center.y), strokePx, StrokeCap.Round)
            // Top line
            drawLine(mainColor, Offset(center.x, center.y - gapPx - sizePx), Offset(center.x, center.y - gapPx), strokePx, StrokeCap.Round)
            // Bottom line
            drawLine(mainColor, Offset(center.x, center.y + gapPx), Offset(center.x, center.y + gapPx + sizePx), strokePx, StrokeCap.Round)
        }

        CrosshairStyle.DOT -> {
            // Pure dot reticle + subtle outer tiny ring
            val outerRingRadius = (gapPx + sizePx / 3f).coerceAtLeast(dotRadiusPx + 4f)
            drawCircle(
                color = mainColor,
                radius = outerRingRadius,
                center = center,
                style = Stroke(width = strokePx)
            )
        }

        CrosshairStyle.CIRCLE_CROSS -> {
            // Classic cross + outer circle
            val circleRadius = gapPx + sizePx * 0.8f
            drawCircle(
                color = mainColor,
                radius = circleRadius,
                center = center,
                style = Stroke(width = strokePx)
            )
            // Cross lines
            drawLine(mainColor, Offset(center.x - gapPx - sizePx, center.y), Offset(center.x - gapPx, center.y), strokePx, StrokeCap.Round)
            drawLine(mainColor, Offset(center.x + gapPx, center.y), Offset(center.x + gapPx + sizePx, center.y), strokePx, StrokeCap.Round)
            drawLine(mainColor, Offset(center.x, center.y - gapPx - sizePx), Offset(center.x, center.y - gapPx), strokePx, StrokeCap.Round)
            drawLine(mainColor, Offset(center.x, center.y + gapPx), Offset(center.x, center.y + gapPx + sizePx), strokePx, StrokeCap.Round)
        }

        CrosshairStyle.SQUARE_CROSS -> {
            // Square frame
            val rectSide = gapPx + sizePx * 0.9f
            drawRect(
                color = mainColor,
                topLeft = Offset(center.x - rectSide, center.y - rectSide),
                size = Size(rectSide * 2f, rectSide * 2f),
                style = Stroke(width = strokePx)
            )
            // Corner ticks
            drawLine(mainColor, Offset(center.x - gapPx - sizePx, center.y), Offset(center.x - rectSide, center.y), strokePx)
            drawLine(mainColor, Offset(center.x + rectSide, center.y), Offset(center.x + gapPx + sizePx, center.y), strokePx)
            drawLine(mainColor, Offset(center.x, center.y - gapPx - sizePx), Offset(center.x, center.y - rectSide), strokePx)
            drawLine(mainColor, Offset(center.x, center.y + rectSide), Offset(center.x, center.y + gapPx + sizePx), strokePx)
        }

        CrosshairStyle.T_SHAPE -> {
            // T-shape (no top line)
            // Left line
            drawLine(mainColor, Offset(center.x - gapPx - sizePx, center.y), Offset(center.x - gapPx, center.y), strokePx, StrokeCap.Round)
            // Right line
            drawLine(mainColor, Offset(center.x + gapPx, center.y), Offset(center.x + gapPx + sizePx, center.y), strokePx, StrokeCap.Round)
            // Bottom line
            drawLine(mainColor, Offset(center.x, center.y + gapPx), Offset(center.x, center.y + gapPx + sizePx), strokePx, StrokeCap.Round)
        }

        CrosshairStyle.SNIPER -> {
            // Scope circle ring
            val outerRadius = gapPx + sizePx
            drawCircle(
                color = mainColor,
                radius = outerRadius,
                center = center,
                style = Stroke(width = strokePx)
            )
            // Thin inner ring
            val innerRadius = gapPx + sizePx * 0.4f
            drawCircle(
                color = mainColor,
                radius = innerRadius,
                center = center,
                style = Stroke(width = (strokePx * 0.7f).coerceAtLeast(1f))
            )
            // Cross hairs extending outside ring
            drawLine(mainColor, Offset(center.x - outerRadius - sizePx * 0.5f, center.y), Offset(center.x - outerRadius, center.y), strokePx)
            drawLine(mainColor, Offset(center.x + outerRadius, center.y), Offset(center.x + outerRadius + sizePx * 0.5f, center.y), strokePx)
            drawLine(mainColor, Offset(center.x, center.y - outerRadius - sizePx * 0.5f), Offset(center.x, center.y - outerRadius), strokePx)
            drawLine(mainColor, Offset(center.x, center.y + outerRadius), Offset(center.x, center.y + outerRadius + sizePx * 0.5f), strokePx)
        }

        CrosshairStyle.X_CROSS -> {
            // Diagonal cross
            val diagGap = gapPx * 0.707f
            val diagSize = sizePx * 0.707f
            // Top-left to center
            drawLine(mainColor, Offset(center.x - diagGap - diagSize, center.y - diagGap - diagSize), Offset(center.x - diagGap, center.y - diagGap), strokePx, StrokeCap.Round)
            // Bottom-right
            drawLine(mainColor, Offset(center.x + diagGap, center.y + diagGap), Offset(center.x + diagGap + diagSize, center.y + diagGap + diagSize), strokePx, StrokeCap.Round)
            // Top-right
            drawLine(mainColor, Offset(center.x + diagGap, center.y - diagGap), Offset(center.x + diagGap + diagSize, center.y - diagGap - diagSize), strokePx, StrokeCap.Round)
            // Bottom-left
            drawLine(mainColor, Offset(center.x - diagGap - diagSize, center.y + diagGap + diagSize), Offset(center.x - diagGap, center.y + diagGap), strokePx, StrokeCap.Round)
        }

        CrosshairStyle.TRIANGLE -> {
            // Chevron / Triangle
            val triHeight = sizePx * 1.2f
            val triHalfWidth = sizePx * 0.8f
            val path = Path().apply {
                moveTo(center.x, center.y - gapPx - triHeight)
                lineTo(center.x - triHalfWidth, center.y + gapPx)
                lineTo(center.x + triHalfWidth, center.y + gapPx)
                close()
            }
            drawPath(path, mainColor, style = Stroke(width = strokePx))
        }

        CrosshairStyle.DIAMOND -> {
            // Diamond reticle
            val dSize = gapPx + sizePx
            val path = Path().apply {
                moveTo(center.x, center.y - dSize)
                lineTo(center.x + dSize, center.y)
                lineTo(center.x, center.y + dSize)
                lineTo(center.x - dSize, center.y)
                close()
            }
            drawPath(path, mainColor, style = Stroke(width = strokePx))
        }

        CrosshairStyle.HALO_RING -> {
            val ringRadius = gapPx + sizePx * 0.7f
            drawCircle(mainColor, ringRadius, center, style = Stroke(width = strokePx))
            // 4 outer tick notches
            val tickLen = sizePx * 0.3f
            drawLine(mainColor, Offset(center.x - ringRadius - tickLen, center.y), Offset(center.x - ringRadius, center.y), strokePx)
            drawLine(mainColor, Offset(center.x + ringRadius, center.y), Offset(center.x + ringRadius + tickLen, center.y), strokePx)
            drawLine(mainColor, Offset(center.x, center.y - ringRadius - tickLen), Offset(center.x, center.y - ringRadius), strokePx)
            drawLine(mainColor, Offset(center.x, center.y + ringRadius), Offset(center.x, center.y + ringRadius + tickLen), strokePx)
        }

        CrosshairStyle.DYNAMIC_TARGET -> {
            // Concentric circles with cross notches
            val r1 = gapPx + sizePx * 0.4f
            val r2 = gapPx + sizePx * 0.9f
            drawCircle(mainColor, r1, center, style = Stroke(width = strokePx * 0.8f))
            drawCircle(mainColor, r2, center, style = Stroke(width = strokePx))

            drawLine(mainColor, Offset(center.x - r2 - sizePx * 0.3f, center.y), Offset(center.x - r1, center.y), strokePx)
            drawLine(mainColor, Offset(center.x + r1, center.y), Offset(center.x + r2 + sizePx * 0.3f, center.y), strokePx)
            drawLine(mainColor, Offset(center.x, center.y - r2 - sizePx * 0.3f), Offset(center.x, center.y - r1), strokePx)
            drawLine(mainColor, Offset(center.x, center.y + r1), Offset(center.x, center.y + r2 + sizePx * 0.3f), strokePx)
        }

        CrosshairStyle.STAR_CROSS -> {
            // 8 point star reticle
            for (i in 0 until 8) {
                val angle = Math.toRadians(i * 45.0)
                val cosA = cos(angle).toFloat()
                val sinA = sin(angle).toFloat()

                val len = if (i % 2 == 0) sizePx else sizePx * 0.6f
                val start = Offset(center.x + cosA * gapPx, center.y + sinA * gapPx)
                val end = Offset(center.x + cosA * (gapPx + len), center.y + sinA * (gapPx + len))
                drawLine(mainColor, start, end, strokePx, StrokeCap.Round)
            }
        }
    }
}
