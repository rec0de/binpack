package binpack

class BinPackSolution(val containerSize: Int, val containers: List<Collection<PlacedBox>>) {
    val boxCount: Int
        get() = containers.sumOf { it.size }
}