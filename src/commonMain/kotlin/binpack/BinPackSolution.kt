package binpack

import kotlin.math.max

open class BinPackSolution(val containerSize: Int, val containers: List<Collection<PlacedBox>>) {
    val boxCount: Int
        get() = containers.sumOf { it.size }

    // compute average container utilization of the k-1 most densely packed containers
    fun k1PackDensity() : Double {
        val densities = containers.map { containerDensity(it) }
        val leastUsed = densities.minOrNull() ?: 0.0
        return (densities.sum() - leastUsed) / max(1, containers.size - 1)
    }

    // check that all boxes are in-bounds and don't intersect each other (touching is allowed)
    fun verify(): Boolean {
        containers.forEachIndexed { i, container ->
            val boxes = container.toMutableList()
            while(boxes.isNotEmpty()) {
                val box = boxes.removeFirst()

                if(box.outOfBounds(containerSize))
                    throw Exception("Box $box in container $i is out of bounds")
                if(boxes.any { box.intersects(it) }) {
                    val overlapping = boxes.first{ box.intersects(it) }
                    throw Exception("Boxes $box and $overlapping in container $i have overlap")
                }
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

class SpaceContainerSolution(containerSize: Int, val containerObjs: List<SpaceContainer>) : BinPackSolution(containerSize, containerObjs.map{ it.boxes })
class ContainerSolution<C : Container>(containerSize: Int, val containerObjs: List<C>) : BinPackSolution(containerSize, containerObjs.map{ it.boxes })