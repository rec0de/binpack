package binpack.localsearch

import algorithms.localsearch.LocalSearchStrategy
import binpack.BinPackProblem
import binpack.Box
import binpack.ContainerSolution
import binpack.SegmentContainer
import binpack.greedy.GenericBinPacker
import kotlin.math.max

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
        val containers = solution.containerObjs

        // Try reflowing boxes into containers with accessible space
        val reflowTargets = containers.filter { it.accessibleSpace > 0 }.map { it.ci }.toSet()
        val reflows = containers.indices.flatMap { i -> (0 until i).mapNotNull { j -> if(reflowTargets.contains(j)) ReflowContainer(j, i) else null } }

        // Try locally swapping boxes in containers where accessible space < free space
        val swaps = containers.filter{ it.accessibleSpace + 1 < it.freeSpace && it.ci != containers.size - 1 }.flatMap { container -> container.boxes.indices.flatMap { i -> (0 until i).map { j -> LocalSwap(container.ci, i, j) } } }

        // Try inserting boxes into otherwise inaccessible air pockets
        val insertTargets = containers.flatMapIndexed { ci, container -> container.deepInsertionCandidates.map { Triple(ci, it.first, it.second) } }
        val insertSources = containers.subList(max(0, containers.size - 3), containers.size).flatMap { container -> container.boxes.mapIndexed{ i, b -> Triple(container.ci, i, b.area) } }
        val inserts = insertTargets.flatMap { target ->
            insertSources.mapNotNull { source ->
                if(source.first != target.first && target.third >= source.third)
                    DeepInsert(source.first, source.second, target.first, target.second)
                else
                    null
            }
        }

        Logger.log("Move count: ${reflows.size} reflow ops, ${swaps.size} swap ops, ${inserts.size} insert ops")
        return reflows + swaps + inserts
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
                repacked == null -> 1.0 // Overflow in source container during reflow - annoying bc technically just a packer quirk (load on source container is only ever reduced)
                else -> (scoreContainer(targetContainer) - targetOld) * 1.5
            }
            //log("$move scored $res")
            res
        }
        else if(move is DeepInsert) {
            val sourceContainer = solution.containerObjs[move.sourceC]
            val targetContainer = solution.containerObjs[move.targetC]
            val box = sourceContainer.boxes[move.sourceB]

            val newBoxes = targetContainer.boxes.toMutableList()
            newBoxes.add(move.targetB, box)
            val repacked = repackContainer(targetContainer, newBoxes)

            if(repacked == null)
                1.0 // Deep insert failed, target container overflow
            else {
                // Repack source container
                val sourceBoxes = sourceContainer.boxes.toMutableList()
                sourceBoxes.removeAt(move.sourceB)
                val repackedSource = repackContainer(sourceContainer, sourceBoxes)
                if(repackedSource == null)
                    1.0 // Source container overflowed under lower load (sigh)
                else
                    ((scoreContainer(repackedSource) + scoreContainer(repacked)) - (scoreContainer(sourceContainer) + scoreContainer(targetContainer))) / 2
            }
        }
        else {
            move as LocalSwap
            val container = solution.containerObjs[move.ci]

            container.swap(move.a, move.b)
            val repacked = repackContainer(container)
            container.swap(move.a, move.b)

            if(repacked != null)
                (container.accessibleSpace - repacked.accessibleSpace).toDouble() / 2
            else
                1.0 // Positive delta scores should never be chosen -> avoids overflows due to local repack
        }
    }

    override fun applyMove(solution: ContainerSolution<SegmentContainer>, move: LSSMove): ContainerSolution<SegmentContainer> {
        return when (move) {
            is ReflowContainer -> {
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
                    newContainers[move.source] = repacked!!
                }
                ContainerSolution(solution.containerSize, newContainers)
            }
            is DeepInsert -> {
                val sourceContainer = solution.containerObjs[move.sourceC]
                val targetContainer = solution.containerObjs[move.targetC]
                val box = sourceContainer.boxes[move.sourceB]
                val newContainers = solution.containerObjs.toMutableList()

                // Repack target container
                targetContainer.boxes.add(move.targetB, box)
                newContainers[move.targetC] = repackContainer(targetContainer)!!

                // Repack source container
                sourceContainer.boxes.removeAt(move.sourceB)
                newContainers[move.sourceC] = repackContainer(sourceContainer)!!

                ContainerSolution(solution.containerSize, newContainers)
            }
            else -> {
                move as LocalSwap
                val container = solution.containerObjs[move.ci]
                container.swap(move.a, move.b)
                val repacked = repackContainer(container)
                    ?: throw Exception("LocalSwap move caused container overflow - this shouldn't happen")

                val newContainers = solution.containerObjs.toMutableList()
                newContainers[move.ci] = repacked
                ContainerSolution(solution.containerSize, newContainers)
            }
        }
    }

    override fun scoreSolution(solution: ContainerSolution<SegmentContainer>) = solution.containerObjs.sumOf { scoreContainer(it) }

    private fun scoreContainer(container: SegmentContainer) = container.freeSpace - container.accessibleSpace.toDouble() / 4

    private fun repackContainer(container: SegmentContainer, boxes: List<Box> = container.boxes): SegmentContainer? {
        val repacked = SegmentContainer(container.ci, container.size)
        boxes.forEach { box ->
            val res = packer.packIntoContainer(box, repacked)
            if(res != null)
                repacked.add(res.first)
            else
                return null
        }
        return repacked
    }
}

interface LSSMove
data class ReflowContainer(val target: Int, val source: Int) : LSSMove
data class LocalSwap(val ci: Int, val a: Int, val b: Int) : LSSMove
data class DeepInsert(val sourceC: Int, val sourceB: Int, val targetC: Int, val targetB: Int) : LSSMove