package binpack.localsearch

import algorithms.localsearch.LocalSearchStrategy
import binpack.BinPackProblem
import binpack.BinPackSolution
import binpack.Box
import binpack.ContainerSolution
import binpack.greedy.Container
import binpack.greedy.GenericBinPacker

class LocalSequenceStrategy(private val packer: GenericBinPacker) : LocalSearchStrategy<BinPackProblem, ContainerSolution, LSSMove> {
    private lateinit var instance: BinPackProblem

    override fun init(instance: BinPackProblem) {
        this.instance = instance
    }

    override fun initialSolution(): ContainerSolution {
        packer.init(instance.containerSize)
        instance.boxes.sortedBy { -it.area }.forEach { packer.packItem(it) }
        //instance.boxes.forEach { packer.packItem(it) }
        return packer.getSolution()
    }

    override fun neighboringSolutions(solution: ContainerSolution): Iterable<LSSMove> {
        val reflowTargets = solution.containerObjs.filter { it.accessibleSpace > 0 }.map { it.ci }.toSet()
        val reflows = solution.containerObjs.indices.flatMap { i -> (0 until i).mapNotNull { j -> if(reflowTargets.contains(j)) ReflowContainer(j, i) else null } }
        val swaps = solution.containerObjs.filter{ it.accessibleSpace < it.freeSpace }.flatMap { container -> container.boxes.indices.flatMap { i -> (0 until i).map { j -> LocalSwap(container.ci, i, j) } } }
        console.log("Move count: ${reflows.size} reflow ops, ${swaps.size} swap ops, total ${reflows.size + swaps.size}")
        return reflows + swaps
    }

    override fun deltaScoreMove(solution: ContainerSolution, currentScore: Double, move: LSSMove): Double {
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

            if(repacked.second != 0)
                1.0 // Overflow in source container during reflow - annoying bc technically just a packer quirk (load on source container is only ever reduced)
            else
                scoreContainer(targetContainer) - targetOld
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

    override fun applyMove(solution: ContainerSolution, move: LSSMove): ContainerSolution {
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

    override fun scoreSolution(solution: ContainerSolution): Double {
        // TODO: freeSpace and accessibleSpace can be cached in container
        return solution.containerObjs.sumOf { scoreContainer(it) }
    }

    private fun scoreContainer(container: Container) = container.freeSpace - container.accessibleSpace.toDouble() / 2

    private fun repackContainer(container: Container, boxes: List<Box> = container.boxes): Pair<Container, Int> {
        val repacked = Container(container.ci, container.size)
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
