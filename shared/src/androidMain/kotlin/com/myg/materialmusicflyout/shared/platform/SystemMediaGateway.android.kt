package com.myg.materialmusicflyout.shared.platform

import com.myg.materialmusicflyout.shared.model.MusicState

actual fun createSystemMediaGateway(): SystemMediaGateway = NoOpSystemMediaGateway

private object NoOpSystemMediaGateway : SystemMediaGateway {
    override suspend fun readState(previous: MusicState): MusicState? = null

    override suspend fun togglePlayPause(): Boolean = false

    override suspend fun nextTrack(): Boolean = false

    override suspend fun previousTrack(): Boolean = false
}

