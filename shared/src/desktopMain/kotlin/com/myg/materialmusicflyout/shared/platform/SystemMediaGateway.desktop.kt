package com.myg.materialmusicflyout.shared.platform

import androidx.compose.ui.graphics.Color
import com.myg.materialmusicflyout.shared.model.MusicState
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO

actual fun createSystemMediaGateway(): SystemMediaGateway = WindowsSystemMediaGateway()

private class WindowsSystemMediaGateway : SystemMediaGateway {
    private val client = WinRtMediaBridgeClient()

    override suspend fun readState(previous: MusicState): MusicState? {
        val map = client.getState() ?: return null

        if (map["hasSession"] != "1") {
            return previous.copy(isPlaying = false, positionMs = previous.positionMs.coerceAtMost(previous.durationMs))
        }

        val title = map["title"].orEmpty().ifBlank { previous.songTitle }
        val artist = map["artist"].orEmpty().ifBlank { previous.artist }
        val durationMs = map["durationMs"]?.toLongOrNull()?.coerceAtLeast(0L) ?: previous.durationMs
        val positionMs = map["positionMs"]?.toLongOrNull()?.coerceAtLeast(0L) ?: previous.positionMs
        val isPlaying = map["isPlaying"] == "1"
        val coverBase64 = map["coverBase64"].orEmpty()

        val coverBytes = when {
            coverBase64.isBlank() -> previous.coverArtBytes
            coverBase64 == "null" -> null
            else -> runCatching { Base64.getDecoder().decode(coverBase64) }.getOrNull() ?: previous.coverArtBytes
        }
        val avgCoverColor = coverBytes?.let(::averageColorFromCover)
        val nextPaletteSeed = avgCoverColor?.let(::listOf) ?: previous.paletteSeed

        return previous.copy(
            songTitle = title,
            artist = artist,
            isPlaying = isPlaying,
            durationMs = durationMs,
            positionMs = positionMs.coerceAtMost(durationMs.takeIf { it > 0L } ?: positionMs),
            coverArtBytes = coverBytes,
            paletteSeed = nextPaletteSeed
        )
    }

    override suspend fun togglePlayPause(): Boolean = client.toggle()

    override suspend fun nextTrack(): Boolean = client.next()

    override suspend fun previousTrack(): Boolean = client.previous()
}

private fun averageColorFromCover(bytes: ByteArray): Color? {
    val image = runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull() ?: return null
    val width = image.width
    val height = image.height
    if (width <= 0 || height <= 0) return null

    val stepX = maxOf(1, width / 24)
    val stepY = maxOf(1, height / 24)

    var sumR = 0.0
    var sumG = 0.0
    var sumB = 0.0
    var counted = 0

    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val argb = image.getRGB(x, y)
            val alpha = (argb ushr 24) and 0xFF
            if (alpha > 10) {
                sumR += ((argb ushr 16) and 0xFF)
                sumG += ((argb ushr 8) and 0xFF)
                sumB += (argb and 0xFF)
                counted += 1
            }
            x += stepX
        }
        y += stepY
    }

    if (counted == 0) return null
    return Color(
        red = (sumR / counted / 255.0).toFloat(),
        green = (sumG / counted / 255.0).toFloat(),
        blue = (sumB / counted / 255.0).toFloat(),
        alpha = 1f
    )
}

