actual object Logger {
    actual fun log(vararg o: Any?) = o.forEach { println(it.toString()) }
}