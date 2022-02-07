package ui

import binpack.PlacedBox

// Just a stub to be able to make debug viz calls from common code
actual object UIState {
    actual var debugVisualizer: DebugVisualizer? = DummyViz
}

object DummyViz : DebugVisualizer {
    override fun debugClear() {}
    override fun debugBox(box: PlacedBox, ci: Int) {}
    override fun debugLine(sx: Int, sy: Int, ex: Int, ey: Int, ci: Int) {}
}