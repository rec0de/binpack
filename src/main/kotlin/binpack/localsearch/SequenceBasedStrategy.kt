package binpack.localsearch

import algorithms.localsearch.LocalSearchStrategy
import binpack.BinPackProblem
import binpack.SequenceSolution
import binpack.greedy.GenericBinPacker

class SequenceBasedStrategy(private val packer: GenericBinPacker) : LocalSearchStrategy<BinPackProblem, SequenceSolution, SwapMove> {
    private lateinit var moves: List<SwapMove>
    private lateinit var instance: BinPackProblem

    override fun init(instance: BinPackProblem) {
        this.instance = instance
        this.moves = instance.boxes.indices.flatMap { i ->
            (0 until i).map { j -> SwapMove(i, j) }
        }
    }

    override fun initialSolution(): SequenceSolution {
        packer.init(instance.containerSize)
        instance.boxes.forEach { packer.packItem(it) }
        return packer.getSolution().asSequenceSolution(instance.boxes)
    }

    override fun neighboringSolutions(solution: SequenceSolution): Iterable<SwapMove> = moves

    override fun applyMove(solution: SequenceSolution, move: SwapMove): SequenceSolution {
        val newBoxes = solution.insertionSequence.toMutableList()

        val tmp = newBoxes[move.a]
        newBoxes[move.a] = newBoxes[move.b]
        newBoxes[move.b] = tmp

        packer.init(solution.containerSize)
        newBoxes.forEach { packer.packItem(it) }

        return SequenceSolution(solution.containerSize, newBoxes, packer.getSolution().containers)
    }

    override fun scoreMove(solution: SequenceSolution, move: SwapMove): Double {
        val newState = applyMove(solution, move)
        return scoreSolution(newState)
    }

    override fun scoreSolution(solution: SequenceSolution): Double {
        return 1 - solution.k1PackDensity()
    }
}