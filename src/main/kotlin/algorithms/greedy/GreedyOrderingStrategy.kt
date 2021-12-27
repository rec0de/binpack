package algorithms.greedy

interface GreedyOrderingStrategy<Item, Constraints, Solution> {
    fun init(items: Collection<Item>, constraints: Constraints)
    fun nextItem(solution: Solution): Item?
}