package binpack.localsearch

import Logger
import binpack.SpaceContainer
import binpack.SpaceContainerSolution
import kotlin.math.max

class OverlapSpaceStrategy : RepackSpaceStrategy() {
    override val estimateFactor = 1.0
    private var allowedOverlap = 1.0
    private var overlapPenalty = 1.0
    private var consecutivePlaceboMoves = 0
    private var forceLateNeighborhood = false

    override fun initialSolution(): SpaceContainerSolution {
        // Place every box in its own container
        val containers = instance.boxes.mapIndexed { i, box ->
            val c = SpaceContainer(i, instance.containerSize)
            c.add(box, c.spaces[0])
            c
        }
        return SpaceContainerSolution(instance.containerSize, containers)
    }

    override fun perIterationSharedSetup(solution: SpaceContainerSolution) {
        if(forceLateNeighborhood) {
            repackEstimationAvailableSpaces = solution.containerObjs.flatMap { c -> c.spaces.map { Pair(c.ci, it) } }
        }
    }

    override fun neighboringSolutions(solution: SpaceContainerSolution): List<MSSMove> {
        val containers = solution.containerObjs

        // For fast progress early on, emulate the greedy algorithm, producing as few candidate moves as possible
        // later on (once we have a halfway decent solution), produce more higher-effort moves
        return if(!forceLateNeighborhood)
            earlyNeighborhood(containers)
        else
            lateNeighborhood(containers)
    }

    override fun earlyNeighborhood(containers: List<SpaceContainer>) : List<MSSMove> {
        val cramMoves = mutableListOf<MSSMove>()

        val target = containers.firstOrNull { it.hasAccessibleSpace }

        target?.spaces?.forEachIndexed { si, space ->

            val box = containers.subList(target.ci+1, containers.size).flatMap { c ->
                c.boxes.mapIndexedNotNull { bi, b ->
                    val placed = optimalPlacement(b, space, target.size)
                    val condition = (placed == null) // || (allowedOverlap < 1.0 && c.boxes.any { it.relativeOverlap(placed) > allowedOverlap })
                    if(condition) null else Triple(c.ci, bi, placed!!)
                }
            }.maxByOrNull { space.relativeOverlap(it.third) }

            if(box != null)
                cramMoves.add(CramMove(box.first, box.second, target.ci, si))
        }

        return if(cramMoves.size == 0) {
            forceLateNeighborhood = true
            lateNeighborhood(containers)
        }
        else
            listOf(PlaceboMove) + cramMoves
    }

    override fun lateNeighborhood(containers: List<SpaceContainer>) : List<MSSMove> {
        val baseMoves = mutableListOf<MSSMove>()
        var cramMoves = mutableListOf<MSSMove>()
        var localMoves = mutableListOf<MSSMove>()

        // Ensure that local search never stops before all overlaps are removed
        if(containers.any { c -> c.boxes.any{ b1 -> c.boxes.any{ b2 -> b1 != b2 && b1.intersects(b2) } } })
            baseMoves.add(PlaceboMove)

        // In the worst case, we have to add overlapping boxes to a new container
        val escapeMoves = containers.flatMap { c ->
            c.boxes.mapIndexedNotNull { bi, box -> if(c.boxes.any { box != it && box.relativeOverlap(it) > allowedOverlap }) EscapeMove(c.ci, bi) else null }
        }

        val sourceCandidates = containers.filter { it.freeSpace.toDouble() / it.area > 0.1 }

        sourceCandidates.forEach { container ->
            val ci = container.ci
            container.boxes.forEachIndexed { bi, box ->
                // Generate local moves only for a subset of containers
                if(ci % 5 == moveIndex % 5) {
                    container.spaces.indices.forEach { si ->
                        localMoves.add(LocalMove(ci, bi, si))
                    }
                }

                // Generate cross-container cram moves
                /*(0 until ci).forEach { tci ->
                    containers[tci].spaces.forEachIndexed { si, space ->
                        if(box.area * allowedOverlap < space.area)
                            cramMoves.add(CramMove(ci, bi, tci, si))
                    }
                }*/
                // Generate cross-container repack moves
                (0 until ci).forEach { tci ->
                    containers[tci].spaces.indices.forEach { si ->
                        cramMoves.add(RepackMove(ci, bi, tci, si))
                    }
                }
            }
        }

        //Logger.log("Move count: ${cramMoves.size} cram; ${localMoves.size} local; ${escapeMoves.size} escape;")

        if(localMoves.size > moveBudget * 0.2) {
            localMoves.shuffle()
            localMoves = localMoves.subList(0, (moveBudget * 0.2).toInt())
        }

        if(cramMoves.size > moveBudget - localMoves.size) {
            cramMoves.shuffle()
            cramMoves = cramMoves.subList(0, moveBudget - localMoves.size)
        }

        moveIndex += 1
        return baseMoves + escapeMoves + localMoves + cramMoves
    }

