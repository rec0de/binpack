package binpack.greedy

import algorithms.greedy.GreedyOrderingStrategy
import binpack.BinPackSolution
import binpack.Box

object OnlineOrdering : GreedyOrderingStrategy<Box,Int,BinPackSolution> {
    private lateinit var boxes: MutableList<Box>

    override fun init(items: Collection<Box>, constraints: Int) {
        // no sorting, boxes arrive in unmodified order
        boxes = items.toMutableList()
    }

    override fun nextItem(solution: BinPackSolution) = boxes.removeFirstOrNull()
}