package binpack

import kotlin.math.ceil
import kotlin.math.max

class BinPackSolution(val containerSize: Int, val containers: List<Collection<PlacedBox>>) {
    val boxCount: Int
        get() = containers.sumOf { it.size }

    fun lowerBound() : Int {
        val boxArea = containers.sumOf { c -> c.sumOf { box -> box.area } }
        return ceil(boxArea.toDouble() / (containerSize * containerSize)).toInt()
    }

    // compute average container utilization of the k-1 most densely packed containers
    fun k1PackDensity() : Double {
        val densities = containers.map { containerDensity(it) }
        val leastUsed = densities.minOrNull() ?: 0.0
        return (densities.sum() - leastUsed) / max(1, containers.size - 1)
    }

    // check that all boxes are in-bounds and don't intersect each other (touching is allowed)
    fun verify(): Boolean {
        containers.forEach { container ->
            val boxes = container.toMutableList()
            while(boxes.isNotEmpty()) {
                val box = boxes.removeFirst()
                if(box.outOfBounds(containerSize) || boxes.any { box.intersects(it) })
                    return false
            }
        }

        return true
    }

    private fun containerDensity(container: Collection<PlacedBox>) : Double {
        val available = containerSize * containerSize
        val used = container.sumOf { box -> box.w * box.h }
        return used.toDouble() / available
    }
}