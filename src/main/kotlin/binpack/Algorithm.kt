package binpack

abstract class Algorithm {
    abstract val name: String
    abstract fun optimize(): BinPackSolution
    abstract fun optimizeStep(limit: Int): Pair<BinPackSolution,Boolean>
    abstract fun init(instance: BinPackProblem)
}