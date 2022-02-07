package binpack

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class BinPackProblem(val containerSize: Int, val boxes: List<Box>) {
    val lowerBound = ceil(boxes.sumOf { it.area }.toDouble() / (containerSize * containerSize)).toInt()
}

open class Box(val w: Int, val h: Int) {
    val area: Int
        get() = w * h
    val circumference: Int
        get() = 2 * w + 2 * h
    val longSide: Int
        get() = max(w, h)
    val shortSide: Int
        get() = min(w, h)

    fun asPlaced(x: Int, y: Int) = PlacedBox(w, h, x, y)

    fun fitsRotated(box: Box) = (box.w <= w && box.h <= h) || (box.w <= h && box.h <= w)
    fun fits(box: Box) = box.w <= w && box.h <= h

    fun rotate() = Box(h, w)
    open fun clone() = Box(w, h)
    override fun toString() = "[$w x $h]"
}

class PlacedBox(w: Int, h: Int, val x: Int, val y: Int) : Box(w, h) {
    override fun clone() = PlacedBox(w, h, x, y)
    override fun toString() = "[${w}x$h @ ($x,$y)]"

    val endX: Int
        get() = x + w
    val endY: Int
        get() = y + h

    fun contains(box: PlacedBox) = (box.x >= x && box.y >= y && box.endY <= endY && endX <= endX)
    fun continuous(box: PlacedBox) = (box.x == x && box.w == w && (box.endY == y || box.y == endY)) || (box.y == y && box.h == h && (box.endX == x || box.x == endX))
    fun superBox(box: PlacedBox): PlacedBox {
        val sx = min(x, box.x)
        val sy = min(y, box.y)
        val w = max(endX, box.endX) - sx
        val h = max(endY, box.endY) - sy
        return PlacedBox(w, h, sx, sy)
    }

    fun shatter(box: PlacedBox): List<PlacedBox> {
        val fragments = mutableListOf<PlacedBox>()

        if(!intersects(box))
            throw Exception("Trying to shatter space with non-intersecting box")

        if (endX > box.endX)
            fragments.add(PlacedBox(endX - box.endX, h, box.endX, y))
        if (endY > box.endY)
            fragments.add(PlacedBox(min(w, box.endX - x), endY - box.endY, x, box.endY))
        if (x < box.x)
            fragments.add(PlacedBox(box.x - x, min(h, box.endY - y), x, y))
        if (y < box.y)
            fragments.add(PlacedBox(min(box.endX, endX) - max(box.x, x), box.y - y, max(box.x, x), y))

        //Logger.log("Shattered ${toString()} using $box into ${fragments.joinToString(", ")}")
        return fragments
    }

    fun outOfBounds(size: Int) = x < 0 || y < 0 || endX > size || endY > size
    fun intersects(box: PlacedBox) = !((endX <= box.x) || (box.endX <= x) || (endY <= box.y) || (box.endY <= y))

    fun intersection(box: PlacedBox) : Int {
        val xOverlap = max(0, min(endX, box.endX) - max(x, box.x));
        val yOverlap = max(0, min(endY, box.endY) - max(y, box.y));
        return xOverlap * yOverlap
    }
    fun relativeOverlap(box: PlacedBox) = intersection(box).toDouble() / max(area, box.area)


}