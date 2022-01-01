package binpack.greedy

import binpack.BinPackProblem
import binpack.Box
import binpack.PlacedBox
import ui.UIState
import kotlin.math.min

object NormalPosCircTouchPacker: AbstractNormalPosCircTouchPacker(false, 0)

class GoldStandardPacker(instance: BinPackProblem): AbstractNormalPosCircTouchPacker(true, instance.lowerBound)

abstract class AbstractNormalPosCircTouchPacker(highEffort: Boolean, lowerBound: Int) : GenericBinPacker(highEffort, lowerBound) {

    override fun packIntoContainer(item: Box, container: Container): Pair<PlacedBox, Double>? {
        var box = item

        // Try both rotation options and use best resulting fit
        var bestFit = findBestFit(box, container)
        val bestFitRotated = findBestFit(box.rotate(), container)

        if(bestFitRotated != null && (bestFit == null || bestFit.third < bestFitRotated.third)) {
            bestFit = bestFitRotated
            box = box.rotate()
        }

        // No fit found
        if(bestFit == null)
            return null

        //console.log("Found fit for box $box, total circumference: ${box.circumference}, touched: ${bestFit.third}")

        return Pair(box.asPlaced(bestFit.first, bestFit.second), bestFit.third)
    }

    private fun findBestFit(box: Box, container: Container) : Triple<Int, Int, Double>? {
        return container.segmentsX.mapIndexedNotNull { index, startSeg ->
            var y = startSeg.start
            val prevX = container.segmentsX.getOrNull(index-1)?.value ?: size
            val x = container.getRelevantSegments(container.segmentsX, y, box.h).maxOf { it.value }

            val yCor = container.getRelevantSegments(container.segmentsY, x, box.w).maxOf { it.value }
            if(yCor < y && x < prevX && x + box.w <= size && y + box.h <= size)
                y = yCor

            if(x >= prevX || x + box.w > size || y + box.h > size)
                null
            else
                Triple(x, y, evaluateFit(box, container, x, y))
        }.maxByOrNull { it.third }
    }

    private fun evaluateFit(box: Box, container: Container, x: Int, y: Int) : Double {
        var containerTouch = 0
        var boxTouch = 0

        // Calculate edge overlap with bottom and right container border
        if(y + box.h == size)
            containerTouch += box.w
        /*if(x + box.w == size)
            containerTouch += box.h*/

        // Calculate edge overlap with surrounding boxes (only considers bottom and left edges)
        // also inexact because segments are more of an approximation than actual reflection of the boxes
        container.segmentsX.forEachIndexed { index, segment ->
            if(segment.start >= y && segment.start < y + box.h && segment.value == x) {
                val startY = segment.start
                val endY = min(container.segmentsX.getOrNull(index+1)?.start ?: size , y + box.h)
                boxTouch += endY - startY
            }
        }

        container.segmentsY.forEachIndexed { index, segment ->
            if(segment.start >= x && segment.start < x + box.w && segment.value == y) {
                val startX = segment.start
                val endX = min(container.segmentsY.getOrNull(index+1)?.start ?: size , x + box.w)
                boxTouch += endX - startX
            }
        }

        //console.log("Considering position: $x $y, touch: ${containerTouch + boxTouch}")
        return containerTouch + boxTouch + (size - x).toDouble() / size // all things being equal, prefer fits that sit further back in the container
    }
}