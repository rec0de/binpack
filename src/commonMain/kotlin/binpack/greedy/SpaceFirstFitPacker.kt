package binpack.greedy

import binpack.Box
import binpack.PlacedBox
import binpack.SpaceContainer

object SpaceFirstFitPacker : GenericBinPacker<SpaceContainer>(false) {

    override fun packIntoContainer(item: Box, container: SpaceContainer): Pair<PlacedBox, Double>? {
        val space = container.spaces.firstOrNull { space -> space.fitsRotated(item) } ?: return null
        val box = if(space.fits(item)) item else item.rotate()
        return Pair(box.asPlaced(space.x, space.y), 0.0)
    }

    override fun createEmptyContainer(ci: Int, size: Int) = SpaceContainer(ci, size)
}