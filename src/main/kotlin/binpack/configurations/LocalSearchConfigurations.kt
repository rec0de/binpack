package binpack.configurations

import algorithms.localsearch.LocalSearch
import binpack.*
import binpack.greedy.NormalPosCircTouchPacker
import binpack.greedy.NormalPosFirstFitPacker
import binpack.localsearch.LSSMove
import binpack.localsearch.LocalSequenceStrategy
import binpack.localsearch.SequenceBasedStrategy
import binpack.localsearch.SwapMove

object LocalSearchCircTouch : Algorithm() {
    override val name = "LocalSearch-NormalPosCircTouch"
    private lateinit var localsearch: LocalSearch<BinPackProblem, SequenceSolution, SwapMove>

    override fun optimize(): BinPackSolution {
        return localsearch.optimize()
    }

    override fun optimizeStep(limit: Int): Pair<BinPackSolution, Boolean> {
        return localsearch.optimizeStep(limit)
    }

    override fun init(instance: BinPackProblem) {
        localsearch = LocalSearch(SequenceBasedStrategy(NormalPosCircTouchPacker), instance)
    }
}

object LocalSearchFirstFit : Algorithm() {
    override val name = "LocalSearch-NormalPosFirstFit"
    private lateinit var localsearch: LocalSearch<BinPackProblem, SequenceSolution, SwapMove>

    override fun optimize(): BinPackSolution {
        return localsearch.optimize()
    }

    override fun optimizeStep(limit: Int): Pair<BinPackSolution, Boolean> {
        return localsearch.optimizeStep(limit)
    }

    override fun init(instance: BinPackProblem) {
        localsearch = LocalSearch(SequenceBasedStrategy(NormalPosFirstFitPacker), instance)
    }
}

object LocalSearchLocalSequence : Algorithm() {
    override val name = "LocalSearch-LocalSequence"
    private lateinit var localsearch: LocalSearch<BinPackProblem, ContainerSolution, LSSMove>

    override fun optimize(): BinPackSolution {
        return localsearch.optimize()
    }

    override fun optimizeStep(limit: Int): Pair<ContainerSolution, Boolean> {
        return localsearch.optimizeStep(limit)
    }

    override fun init(instance: BinPackProblem) {
        localsearch = LocalSearch(LocalSequenceStrategy(NormalPosCircTouchPacker), instance)
    }
}