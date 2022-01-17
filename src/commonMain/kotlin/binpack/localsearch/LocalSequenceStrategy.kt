package binpack.localsearch

import algorithms.localsearch.LocalSearchStrategy
import binpack.BinPackProblem
import binpack.Box
import binpack.ContainerSolution
import binpack.SegmentContainer
import binpack.greedy.GenericBinPacker

class LocalSequenceStrategy(private val packer: GenericBinPacker<SegmentContainer>) : LocalSearchStrategy<BinPackProblem, ContainerSolution<SegmentContainer>, LSSMove> {
    private lateinit var instance: BinPackProblem

    override fun init(instance: BinPackProblem) {
        this.instance = instance
    }

    override fun initialSolution(): ContainerSolution<SegmentContainer> {
        packer.init(instance.containerSize)
        instance.boxes.sortedBy { -it.area }.forEach { packer.packItem(it) }
        //instance.boxes.forEach { packer.packItem(it) }
        return packer.getSolution()
    }

    override fun neighboringSolutions(solution: ContainerSolution<SegmentContainer>): Iterable<LSSMove> {
        val reflowTargets = solution.containerObjs.filter { it.accessibleSpace > 0 }.map { it.ci }.toSet()
        val reflows = solution.containerObjs.indices.flatMap { i -> (0 until i).mapNotNull { j -> if(reflowTargets.contains(j)) ReflowContainer(j, i) else null } }
        val swaps = solution.containerObjs.filter{ it.accessibleSpace + 1 < it.freeSpace && it.ci != solution.containerObjs.size - 1 }.flatMap { container -> container.boxes.indices.flatMap { i -> (0 until i).map { j -> LocalSwap(container.ci, i, j) } } }
        //Logger.log("Move count: ${reflows.size} reflow ops, ${swaps.size} swap ops, total ${reflows.size + swaps.size}")
        return reflows + swaps
    }

    override fun deltaScoreMove(solution: ContainerSolution<SegmentContainer>, currentScore: Double, move: LSSMove): Double {
        return if(move is ReflowContainer) {
            val targetContainer = solution.containerObjs[move.target].clone()
            val sourceContainer = solution.containerObjs[move.source]
            val targetOld = scoreContainer(targetContainer)

            val remaining = sourceContainer.boxes.filter { box ->
                val res = packer.packIntoContainer(box, targetContainer)
                if(res != null) {
                    targetContainer.add(res.first)
                    false
                }
                else
                    true
            }

            val repacked = repackContainer(sourceContainer, remaining)

            val res = when {
                remaining.isEmpty() -> (- solution.containerSize * solution.containerSize).toDouble() // Large bonus score for emptying container
                repacked.second != 0 -> 1.0 // Overflow in source container during reflow - annoying bc technically just a packer quirk (load on source container is only ever reduced)
                else -> scoreContainer(targetContainer) - targetOld
            }
            //log("$move scored $res")
            res
        }
        else {
            move as LocalSwap
            val container = solution.containerObjs[move.ci]

            container.swap(move.a, move.b)
            val repacked = repackContainer(container)
            container.swap(move.a, move.b)

            if(repacked.second == 0)
                (container.accessibleSpace - repacked.first.accessibleSpace).toDouble() / 2
            else
                1.0 // Positive delta scores should never be chosen -> avoids overflows due to local repack
        }
    }

    override fun applyMove(solution: ContainerSolution<SegmentContainer>, move: LSSMove): ContainerSolution<SegmentContainer> {
        return if(move is ReflowContainer) {
            val targetContainer = solution.containerObjs[move.target]
            val sourceContainer = solution.containerObjs[move.source]

            val remaining = sourceContainer.boxes.filter { box ->
                val res = packer.packIntoContainer(box, targetContainer)
                if(res != null) {
                    targetContainer.add(res.first)
                    false
                }
                else
                    true
            }

            val newContainers = solution.containerObjs.toMutableList()

            if(remaining.isEmpty()) {
                newContainers.removeAt(move.source)
                newContainers.subList(move.source, newContainers.size).forEach { container -> container.ci -= 1 }
            }
            else {
                sourceContainer.boxes.clear()
                sourceContainer.boxes.addAll(remaining)
                val repacked = repackContainer(sourceContainer)
                newContainers[move.source] = repacked.first
            }
            ContainerSolution(solution.containerSize, newContainers)
        }
        else {
            move as LocalSwap
            val container = solution.containerObjs[move.ci]
            container.swap(move.a, move.b)
            val repacked = repackContainer(container)

            if(repacked.second != 0)
                throw Exception("LocalSwap move caused container overflow - this shouldn't happen")

            val newContainers = solution.containerObjs.toMutableList()
            newContainers[move.ci] = repacked.first
            ContainerSolution(solution.containerSize, newContainers)
        }
    }

    override fun scoreSolution(solution: ContainerSolution<SegmentContainer>) = solution.containerObjs.sumOf { scoreContainer(it) }

    private fun scoreContainer(container: SegmentContainer) = container.freeSpace - container.accessibleSpace.toDouble() / 4

    private fun repackContainer(container: SegmentContainer, boxes: List<Box> = container.boxes): Pair<SegmentContainer, Int> {
        val repacked = SegmentContainer(container.ci, container.size)
        var overflows = 0
        boxes.forEach { box ->
            val res = packer.packIntoContainer(box, repacked)
            if(res != null)
                repacked.add(res.first)
            else
                overflows += 1
        }
        return Pair(repacked, overflows)
    }
}

interface LSSMove
data class ReflowContainer(val target: Int, val source: Int) : LSSMove
data class LocalSwap(val ci: Int, val a: Int, val b: Int) : LSSMove
