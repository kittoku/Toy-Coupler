package kittoku.tc.extension

import java.nio.Buffer
import java.nio.DoubleBuffer
import java.nio.FloatBuffer


internal fun Buffer.move(diff: Int) {
    position(position() + diff)
}

internal fun Buffer.discard() {
    this.position(0)
    this.limit(0)
}

internal val Buffer.capacityAfterLimit: Int
    get() = this.capacity() - this.limit()

internal fun FloatBuffer.slide() {
    val remaining = this.remaining()

    this.array().also {
        it.copyInto(it, 0, this.position(), this.limit())
    }

    this.position(0)
    this.limit(remaining)
}

internal fun DoubleBuffer.slide() {
    val remaining = this.remaining()

    this.array().also {
        it.copyInto(it, 0, this.position(), this.limit())
    }

    this.position(0)
    this.limit(remaining)
}

internal fun DoubleBuffer.probe(diff: Int = 0): Double {
    return this.get(this.position() + diff)
}
