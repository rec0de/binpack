package binpack.configurations

import algorithms.localsearch.LocalSearch
import binpack.Algorithm
import binpack.BinPackProblem
import binpack.BinPackSolution
import binpack.greedy.NormalPosFirstFitPacker
import binpack.localsearch.GravityBinPackStrategy

object LocalSearchGravity : Algorithm() {
    override val name = "LocalSearch-NormalPosFirstFit"
    private val strategy = GravityBinPackStrategy
    private lateinit var partialSolution: BinPackProblem

    override fun optimize(): BinPackSolution {
        partialSolution = LocalSearch.optimize(strategy, partialSolution)
        return convertSolution(partialSolution)
    }

    override fun optimizeStep(limit: Int): Pair<BinPackSolution, Boolean> {
        val res = LocalSearch.optimizeStep(strategy, partialSolution, limit)
        partialSolution = res.first
        return Pair(convertSolution(partialSolution), res.second)
    }

    override fun init(instance: BinPackProblem) {
        partialSolution = strategy.initialSolution(instance)
    }

    private fun convertSolution(sequence: BinPackProblem): BinPackSolution {
        NormalPosFirstFitPacker.init(sequence.containerSize)
        sequence.boxes.forEach { NormalPosFirstFitPacker.packItem(it) }
        return NormalPosFirstFitPacker.getSolution()
    }
}