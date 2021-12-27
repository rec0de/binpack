package algorithms.greedy

class GreedyPacker<Item, Constraints, Solution>(
    private val order: GreedyOrderingStrategy<Item, Constraints, Solution>,
    private val packer: GreedyPackStrategy<Item, Constraints, Solution>,
    private val constraints: Constraints,
    items: Collection<Item>
) {

    private var solution: Solution

    init {
        order.init(items, constraints)
        packer.init(constraints)
        solution = packer.initialSolution()
    }

    fun optimize(): Solution {
        return optimizeStep(Int.MAX_VALUE)
    }

    fun optimizeStep(stepLimit: Int): Solution {
        var steps = 0
        var item: Item? = order.nextItem(solution)

        while(steps < stepLimit && item != null) {
            solution = packer.packItem(item)
            item = order.nextItem(solution)
            steps++
        }

        return solution
    }
}