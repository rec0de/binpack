package algorithms.greedy

interface GreedyPackStrategy<Item, in Constraints, out Solution> {
    fun init(constraints: Constraints)
    fun initialSolution(): Solution
    fun packItem(item: Item): Solution
}