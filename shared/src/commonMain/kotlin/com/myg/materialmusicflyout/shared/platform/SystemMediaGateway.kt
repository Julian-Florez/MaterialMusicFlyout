package com.myg.materialmusicflyout.shared.platform

import com.myg.materialmusicflyout.shared.model.MusicState

interface SystemMediaGateway {
    suspend fun readState(previous: MusicState): MusicState?
    suspend fun togglePlayPause(): Boolean
    suspend fun nextTrack(): Boolean
    suspend fun previousTrack(): Boolean
}

expect fun createSystemMediaGateway(): SystemMediaGateway

