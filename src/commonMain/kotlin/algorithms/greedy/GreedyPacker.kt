package algorithms.greedy

class GreedyPacker<Item, Constraints, Solution>(
    private val order: GreedyOrderingStrategy<Item, Constraints, Solution>,
    private val packer: GreedyPackStrategy<Item, Constraints, Solution>,
    constraints: Constraints,
    items: Collection<Item>
) {

    private var solution: Solution
    private var item: Item? = null

    init {
        order.init(items, constraints)
        packer.init(constraints)
        solution = packer.initialSolution()
        item = order.nextItem(solution)
    }

    fun optimize(): Solution {
        return optimizeStep(Int.MAX_VALUE).first
    }

    fun optimizeStep(stepLimit: Int): Pair<Solution,Boolean> {
        var steps = 0

        while(steps < stepLimit && item != null) {
            solution = packer.packItem(item!!)
            item = order.nextItem(solution)
            steps++
        }

        return Pair(solution, item == null)
    }
}