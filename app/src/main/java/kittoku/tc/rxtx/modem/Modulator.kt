package kittoku.tc.rxtx.modem

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kittoku.tc.extension.toIntAsUByte
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.sin


private const val INTERVAL_PACKET = 50L

internal class Modulator {
    private val buffer = FloatBuffer.allocate(BUFFER_FRAME_SIZE)
    private val player = preparePlayer()
    private val tones: List<FloatArray> = FREQUENCIES.map { generateTone(it) }

    private fun preparePlayer(): AudioTrack {
        val attribute = AudioAttributes.Builder().let {
            it.setUsage(AudioAttributes.USAGE_MEDIA)
            it.build()
        }

        val format = AudioFormat.Builder().let {
            it.setSampleRate(SAMPLING_RATE)
            it.setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            it.setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            it.build()
        }

        return AudioTrack.Builder().let {
            it.setAudioAttributes(attribute)
            it.setAudioFormat(format)
            it.setBufferSizeInBytes(format.frameSizeInBytes * buffer.capacity())
            it.setTransferMode(AudioTrack.MODE_STATIC)
            it.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            it.build()
        }
    }

    private fun generateTone(freq: Int): FloatArray {
        val tone = FloatArray(TONE_FRAME_SIZE)

        (0 until TONE_FRAME_SIZE).forEach {
            tone[it] = sin(2.0 * PI * it * freq / SAMPLING_RATE).toFloat()
        }

        return tone
    }

    private fun addTones(pseudoBits: Int, bitLength: Int) {
        var mask = 1.shl(bitLength - 1)

        repeat(bitLength) {
            val symbol = pseudoBits and mask
            val tone = if (symbol == 0) tones[0] else tones[1]
            buffer.put(tone)

            mask = mask.shr(1)
        }
    }

    internal suspend fun transmitTones(packet: ByteBuffer) {
        buffer.clear()


        addTones(PREAMBLE, Byte.SIZE_BITS)
        addTones(packet.remaining(), Short.SIZE_BITS)

        (packet.position() until packet.limit()).forEach {
            addTones(packet.get(it).toIntAsUByte(), Byte.SIZE_BITS)
        }

        addTones(calcChecksum(packet), Byte.SIZE_BITS)

        packet.position(packet.limit())


        buffer.flip()
        player.write(buffer.array(), 0, buffer.limit(), AudioTrack.WRITE_BLOCKING)
        player.play()

        delay(INTERVAL_PACKET + (buffer.limit() * 1000 / SAMPLING_RATE))

        while (player.playbackHeadPosition < buffer.limit()) {
            delay(1)
        }

        player.stop()
    }

    internal fun release() {
        player.release()
    }
}
