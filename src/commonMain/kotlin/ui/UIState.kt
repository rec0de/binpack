package ui

import binpack.PlacedBox

expect object UIState {
    var debugVisualizer: DebugVisualizer?
}

interface DebugVisualizer {
    fun debugClear()
    fun debugBox(box: PlacedBox, ci: Int)
    fun debugLine(sx: Int, sy: Int, ex: Int, ey: Int, ci: Int)
}