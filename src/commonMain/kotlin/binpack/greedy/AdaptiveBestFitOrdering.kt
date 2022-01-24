package binpack.greedy

import algorithms.greedy.GreedyOrderingStrategy
import binpack.Box
import binpack.ContainerSolution
import binpack.SpaceContainer

object AdaptiveBestFitOrdering : GreedyOrderingStrategy<Box, Int, ContainerSolution<SpaceContainer>> {
    private lateinit var boxes: MutableList<Box>

    override fun init(items: Collection<Box>, constraints: Int) {
        // sort by non-increasing area and shortest side, orient horizontally
        boxes = items.sortedWith(compareBy({ -it.area }, { -it.shortSide })).toMutableList()
    }

    override fun nextItem(solution: ContainerSolution<SpaceContainer>): Box? {
        var res: Box? = null
        var si = 0
        val spaces = solution.containerObjs.flatMap { it.spaces }

        while(res == null && boxes.isNotEmpty() && si < spaces.size) {
            val space = spaces[si]
            res = boxes.firstOrNull { space.fitsRotated(it) }
            si += 1
        }

        if(res == null && boxes.isNotEmpty())
            res = boxes.first()

        if(res != null)
            boxes.remove(res)

        return res
    }
}