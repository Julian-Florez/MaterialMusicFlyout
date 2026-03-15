package com.myg.materialmusicflyout.shared.model

import androidx.compose.ui.graphics.Color

data class MusicState(
    val songTitle: String,
    val artist: String,
    val isPlaying: Boolean,
    val durationMs: Long,
    val positionMs: Long,
    val coverArtBytes: ByteArray?,
    val paletteSeed: List<Color>
) {
    val progress: Float
        get() = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    val durationSec: Int
        get() = (durationMs / 1000L).toInt()
}

fun demoMusicState(): MusicState = MusicState(
    songTitle = "Midnight Drive",
    artist = "Nova Lights",
    isPlaying = true,
    durationMs = 224_000L,
    positionMs = 72_000L,
    coverArtBytes = null,
    paletteSeed = listOf(
        Color(0xFF304B8E),
        Color(0xFF7A2D7A),
        Color(0xFFCA4F5C),
        Color(0xFFE2A96B),
        Color(0xFF1E202A)
    )
)
