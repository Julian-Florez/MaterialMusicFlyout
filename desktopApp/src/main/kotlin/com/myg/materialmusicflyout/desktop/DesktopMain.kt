package com.myg.materialmusicflyout.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.myg.materialmusicflyout.shared.ui.MusicFlyoutApp
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.MouseInfo
import java.awt.Point

fun main() = application {
    val windowIcon = runCatching { painterResource("app-icon.png") }.getOrNull()
    var pinned by remember { mutableStateOf(false) }
    var dragMouseAnchor by remember { mutableStateOf<Point?>(null) }
    var dragWindowAnchor by remember { mutableStateOf<Point?>(null) }
    var restoreAfterPinToggle by remember { mutableStateOf<Pair<Point, Dimension>?>(null) }
    val windowState = WindowState(
        width = 300.dp,
        height = 200.dp
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Material Music Flyout",
        icon = windowIcon,
        state = windowState,
        resizable = false,
        undecorated = true,
        transparent = true,
        alwaysOnTop = pinned
    ) {
        LaunchedEffect(Unit) {
            window.toFront()
            window.requestFocus()
        }

        LaunchedEffect(pinned) {
            restoreAfterPinToggle?.let {
                val location = it.first
                val size = it.second
                window.setBounds(location.x, location.y, size.width, size.height)
                EventQueue.invokeLater {
                    window.setBounds(location.x, location.y, size.width, size.height)
                }
                restoreAfterPinToggle = null
            }
        }

        MusicFlyoutApp(
            uiScale = 0.5f,
            alwaysOnTop = pinned,
            onToggleAlwaysOnTop = {
                restoreAfterPinToggle = Point(window.x, window.y) to Dimension(window.width, window.height)
                pinned = !pinned
            },
            onCloseRequest = ::exitApplication,
            onDragWindowStart = {
                dragMouseAnchor = MouseInfo.getPointerInfo()?.location
                dragWindowAnchor = window.location
            },
            onDragWindow = { _, _ ->
                val currentMouse = MouseInfo.getPointerInfo()?.location
                val startMouse = dragMouseAnchor
                val startWindow = dragWindowAnchor
                if (currentMouse != null && startMouse != null && startWindow != null) {
                    window.setLocation(
                        startWindow.x + (currentMouse.x - startMouse.x),
                        startWindow.y + (currentMouse.y - startMouse.y)
                    )
                }
            },
            onDragWindowEnd = {
                dragMouseAnchor = null
                dragWindowAnchor = null
            }
        )
    }
}

