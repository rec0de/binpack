package binpack.greedy

import binpack.BinPackSolution
import binpack.Box

object NormalPosFirstFitPacker : GenericBinPacker() {
    private var segments = listOf<Pair<Int,Int>>()

    override fun packItem(item: Box): BinPackSolution {
        // place box at lowest height as far back as it fits

        var minX = segments.filter { it.second < item.h }.maxOf { it.first }
        var offset = 0
        var segmentIndex = 1

        // if it doesn't fit, try sliding up
        while(minX + item.w > size) {
            offset = segments[segmentIndex].second

            // Start new container if no fit was found
            if(offset + item.h > size) {
                segmentIndex = 0
                containerIndex++
                minX = 0
                offset = 0
                segments = listOf(Pair(0,0))
            }
            else {
                minX = segments.filter { it.second >= offset && it.second < item.h + offset }.maxOf { it.first }
                segmentIndex++
            }
        }

        // update segments
        val lastUsedSegment = segments.filter { it.second >= offset && it.second <= item.h + offset }.maxByOrNull { it.second }!!

        segments = listOf(Pair(minX + item.w, offset), Pair(lastUsedSegment.first, offset + item.h)) + segments.filter{ it.second < offset || it.second > item.h + offset }
        segments = segments.sortedBy { it.second }

        if(containers.size == containerIndex)
            containers.add(mutableListOf())

        containers[containerIndex].add(item.asPlaced(minX, offset))

        return BinPackSolution(size, containers)
    }

    override fun reset() {
        super.reset()
        segments = listOf(Pair(0,0))
    }
}