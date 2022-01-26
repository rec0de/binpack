package binpack

interface Container {
    val boxes: Collection<PlacedBox>
    val size: Int
    var ci: Int
    val hasAccessibleSpace: Boolean
    val freeSpace: Int

    val area: Int
        get() = size * size

    fun add(box: PlacedBox)
    fun clone(): Container
}