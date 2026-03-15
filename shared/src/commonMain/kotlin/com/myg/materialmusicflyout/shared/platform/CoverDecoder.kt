package com.myg.materialmusicflyout.shared.platform

import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeCoverArt(bytes: ByteArray): ImageBitmap?

