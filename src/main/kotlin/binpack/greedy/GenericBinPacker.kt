package binpack.greedy

import algorithms.greedy.GreedyPackStrategy
import binpack.BinPackSolution
import binpack.Box
import binpack.PlacedBox
import ui.UIState

abstract class GenericBinPacker : GreedyPackStrategy<Box, Int, BinPackSolution> {
    protected var size = 0
    private var containerIndex = 0
    private val containers = mutableListOf<Container>()
    private val availableContainers = mutableListOf<Container>()

    override fun init(constraints: Int) {
        reset()
        size = constraints
    }

    override fun initialSolution(): BinPackSolution {
        reset()
        return BinPackSolution(size, containers.map { it.boxes })
    }

    override fun packItem(item: Box): BinPackSolution {
        var placed: PlacedBox? = null
        var ci = 0
        var i = 0

        while(placed == null && i < availableContainers.size) {
            val tryContainer = availableContainers[i]
            placed = packIntoContainer(item, tryContainer)
            ci = tryContainer.ci
            i += 1
        }

        // Start new container if no fit found
        if(placed == null) {
            ci = containers.size
            val container = Container(ci, size)
            containers.add(container)
            availableContainers.add(container)
            placed = packIntoContainer(item, containers[ci])!!
        }

        containers[ci].add(placed)

        if(!containers[ci].hasAccessibleSpace) {
            console.log("Container $ci is full")
            availableContainers.remove(containers[ci])
        }

        return BinPackSolution(size, containers.map{ it.boxes })
    }

    abstract fun packIntoContainer(item: Box, container: Container): PlacedBox?

    open fun reset() {
        containers.clear()
        availableContainers.clear()
        containerIndex = 0
    }

    fun getSolution(): BinPackSolution {
        return BinPackSolution(size, containers.map { it.boxes })
    }
}

class Container(val ci: Int, val size: Int) {
    val boxes = mutableListOf<PlacedBox>()
    val segmentsX = mutableListOf(Segment(0,0))
    val segmentsY = mutableListOf(Segment(0,0))

    val hasAccessibleSpace: Boolean
        get() = segmentsX.any { it.value < size && it.start < size }

    fun add(box: PlacedBox) {
        boxes.add(box)
        console.log(box.toString())

        UIState.visualizer.debugBox(box,ci)

        updateSegments(segmentsX, box.y, box.h, box.x, box.w)
        updateSegments(segmentsY, box.x, box.w, box.y, box.h)

        UIState.visualizer.debugClear()
        segmentsX.forEachIndexed { index, segment ->
            val end = segmentsX.getOrNull(index + 1)?.start ?: size
            UIState.visualizer.debugLine(segment.value, segment.start, segment.value, end, ci)
        }
        segmentsY.forEachIndexed { index, segment ->
            val end = segmentsY.getOrNull(index + 1)?.start ?: size
            UIState.visualizer.debugLine(segment.start, segment.value, end, segment.value, ci)
        }
    }

    fun getRelevantSegments(segs: List<Segment>, boxStart: Int, boxMeasurement: Int): List<Segment> {
        val boxEnd = boxStart + boxMeasurement
        val lastSegmentIndex = normalizeBinarySearchIndex(segs.binarySearchBy(boxEnd - 1){ it.start })
        val firstSegmentIndex = normalizeBinarySearchIndex(segs.binarySearchBy(boxStart){ it.start })
        return segs.subList(firstSegmentIndex, lastSegmentIndex + 1)
    }

    private fun updateSegments(segs: MutableList<Segment>, boxStart: Int, boxMeasurement: Int, value: Int, boxValue: Int) {
        val boxEnd = boxStart + boxMeasurement
        val lastSegmentIndex = normalizeBinarySearchIndex(segs.binarySearchBy(boxEnd){ it.start })
        val lastUsedSegment = segs[lastSegmentIndex]

        val segStart = getRelevantSegments(segs, boxStart, boxMeasurement).firstOrNull { it.value <= value }?.start
            ?: return

        segs.removeAll { it.start in boxStart until boxEnd }
        val insertIndex = -(segs.binarySearchBy(segStart){ it.start } + 1)
        segs.add(insertIndex, Segment(segStart, value + boxValue))

        if(lastUsedSegment.start < boxEnd)
            segs.add(insertIndex+1, Segment(boxEnd, lastUsedSegment.value))
    }

    private fun normalizeBinarySearchIndex(index: Int) = if(index < 0) (-(index+1)-1) else index
}

data class Segment(val start: Int, val value: Int) {
    override fun toString() = "(s: $start, v: $value)"
}