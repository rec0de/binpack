package binpack

interface Container {
    val boxes: Collection<PlacedBox>
    val size: Int
    var ci: Int
    val hasAccessibleSpace: Boolean
    val freeSpace: Int

    fun add(placedBox: PlacedBox)
    fun clone(): Container
}