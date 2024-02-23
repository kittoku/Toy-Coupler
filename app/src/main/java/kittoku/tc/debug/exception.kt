package kittoku.tc.debug


internal class ParsingDataUnitException : Exception("Failed to parse data unit")

internal fun assertAlways(value: Boolean, message: Any? = null) {
    if (!value) {
        throw AssertionError(message.toString())
    }
}
