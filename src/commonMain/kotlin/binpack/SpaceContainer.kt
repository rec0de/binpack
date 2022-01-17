package binpack

class SpaceContainer(
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

    fun add(box: Box, space: PlacedBox) {
        var box = box
        if(!space.fits(box) && space.fits(box.rotate()))
            box = box.rotate()
        else if(!space.fits(box))
            throw Exception("Space $space does not fit box $box")

        boxes.add(box.asPlaced(space.x, space.y))
        spaces.remove(space)
        spaces.addAll(shatter(space, box))
    }

    fun remove(box: PlacedBox) {
        boxes.remove(box)
        spaces.add(box)
    }

    private fun shatter(space: PlacedBox, box: Box): List<PlacedBox> {
        val newSpaces = mutableListOf<PlacedBox>()
        if(space.w > box.w)
            newSpaces.add(PlacedBox(space.w - box.w, space.h, space.x + box.w, space.y))
        if(space.h > box.h)
            newSpaces.add(PlacedBox(box.w, space.h - box.h, space.x, space.y + box.h))
        return newSpaces
    }
}