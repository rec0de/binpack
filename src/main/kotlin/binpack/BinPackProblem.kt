package binpack

import kotlin.math.max
import kotlin.math.min

data class BinPackProblem(val containerSize: Int, val boxes: List<Box>)

open class Box(val w: Int, val h: Int) {
    val area: Int
        get() = w * h
    val longSide: Int
        get() = max(w, h)
    val shortSide: Int
        get() = min(w, h)

    fun asPlaced(x: Int, y: Int) = PlacedBox(w, h, x, y)

    fun rotate() = Box(h, w)
    open fun clone() = Box(w, h)
    override fun toString() = "[$w x $h]"
}

class PlacedBox(w: Int, h: Int, val x: Int, val y: Int) : Box(w, h) {
    override fun clone() = PlacedBox(w, h, x, y)
    override fun toString() = "[${w}x$h @ ($x,$y)]"

    fun intersects(box: PlacedBox) = !((x+w <= box.x) || (box.x+box.w <= x) || (y+h <= box.y) || (box.y+box.h <= y))
    fun outOfBounds(size: Int) = x < 0 || y < 0 || x+w > size || y+h > size
}