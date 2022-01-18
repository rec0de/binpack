package binpack

class SegmentContainer(
    override var ci: Int,
    override val size: Int,
    override val boxes: MutableList<PlacedBox> = mutableListOf(),
    val segmentsX: MutableList<Segment> = mutableListOf(Segment(0, 0)),
    val segmentsY: MutableList<Segment> = mutableListOf(Segment(0, 0)),
    val deepInsertionCandidates: MutableList<Pair<Int,Int>> = mutableListOf(),
    freeSpaceInit: Int = -1,
    accessibleSpaceInit: Int = -1
) : Container {

    override val hasAccessibleSpace: Boolean
        get() = segmentsX.any { it.value < size && it.start < size }

    override var freeSpace: Int = freeSpaceInit
        get() {
            if(field < 0)
                field = size * size - boxes.sumOf { it.area }
            return field
        }

    var accessibleSpace: Int = accessibleSpaceInit
        get() {
            if(field < 0)
                field = segmentsX.mapIndexed { idx, seg ->
                    val width = size - seg.value
                    val height = if(idx == segmentsX.size-1) size - seg.start else segmentsX[idx+1].start - seg.start
                    width * height
                }.sum()
            return field
        }

    override fun clone() = SegmentContainer(
        ci,
        size,
        boxes.toMutableList(),
        segmentsX.toMutableList(),
        segmentsY.toMutableList(),
        deepInsertionCandidates.toMutableList(),
        freeSpace,
        accessibleSpace
    )

    override fun add(box: PlacedBox) {
        val prevAccessible = accessibleSpace

        boxes.add(box)
        freeSpace -= box.area
        invalidateAccessible()

        updateSegments(segmentsX, box.y, box.h, box.x, box.w)
        updateSegments(segmentsY, box.x, box.w, box.y, box.h)

        val accessibleChange = prevAccessible - accessibleSpace
        if(accessibleChange - box.area >= size * size * 0.05)
            deepInsertionCandidates.add(Pair(boxes.size - 1, accessibleChange - box.area))
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

    private fun invalidateAccessible() {
        accessibleSpace = -1
    }

    private fun updateSegments(segs: MutableList<Segment>, boxStart: Int, boxMeasurement: Int, value: Int, boxValue: Int) {
        val boxEnd = boxStart + boxMeasurement
        val lastSegmentIndex = normalizeBinarySearchIndex(segs.binarySearchBy(boxEnd){ it.start })
        val lastUsedSegment = segs[lastSegmentIndex]

        val segStart = getRelevantSegments(segs, boxStart, boxMeasurement).firstOrNull { it.value <= value }?.start
            ?: return

        if(segStart >= boxEnd)
            return

        segs.removeAll { it.start in segStart until boxEnd }
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