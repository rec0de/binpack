actual object Logger {
    actual fun log(vararg o: Any?) = console.log(*o)
}