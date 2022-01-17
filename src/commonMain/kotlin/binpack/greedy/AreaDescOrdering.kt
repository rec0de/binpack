package binpack.greedy

import algorithms.greedy.GreedyOrderingStrategy
import binpack.BinPackSolution
import binpack.Box

object AreaDescOrdering : GreedyOrderingStrategy<Box,Int,BinPackSolution> {
    private lateinit var boxes: MutableList<Box>

    override fun init(items: Collection<Box>, constraints: Int) {
        // sort by non-increasing area and shortest side, orient horizontally
        boxes = items.sortedWith(compareBy({ -it.area }, { -it.shortSide })).map{ if(it.h > it.w) it.rotate() else it }.toMutableList()
    }

    override fun nextItem(solution: BinPackSolution) = boxes.removeFirstOrNull()
}