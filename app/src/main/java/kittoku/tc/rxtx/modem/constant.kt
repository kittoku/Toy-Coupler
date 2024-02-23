package kittoku.tc.rxtx.modem

import kittoku.tc.DEFAULT_MTU


internal const val SAMPLING_RATE = 4000
internal const val SYMBOL_RATE = 100

internal const val TONE_FRAME_SIZE = SAMPLING_RATE / SYMBOL_RATE
internal const val BUFFER_FRAME_SIZE = (1 + 2 + DEFAULT_MTU + 1) * Byte.SIZE_BITS * TONE_FRAME_SIZE

internal const val PREAMBLE = 0b10101011

val FREQUENCIES = listOf(500, 1000)
