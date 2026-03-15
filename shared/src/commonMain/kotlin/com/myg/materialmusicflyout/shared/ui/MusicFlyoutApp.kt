package com.myg.materialmusicflyout.shared.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.myg.materialmusicflyout.shared.logic.blendOverlay
import com.myg.materialmusicflyout.shared.logic.paletteFromCoverSeed
import com.myg.materialmusicflyout.shared.platform.decodeCoverArt
import com.myg.materialmusicflyout.shared.state.MusicController
import materialmusicflyout.shared.generated.resources.GoogleSansFlex_Regular
import materialmusicflyout.shared.generated.resources.Res
import materialmusicflyout.shared.generated.resources.cover_preview
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalResourceApi::class)
@Composable
fun MusicFlyoutApp(
    modifier: Modifier = Modifier,
    uiScale: Float = 1f,
    alwaysOnTop: Boolean = false,
    onToggleAlwaysOnTop: () -> Unit = {},
    onCloseRequest: () -> Unit = {},
    onDragWindowStart: () -> Unit = {},
    onDragWindow: (Float, Float) -> Unit = { _, _ -> },
    onDragWindowEnd: () -> Unit = {}
) {
    val scale = uiScale.coerceIn(0.4f, 1.2f)
    val appFontFamily = FontFamily(Font(Res.font.GoogleSansFlex_Regular))
    val controller = remember { MusicController() }
    val state by controller.state.collectAsState()
    val palette = remember(state.paletteSeed) { paletteFromCoverSeed(state.paletteSeed) }

    DisposableEffect(Unit) { onDispose { controller.clear() } }

    val primary by animateColorAsState(palette.dominant, tween(450), label = "p1")
    val secondary by animateColorAsState(palette.container, tween(450), label = "p2")
    val accent by animateColorAsState(palette.accent, tween(450), label = "p3")
    val accentDark by animateColorAsState(lerp(palette.accent, Color.Black, 0.62f), tween(450), label = "p3_dark")
    val onAccent by animateColorAsState(palette.onAccent, tween(450), label = "p4")

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = accent,
            surface = secondary,
            onPrimary = onAccent,
            onSurface = Color.White
        ),
        typography = MaterialTheme.typography.copy(
            headlineLarge = TextStyle(fontFamily = appFontFamily, fontWeight = FontWeight.SemiBold),
            titleMedium = TextStyle(fontFamily = appFontFamily, fontWeight = FontWeight.Normal),
            bodyMedium = TextStyle(fontFamily = appFontFamily, fontWeight = FontWeight.Normal)
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp, vertical = 0.dp),
            contentAlignment = Alignment.Center
        ) {
            MusicCard(
                songTitle = state.songTitle,
                artist = state.artist,
                isPlaying = state.isPlaying,
                progress = state.progress,
                coverArtBytes = state.coverArtBytes,
                accent = accent,
                overlayColor = accentDark,
                uiScale = scale,
                alwaysOnTop = alwaysOnTop,
                fontFamily = appFontFamily,
                onPlayPause = controller::togglePlayPause,
                onSeek = controller::setProgress,
                onBack = controller::previousTrack,
                onForward = controller::nextTrack,
                onToggleAlwaysOnTop = onToggleAlwaysOnTop,
                onCloseRequest = onCloseRequest,
                onDragWindowStart = onDragWindowStart,
                onDragWindow = onDragWindow,
                onDragWindowEnd = onDragWindowEnd
            )
        }
    }
}

@Composable
private fun MusicCard(
    songTitle: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    coverArtBytes: ByteArray?,
    accent: Color,
    overlayColor: Color,
    uiScale: Float,
    alwaysOnTop: Boolean,
    fontFamily: FontFamily,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onToggleAlwaysOnTop: () -> Unit,
    onCloseRequest: () -> Unit,
    onDragWindowStart: () -> Unit,
    onDragWindow: (Float, Float) -> Unit,
    onDragWindowEnd: () -> Unit
) {
    val coverBitmap = remember(coverArtBytes) { coverArtBytes?.let(::decodeCoverArt) }
    val fallbackPainter = painterResource(Res.drawable.cover_preview)
    val coverPainter = remember(coverBitmap, fallbackPainter) {
        coverBitmap?.let(::BitmapPainter) ?: fallbackPainter
    }
    val shape = RoundedCornerShape((32f * uiScale).dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
    ) {
        Image(
            painter = coverPainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur((7f * uiScale).dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(blendOverlay(overlayColor, 0.30f))
        )

        TopControlsBar(
            uiScale = uiScale,
            alwaysOnTop = alwaysOnTop,
            onToggleAlwaysOnTop = onToggleAlwaysOnTop,
            onCloseRequest = onCloseRequest,
            onDragWindowStart = onDragWindowStart,
            onDragWindow = onDragWindow,
            onDragWindowEnd = onDragWindowEnd,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = (24f * uiScale).dp,
                    end = (24f * uiScale).dp,
                    top = (56f * uiScale).dp,
                    bottom = (30f * uiScale).dp
                )
        ) {
            MiddleRow(
                songTitle = songTitle,
                artist = artist,
                isPlaying = isPlaying,
                accent = accent,
                uiScale = uiScale,
                fontFamily = fontFamily,
                onPlayPause = onPlayPause,
                modifier = Modifier.align(Alignment.Center)
            )
            BottomRow(
                progress = progress,
                isPlaying = isPlaying,
                uiScale = uiScale,
                onBack = onBack,
                onForward = onForward,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun TopControlsBar(
    uiScale: Float,
    alwaysOnTop: Boolean,
    onToggleAlwaysOnTop: () -> Unit,
    onCloseRequest: () -> Unit,
    onDragWindowStart: () -> Unit,
    onDragWindow: (Float, Float) -> Unit,
    onDragWindowEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barHeight = (52f * uiScale).dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .padding(horizontal = (30f * uiScale).dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onDragWindowStart() },
                        onDragEnd = { onDragWindowEnd() },
                        onDragCancel = { onDragWindowEnd() }
                    ) { change, dragAmount ->
                        change.consume()
                        onDragWindow(dragAmount.x, dragAmount.y)
                    }
                }
        )
        TopIconButton(
            icon = if (alwaysOnTop) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
            uiScale = uiScale,
            onClick = onToggleAlwaysOnTop,
            modifier = Modifier.padding(top = (10f * uiScale).dp, end = (16f * uiScale).dp)
        )
        TopIconButton(
            icon = Icons.Rounded.Close,
            uiScale = uiScale,
            onClick = onCloseRequest,
            modifier = Modifier.padding(top = (10f * uiScale).dp)
        )
    }
}