    override fun deltaScoreMove(solution: SpaceContainerSolution, currentScore: Double, move: MSSMove): Double {
        return when(move) {
            is PlaceboMove -> -0.00001
            is LocalMove -> super.deltaScoreMove(solution, currentScore, move)
            is RepackMove -> super.deltaScoreMove(solution, currentScore, move)
            is EscapeMove -> {
                val source = solution.containerObjs[move.sourceContainer]
                val box = source.boxes[move.sourceBox]
                val newContainerCost = (source.area - box.area).toDouble() / (1 + solution.containerObjs.size)
                val overlapCost = - source.boxes.sumOf { b -> if(box.relativeOverlap(b) > allowedOverlap && box != b) box.intersection(b) else 0 } * overlapPenalty

                newContainerCost + overlapCost
            }
            is CramMove -> {
                val source = solution.containerObjs[move.sourceContainer]
                val target = solution.containerObjs[move.targetContainer]
                val box = source.boxes[move.sourceBox]
                val space = target.spaces[move.targetSpace]

                val placed = optimalPlacement(box, space, target.size)

                // box doesn't fit target space, like, _at all_
                if(placed == null || target.boxes.any { placed.relativeOverlap(it) > allowedOverlap })
                    0.0
                else {
                    val emptiesSource = source.boxes.size == 1
                    val overlap = target.boxes.sumOf { it.intersection(placed) }
                    val overlapCost = - source.boxes.sumOf { b -> if(box.relativeOverlap(b) > allowedOverlap && box != b) box.intersection(b) else 0 }

                    val targetCost = - max(1.0, (box.area.toDouble() - overlap))
                    val sourceCost = if(emptiesSource) box.area - source.area.toDouble() else if(move.targetContainer < move.sourceContainer) 0.0 else box.area.toDouble()

                    overlapCost * overlapPenalty + sourceCost / (move.sourceContainer + 1) + targetCost / (move.targetContainer + 1)
                }
            }
            else -> 0.0
        }
    }

    override fun applyMove(solution: SpaceContainerSolution, move: MSSMove): SpaceContainerSolution {
        return when(move) {
            is PlaceboMove -> {
                // Accelerate overlap cost increase
                consecutivePlaceboMoves += 1
                overlapPenalty += 0.3 * consecutivePlaceboMoves
                allowedOverlap = max(0.0, allowedOverlap - 0.007 * consecutivePlaceboMoves)
                //Logger.log("Allowed overlap now $allowedOverlap ($consecutivePlaceboMoves consecutive)")
                solution
            }
            is RepackMove -> {
                allowedOverlap = max(0.0, allowedOverlap - 0.001)
                applyRepackMove(solution, move)
            }
            is LocalMove -> applyLocalMove(solution, move)
            is CramMove -> applyCramMove(solution, move)
            is EscapeMove -> applyEscapeMove(solution, move)
            else -> throw Exception("Unknown or unsupported move $move")
        }
    }

    private fun applyCramMove(solution: SpaceContainerSolution, move: CramMove): SpaceContainerSolution {
        consecutivePlaceboMoves = 0
        val source = solution.containerObjs[move.sourceContainer]
        val target = solution.containerObjs[move.targetContainer]
        val box = source.boxes[move.sourceBox]
        val space = target.spaces[move.targetSpace]

        val placed = optimalPlacement(box, space, target.size) ?: throw Exception("No valid placement for $box")

        val newContainers = solution.containerObjs.toMutableList()

        // Remove box from source
        source.remove(box, overlapPossible = true)
        consolidateSpaces(source)

        // Place box and adjust spaces
        target.boxes.add(placed)
        target.spaces.filter { it.intersects(placed) }.forEach {
            target.spaces.remove(it)
            target.spaces.addAll(it.shatter(placed))
        }

        consolidateSpaces(target)

        // Remove container if now empty
        if(source.boxes.isEmpty()) {
            newContainers.removeAt(move.sourceContainer)
            newContainers.subList(move.sourceContainer, newContainers.size).forEach { container -> container.ci -= 1 }
        }

        return SpaceContainerSolution(solution.containerSize, newContainers)
    }

    private fun applyEscapeMove(solution: SpaceContainerSolution, move: EscapeMove): SpaceContainerSolution {
        consecutivePlaceboMoves = 0
        val source = solution.containerObjs[move.sourceContainer]
        val box = source.boxes[move.sourceBox]

        val newContainers = solution.containerObjs.toMutableList()
        source.remove(box, overlapPossible = true)

        // Try a shallow global repack first before creating a new container
        if(!shallowGlobalRepack(newContainers, move.sourceContainer, listOf(box))) {
            val target = SpaceContainer(solution.containerObjs.size, source.size)
            target.add(box)
            newContainers.add(target)
        }

        return SpaceContainerSolution(solution.containerSize, newContainers)
    }

    override fun scoreSolution(solution: SpaceContainerSolution): Double {
        //renderDebugSpaces(solution.containerObjs)
        return 0.0
    }
}

object PlaceboMove : MSSMove {
    override fun toString() = "PlaceboMove"
}
data class CramMove(val sourceContainer: Int, val sourceBox: Int, val targetContainer: Int, val targetSpace: Int) : MSSMove
data class EscapeMove(val sourceContainer: Int, val sourceBox: Int) : MSSMove