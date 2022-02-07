package binpack

open class SpaceContainer(
    override var ci: Int,
    override val size: Int,
    override val boxes: MutableList<PlacedBox> = mutableListOf(),
    val spaces: MutableList<PlacedBox> = mutableListOf(PlacedBox(size, size, 0, 0))
) : Container {
    override val hasAccessibleSpace: Boolean
        get() = spaces.isNotEmpty()

    override val freeSpace: Int
        get() = spaces.sumOf { it.area }

    override fun clone() = SpaceContainer(ci, size, boxes.toMutableList(), spaces.toMutableList())

    override fun add(box: PlacedBox) {
        val space = spaces.firstOrNull { it.intersects(box) }!!
        add(box, space)
    }

    open fun add(box: Box, space: PlacedBox) {
        var box = box
        if(!space.fits(box) && space.fits(box.rotate()))
            box = box.rotate()
        else if(!space.fits(box))
            throw Exception("Space $space does not fit box $box")

        val placed = box.asPlaced(space.x, space.y)
        boxes.add(placed)
        spaces.remove(space)
        spaces.addAll(space.shatter(placed))
    }

    open fun remove(box: PlacedBox, overlapPossible: Boolean = false) {
        boxes.remove(box)
        if(overlapPossible) {
            val candidateSpaces = mutableListOf(box)
            val newSpaces = mutableListOf<PlacedBox>()

            while(candidateSpaces.isNotEmpty()) {
                val space = candidateSpaces.removeAt(0)
                val intersecting = boxes.firstOrNull { it.intersects(space) }
                if(intersecting == null)
                    newSpaces.add(space)
                else
                    candidateSpaces.addAll(space.shatter(intersecting))
            }
            spaces.addAll(newSpaces)
        }
        else
            spaces.add(box)
    }
}
