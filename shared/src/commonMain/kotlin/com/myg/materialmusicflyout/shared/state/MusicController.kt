package com.myg.materialmusicflyout.shared.state

import com.myg.materialmusicflyout.shared.model.MusicState
import com.myg.materialmusicflyout.shared.model.demoMusicState
import com.myg.materialmusicflyout.shared.platform.createSystemMediaGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MusicController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val gateway = createSystemMediaGateway()
    private var syncJob: Job? = null

    private val _state = MutableStateFlow(demoMusicState())
    val state: StateFlow<MusicState> = _state.asStateFlow()
    private var lastSyncAtMs: Long = System.currentTimeMillis()
    private var lastPollAtMs: Long = 0L
    private var lastWinRtPositionMs: Long? = null
    private var lastWinRtTrackKey: String? = null

    init {
        startSyncLoop()
    }

    fun togglePlayPause() {
        scope.launch {
            gateway.togglePlayPause()
        }
    }

    fun nextTrack() {
        scope.launch {
            gateway.nextTrack()
        }
    }

    fun previousTrack() {
        scope.launch {
            gateway.previousTrack()
        }
    }

    fun setProgress(progress: Float) {
        _state.update { current ->
            val target = (progress.coerceIn(0f, 1f) * current.durationMs).toLong()
            current.copy(positionMs = target)
        }
        lastSyncAtMs = System.currentTimeMillis()
    }

    private fun startSyncLoop() {
        if (syncJob != null) return
        syncJob = scope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                try {
                    val current = _state.value
                    if (now - lastPollAtMs >= 700L) {
                        lastPollAtMs = now
                        val synced = gateway.readState(current)
                        if (synced != null) {
                            val trackKey = "${synced.songTitle}|${synced.artist}|${synced.durationMs}"
                            val staleWinRtPosition =
                                synced.isPlaying &&
                                    trackKey == lastWinRtTrackKey &&
                                    lastWinRtPositionMs != null &&
                                    synced.positionMs <= (lastWinRtPositionMs ?: -1L)

                            if (staleWinRtPosition && current.isPlaying && current.durationMs > 0L) {
                                val elapsed = (now - lastSyncAtMs).coerceAtLeast(0L)
                                val localAdvanced = (current.positionMs + elapsed).coerceAtMost(current.durationMs)
                                _state.value = current.copy(
                                    songTitle = synced.songTitle,
                                    artist = synced.artist,
                                    isPlaying = synced.isPlaying,
                                    durationMs = synced.durationMs,
                                    positionMs = localAdvanced,
                                    coverArtBytes = synced.coverArtBytes,
                                    paletteSeed = synced.paletteSeed
                                )
                            } else {
                                _state.value = synced
                                lastWinRtPositionMs = synced.positionMs
                                lastWinRtTrackKey = trackKey
                            }
                            lastSyncAtMs = now
                        }
                    } else if (current.isPlaying && current.durationMs > 0L) {
                        val elapsed = (now - lastSyncAtMs).coerceAtLeast(0L)
                        if (elapsed > 0L) {
                            val advanced = (current.positionMs + elapsed).coerceAtMost(current.durationMs)
                            _state.value = current.copy(positionMs = advanced)
                            lastSyncAtMs = now
                        }
                    }
                } catch (_: Throwable) {
                    // Keep polling even if one WinRT read fails.
                }
                delay(50L)
            }
        }
    }

    fun clear() {
        syncJob?.cancel()
        syncJob = null
        scope.coroutineContext[Job]?.cancel()
    }
}
