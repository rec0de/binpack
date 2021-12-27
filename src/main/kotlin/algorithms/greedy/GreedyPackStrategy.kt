package algorithms.greedy

interface GreedyPackStrategy<Item, Constraints, Solution> {
    fun init(constraints: Constraints)
    fun initialSolution(): Solution
    fun packItem(item: Item): Solution
}