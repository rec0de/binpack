package binpack.configurations

import algorithms.localsearch.LocalSearch
import binpack.*
import binpack.greedy.NormalPosCircTouchPacker
import binpack.localsearch.*

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

object LocalSearchRepackSpace : Algorithm() {
    override val name = "LocalSearch-RepackSpace"
    override val shortName = "LS RepackSpace"
    private lateinit var localsearch: LocalSearch<BinPackProblem, SpaceContainerSolution, MSSMove>

    override fun optimize(): BinPackSolution {
        return localsearch.optimize()
    }

    override fun optimizeStep(limit: Int): Pair<SpaceContainerSolution, Boolean> {
        return localsearch.optimizeStep(limit)
    }

    override fun init(instance: BinPackProblem) {
        localsearch = LocalSearch(RepackSpaceStrategy(), instance)
    }
}

object LocalSearchRelaxedSpace : Algorithm() {
    override val name = "LocalSearch-RelaxedSpace"
    override val shortName = "LS RelaxedSpace"
    private lateinit var localsearch: LocalSearch<BinPackProblem, SpaceContainerSolution, MSSMove>

    override fun optimize(): BinPackSolution {
        return localsearch.optimize()
    }

    override fun optimizeStep(limit: Int): Pair<SpaceContainerSolution, Boolean> {
        return localsearch.optimizeStep(limit)
    }

    override fun init(instance: BinPackProblem) {
        localsearch = LocalSearch(OverlapSpaceStrategy(), instance)
    }
}