@Composable
private fun TopIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    uiScale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val hoverColor = if (hovered) Color.White.copy(alpha = 0.14f) else Color.Transparent

    Box(
        modifier = modifier
            .size((68f * uiScale).dp)
            .clip(CircleShape)
            .background(hoverColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Draw twice with a tiny offset so glyphs look bolder without growing.
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size((42f * uiScale).dp)
                .offset { IntOffset((0.6f * uiScale).dp.roundToPx(), 0) }
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((42f * uiScale).dp)
        )
    }
}

@Composable
private fun MiddleRow(
    songTitle: String,
    artist: String,
    isPlaying: Boolean,
    accent: Color,
    uiScale: Float,
    fontFamily: FontFamily,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = (18f * uiScale).dp, end = (148f * uiScale).dp)
        ) {
            Text(
                text = songTitle,
                color = Color.White,
                fontFamily = fontFamily,
                fontSize = (34f * uiScale).sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
            Text(
                text = artist,
                color = Color.White.copy(alpha = 0.76f),
                fontFamily = fontFamily,
                fontSize = (30f * uiScale).sp
            )
        }

        PlayPauseButton(
            isPlaying = isPlaying,
            accent = accent,
            uiScale = uiScale,
            onToggle = onPlayPause,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun BottomRow(
    progress: Float,
    isPlaying: Boolean,
    uiScale: Float,
    onBack: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = (58f * uiScale).dp)) {
            WaveProgressBar(progress = progress, isPlaying = isPlaying, uiScale = uiScale)
        }

        IconGhostButton(
            icon = Icons.Rounded.SkipPrevious,
            uiScale = uiScale,
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        IconGhostButton(
            icon = Icons.Rounded.SkipNext,
            uiScale = uiScale,
            onClick = onForward,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun IconGhostButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    uiScale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val hoverColor = if (hovered) Color.White.copy(alpha = 0.14f) else Color.Transparent

    Box(
        modifier = modifier
            .size((50f * uiScale).dp)
            .clip(CircleShape)
            .background(hoverColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((42f * uiScale).dp)
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    accent: Color,
    uiScale: Float,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val corner by animateDpAsState(
        if (isPlaying) (34f * uiScale).dp else (64f * uiScale).dp,
        label = "play_shape"
    )
    val buttonColor = lerp(accent, Color.White, 0.40f)
    val iconColor = lerp(accent, Color.Black, 0.74f)

    Box(
        modifier = modifier
            .size((128f * uiScale).dp)
            .clip(RoundedCornerShape(corner))
            .clickable(onClick = onToggle)
            .background(buttonColor),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(targetState = isPlaying, label = "play_pause") { playing ->
            if (playing) {
                Icon(
                    imageVector = Icons.Rounded.Pause,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size((52f * uiScale).dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size((56f * uiScale).dp)
                )
            }
        }
    }
}

@Composable
private fun WaveProgressBar(progress: Float, isPlaying: Boolean, uiScale: Float) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        0f,
        (2f * PI).toFloat(),
        infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    val active by animateFloatAsState(if (isPlaying) 1f else 0f, tween(260), label = "wave_active")

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height((52f * uiScale).dp)
    ) {
        val centerY = size.height * 0.5f
        val amplitude = size.height * 0.10f * active * uiScale
        val progressX = size.width * progress

        if (progressX < size.width) {
            drawLine(
                color = Color.White.copy(alpha = 0.20f),
                start = Offset(progressX, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 3.2f * uiScale,
                cap = StrokeCap.Round
            )
        }

        val pathPlayed = Path().apply {
            moveTo(0f, centerY)
            var x = 0f
            while (x <= progressX) {
                lineTo(x, centerY + sin((x / (10f * uiScale)) + phase) * amplitude)
                x += 2f
            }
        }

        drawPath(pathPlayed, Color.White, style = Stroke(5.6f * uiScale, cap = StrokeCap.Round))

        val indicatorWidth = 7f * uiScale
        val indicatorHeight = 24f * uiScale
        drawRoundRect(
            color = Color.White,
            topLeft = Offset((progressX - indicatorWidth / 2f).coerceIn(0f, size.width - indicatorWidth), centerY - indicatorHeight / 2f),
            size = Size(indicatorWidth, indicatorHeight),
            cornerRadius = CornerRadius(indicatorWidth, indicatorWidth)
        )
    }
}
