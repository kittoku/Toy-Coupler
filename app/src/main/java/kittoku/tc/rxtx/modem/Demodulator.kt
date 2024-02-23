package kittoku.tc.rxtx.modem

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import kittoku.tc.DEFAULT_MRU
import kittoku.tc.SharedBridge
import kittoku.tc.control.SignalObserver
import kittoku.tc.debug.assertAlways
import kittoku.tc.extension.capacityAfterLimit
import kittoku.tc.extension.discard
import kittoku.tc.extension.move
import kittoku.tc.extension.probe
import kittoku.tc.extension.slide
import kittoku.tc.extension.toIntAsUByte
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import java.nio.FloatBuffer
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow


private val DFT_DECIBEL_OFFSET = 20.0 * (log10(2.0) - log10(TONE_FRAME_SIZE.toDouble()))
private val DEVICE_DECIBEL_OFFSET = 90.0 - 20.0 * log10(2500.0 / Short.MAX_VALUE)
private val DECIBEL_OFFSET = DFT_DECIBEL_OFFSET + DEVICE_DECIBEL_OFFSET

internal class Demodulator(private val bridge: SharedBridge) {
    private val observer = SignalObserver(bridge)
    private val audioRecord: AudioRecord

    private val frameBuffer = FloatBuffer.allocate(TONE_FRAME_SIZE * 2).also { it.discard() }
    private val levelBuffer = DoubleBuffer.allocate(TONE_FRAME_SIZE * (Byte.SIZE_BITS + 1)).also { it.discard() }
    private val discardArray = FloatArray(1000)

    private val levelPool = DoubleArray(10) { Double.NEGATIVE_INFINITY } // for 0.1 s
    private var poolIndex = 0

    init {
        if (bridge.service.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw Error("NO PERMISSION")
        }

        audioRecord = AudioRecord(
            AudioSource.MIC,
            SAMPLING_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
            BUFFER_FRAME_SIZE * Float.SIZE_BYTES
        )
    }

    internal fun startRecord() {
        audioRecord.startRecording()
    }

    internal fun discardFrames() {
        while (true) {
            val numRead = audioRecord.read(
                discardArray,
                0,
                discardArray.size,
                AudioRecord.READ_NON_BLOCKING
            )

            assertAlways(numRead >= 0, numRead)

            if (numRead < discardArray.size) {
                break
            }
        }

        frameBuffer.discard()
        levelBuffer.discard()
    }

    private fun poolMaxLevel(level: Double) {
        levelPool[poolIndex] = level

        poolIndex += 1
        if (poolIndex == levelPool.size) {
            poolIndex = 0

            observer.updateLevel(levelPool.max())
        }
    }

    private fun appendFrames() {
        frameBuffer.slide()

        val numRead = audioRecord.read(
            frameBuffer.array(),
            frameBuffer.limit(),
            frameBuffer.capacityAfterLimit,
            AudioRecord.READ_BLOCKING
        )

        assertAlways(numRead >= 0, numRead)

        frameBuffer.limit(frameBuffer.limit() + numRead)
    }

    private fun appendLevels() {
        appendFrames()

        if (levelBuffer.capacityAfterLimit < TONE_FRAME_SIZE) {
            levelBuffer.slide()
        }

        var i = levelBuffer.limit()
        levelBuffer.limit(i + TONE_FRAME_SIZE)

        var maxLeveL = Double.NEGATIVE_INFINITY

        repeat(TONE_FRAME_SIZE) {
            val level = doDFT()
            levelBuffer.put(i, level)

            maxLeveL = max(level, maxLeveL)

            i += 1
        }

        poolMaxLevel(maxLeveL)
    }

    private fun requireLevels(numRequired: Int) {
        val numLacked = numRequired - levelBuffer.remaining()
        if (numLacked <= 0) return

        while (levelBuffer.remaining() < numRequired) {
            appendLevels()
        }
    }

    private fun detectSignal(): Boolean {
        requireLevels(1)

        if (levelBuffer.probe() > bridge.thresholdLevel) {
            observer.updateLastDetected()
            return true
        }

        levelBuffer.move(1)
        return false
    }

    private fun syncPeak() {
        requireLevels(TONE_FRAME_SIZE)

        var peakLevel = Double.NEGATIVE_INFINITY
        var peakPosition = levelBuffer.position()

        repeat(TONE_FRAME_SIZE) {
            val level = levelBuffer.probe()

            if (level > peakLevel) {
                peakLevel = level
                peakPosition = levelBuffer.position()
            }

            levelBuffer.move(1)
        }

        levelBuffer.position(peakPosition)
    }

    private fun expectPreamble(): Boolean {
        requireLevels(TONE_FRAME_SIZE * Byte.SIZE_BITS)

        var mask = 0b1000_0000

        repeat(Byte.SIZE_BITS) {
            val expected = if (PREAMBLE and mask == 0) 0 else 1
            val actual = if (levelBuffer.probe() > bridge.thresholdLevel) 1 else 0

            if (actual != expected) {
                return false
            }

            mask = mask.shr(1)

            levelBuffer.move(TONE_FRAME_SIZE)
        }

        return true
    }

    private fun retrieveByte(): Byte {
        requireLevels(TONE_FRAME_SIZE * Byte.SIZE_BITS)

        var pseudoByte = 0

        repeat(Byte.SIZE_BITS) {
            pseudoByte = pseudoByte.shl(1)

            val bit = if (levelBuffer.probe() > bridge.thresholdLevel) 1 else 0

            pseudoByte = bit or pseudoByte

            levelBuffer.move(TONE_FRAME_SIZE)
        }

        return pseudoByte.toByte()
    }

    internal fun retrievePacket(): ByteBuffer? {
        if (!detectSignal()) {
            return null
        }

        syncPeak()

        if (!expectPreamble()) {
            return null
        }

        val moreSignificantByte = retrieveByte().toIntAsUByte()
        val lessSignificantByte = retrieveByte().toIntAsUByte()

        val packetLength = moreSignificantByte.shl(Byte.SIZE_BITS) or lessSignificantByte
        if (packetLength > DEFAULT_MRU) {
            return null
        }

        val buffer = ByteBuffer.allocate(packetLength)

        repeat(packetLength) {
            buffer.put(retrieveByte())
        }

        buffer.flip()

        val expectedChecksum = retrieveByte().toIntAsUByte()
        val actualChecksum = calcChecksum(buffer)

        return if (expectedChecksum == actualChecksum) {
            observer.updateLastReceived(buffer, true)
            buffer
        } else {
            observer.updateLastReceived(buffer, false)
            null
        }
    }

    private fun doDFT(): Double {
        // Goertzel algorithm, simplified for 1000 Hz
        val startPosition = frameBuffer.position()

        var phase0 = 0.0
        var phase1 = 0.0

        repeat(TONE_FRAME_SIZE) {
            val reminder = it % 4
            val x = frameBuffer.get().toDouble()

            when (reminder) {
                0 -> phase0 += x
                1 -> phase1 += x
                2 -> phase0 -= x
                3 -> phase1 -= x
            }
        }

        frameBuffer.position(startPosition + 1)

        val power = phase0.pow(2) + phase1.pow(2)

        return 10.0 * log10(power) + DECIBEL_OFFSET // dB SPL
    }

    internal fun release() {
        audioRecord.stop()
        audioRecord.release()
        observer.close()
    }
}
