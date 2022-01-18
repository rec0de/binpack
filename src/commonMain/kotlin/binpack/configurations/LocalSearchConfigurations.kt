package binpack.configurations

import algorithms.localsearch.LocalSearch
import binpack.*
import binpack.greedy.NormalPosCircTouchPacker
import binpack.localsearch.LSSMove
import binpack.localsearch.LocalSequenceStrategy
import binpack.localsearch.MSSMove
import binpack.localsearch.MaximalSpaceStrategy

object LocalSearchLocalSequence : Algorithm() {
    override val name = "LocalSearch-LocalSequence"
    override val shortName = "LS LocalSequence"
    private lateinit var localsearch: LocalSearch<BinPackProblem, ContainerSolution<SegmentContainer>, LSSMove>

    override fun optimize(): BinPackSolution {
        return localsearch.optimize()
    }

    override fun optimizeStep(limit: Int): Pair<ContainerSolution<SegmentContainer>, Boolean> {
        return localsearch.optimizeStep(limit)
    }

    override fun init(instance: BinPackProblem) {
        localsearch = LocalSearch(LocalSequenceStrategy(NormalPosCircTouchPacker), instance)
    }
}

object LocalSearchMaximalSpace : Algorithm() {
    override val name = "LocalSearch-MaxSpace"
    override val shortName = "LS MaxSpace"
    private lateinit var localsearch: LocalSearch<BinPackProblem, SpaceContainerSolution, MSSMove>

    override fun optimize(): BinPackSolution {
        return localsearch.optimize()
    }

    override fun optimizeStep(limit: Int): Pair<SpaceContainerSolution, Boolean> {
        return localsearch.optimizeStep(limit)
    }

    override fun init(instance: BinPackProblem) {
        localsearch = LocalSearch(MaximalSpaceStrategy(), instance)
    }
}