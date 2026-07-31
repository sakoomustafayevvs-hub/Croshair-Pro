package com.example.model

data class CrosshairConfig(
    val style: CrosshairStyle = CrosshairStyle.CLASSIC_CROSS,
    val color: Long = 0xFF00FF66L, // Neon green default
    val sizeDp: Float = 36f,
    val strokeWidthDp: Float = 3f,
    val gapDp: Float = 6f,
    val dotSizeDp: Float = 4f,
    val showDot: Boolean = true,
    val hasOutline: Boolean = true,
    val outlineColor: Long = 0xFF000000L,
    val opacity: Float = 1.0f,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val showFloatingSquare: Boolean = true,
    val squareX: Int = 40,
    val squareY: Int = 200
)

data class CrosshairPreset(
    val id: String,
    val name: String,
    val gameName: String,
    val config: CrosshairConfig
)
