package binpack.greedy

import algorithms.greedy.GreedyPackStrategy
import binpack.BinPackSolution
import binpack.Box
import binpack.ContainerSolution
import binpack.PlacedBox

abstract class GenericBinPacker(private val highEffort: Boolean, private val lowerBound: Int = 0) : GreedyPackStrategy<Box, Int, BinPackSolution> {
    protected var size = 0
    private var containerIndex = 0
    private val containers = mutableListOf<Container>()
    private val availableContainers = mutableListOf<Container>()

    override fun init(constraints: Int) {
        reset()
        size = constraints
    }

    override fun initialSolution(): ContainerSolution {
        reset()
        return ContainerSolution(size, containers)
    }

    override fun packItem(item: Box): BinPackSolution = if(highEffort) packItemBestFit(item) else packItemFirstFit(item)

    private fun packItemFirstFit(item: Box): ContainerSolution {
        var placed: PlacedBox? = null
        var ci = 0
        var i = 0

        while(placed == null && i < availableContainers.size) {
            val tryContainer = availableContainers[i]
            placed = packIntoContainer(item, tryContainer)?.first
            ci = tryContainer.ci
            i += 1
        }

        // Start new container if no fit found
        if(placed == null) {
            ci = containers.size
            val container = Container(ci, size)
            containers.add(container)
            availableContainers.add(container)
            placed = packIntoContainer(item, containers[ci])!!.first
        }

        containers[ci].add(placed)

        if(!containers[ci].hasAccessibleSpace) {
            console.log("Container $ci is full")
            availableContainers.remove(containers[ci])
        }

        return ContainerSolution(size, containers)
    }

    private fun packItemBestFit(item: Box): ContainerSolution {
        val placed = availableContainers.map { Pair(packIntoContainer(item, it), it.ci) }.maxByOrNull { it.first?.second ?: Double.NEGATIVE_INFINITY }
        val ci: Int

        // Start new container if no fit found
        if(placed?.first == null) {
            val container = Container(containers.size, size)
            ci = containers.size
            containers.add(container)
            availableContainers.add(container)
            container.add(packIntoContainer(item, container)!!.first)
        }
        else {
            ci = placed.second
            containers[ci].add(placed.first!!.first)
        }

        if(!containers[ci].hasAccessibleSpace) {
            console.log("Container $ci is full")
            availableContainers.remove(containers[ci])
        }

        return ContainerSolution(size, containers)
    }

    abstract fun packIntoContainer(item: Box, container: Container): Pair<PlacedBox,Double>?

    open fun reset() {
        containers.clear()
        availableContainers.clear()
        containerIndex = 0

        // Initialize number of containers equal to lower bound
        (0 until lowerBound).forEach {
            val c = Container(it, size)
            containers.add(c)
            availableContainers.add(c)
        }
    }

    fun getSolution(): ContainerSolution {
        return ContainerSolution(size, containers)
    }
}

class Container(
    val ci: Int,
    val size: Int,
    val boxes: MutableList<PlacedBox> = mutableListOf(),
    val segmentsX: MutableList<Segment> = mutableListOf(Segment(0,0)),
    val segmentsY: MutableList<Segment> = mutableListOf(Segment(0,0)),
) {
    val hasAccessibleSpace: Boolean
        get() = segmentsX.any { it.value < size && it.start < size }

    val freeSpace: Int
        get() = size * size - boxes.sumOf { it.area }

    val accessibleSpace: Int
        get() = segmentsX.mapIndexed { idx, seg ->
                val width = size - seg.value
                val height = if(idx == segmentsX.size-1) size - seg.start else segmentsX[idx+1].start - seg.start
                width * height
            }.sum()

    fun clone() = Container(ci, size, boxes.toMutableList(), segmentsX.toMutableList(), segmentsY.toMutableList())

    fun add(box: PlacedBox) {
        boxes.add(box)

        //console.log(box.toString())
        //UIState.visualizer.debugBox(box,ci)

        updateSegments(segmentsX, box.y, box.h, box.x, box.w)
        updateSegments(segmentsY, box.x, box.w, box.y, box.h)

        /*UIState.visualizer.debugClear()
        segmentsX.forEachIndexed { index, segment ->
            val end = segmentsX.getOrNull(index + 1)?.start ?: size
            UIState.visualizer.debugLine(segment.value, segment.start, segment.value, end, ci)
        }
        segmentsY.forEachIndexed { index, segment ->
            val end = segmentsY.getOrNull(index + 1)?.start ?: size
            UIState.visualizer.debugLine(segment.start, segment.value, end, segment.value, ci)
        }*/
    }

    fun getRelevantSegments(segs: List<Segment>, boxStart: Int, boxMeasurement: Int): List<Segment> {
        val boxEnd = boxStart + boxMeasurement
        val lastSegmentIndex = normalizeBinarySearchIndex(segs.binarySearchBy(boxEnd - 1){ it.start })
        val firstSegmentIndex = normalizeBinarySearchIndex(segs.binarySearchBy(boxStart){ it.start })
        return segs.subList(firstSegmentIndex, lastSegmentIndex + 1)
    }

    fun swap(a: Int, b: Int) {
        val tmp = boxes[a]
        boxes[a] = boxes[b]
        boxes[b] = tmp
    }

    private fun updateSegments(segs: MutableList<Segment>, boxStart: Int, boxMeasurement: Int, value: Int, boxValue: Int) {
        val boxEnd = boxStart + boxMeasurement
        val lastSegmentIndex = normalizeBinarySearchIndex(segs.binarySearchBy(boxEnd){ it.start })
        val lastUsedSegment = segs[lastSegmentIndex]

        val segStart = getRelevantSegments(segs, boxStart, boxMeasurement).firstOrNull { it.value <= value }?.start
            ?: return

        segs.removeAll { it.start in boxStart until boxEnd }
        val insertIndex = -(segs.binarySearchBy(boxStart){ it.start } + 1)
        segs.add(insertIndex, Segment(segStart, value + boxValue))

        if(lastUsedSegment.start < boxEnd)
            segs.add(insertIndex+1, Segment(boxEnd, lastUsedSegment.value))
    }

    private fun normalizeBinarySearchIndex(index: Int) = if(index < 0) (-(index+1)-1) else index
}

data class Segment(val start: Int, val value: Int) {
    override fun toString() = "(s: $start, v: $value)"
}