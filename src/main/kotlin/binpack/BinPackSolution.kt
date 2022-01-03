package binpack

import binpack.greedy.Container
import kotlin.math.ceil
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

    fun asSequenceSolution(insertionSequence: List<Box>) = SequenceSolution(containerSize, insertionSequence, containers)
}

class SequenceSolution(containerSize: Int, val insertionSequence: List<Box>, containers: List<Collection<PlacedBox>>) : BinPackSolution(containerSize, containers) {

}

class ContainerSolution(containerSize: Int, val containerObjs: List<Container>) : BinPackSolution(containerSize, containerObjs.map{ it.boxes })