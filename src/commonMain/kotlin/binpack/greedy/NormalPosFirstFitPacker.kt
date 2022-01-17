package binpack.greedy

import binpack.Box
import binpack.SegmentContainer
import binpack.PlacedBox

object NormalPosFirstFitPacker : GenericBinPacker<SegmentContainer>(false) {

    override fun packIntoContainer(item: Box, container: SegmentContainer): Pair<PlacedBox, Double>? {
        val segments = container.segmentsX

        // place box at lowest height as far back as it fits
        var minX = segments.filter { it.start < item.h }.maxOf { it.value }
        var offset = 0
        var segmentIndex = 1

        // if it doesn't fit, try sliding up
        while(minX + item.w > size) {
            offset = segments[segmentIndex].start

            // Start new container if no fit was found
            if(offset + item.h > size)
                return null

            minX = segments.filter { it.start >= offset && it.start < item.h + offset }.maxOf { it.value }
            segmentIndex++
        }

        return Pair(item.asPlaced(minX, offset), 0.0)
    }

    override fun createEmptyContainer(ci: Int, size: Int) = SegmentContainer(ci, size)
}