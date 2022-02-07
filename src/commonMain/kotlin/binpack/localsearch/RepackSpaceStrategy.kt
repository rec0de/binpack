package binpack.localsearch

import algorithms.localsearch.LocalSearchStrategy
import binpack.*
import ui.UIState

open class RepackSpaceStrategy : LocalSearchStrategy<BinPackProblem, SpaceContainerSolution, MSSMove> {
    protected lateinit var instance: BinPackProblem
    protected open val estimateFactor: Double = 1.5
    protected val moveBudget = 2000
    protected var moveIndex = 0
    protected var repackEstimationAvailableSpaces: List<Pair<Int,PlacedBox>>? = null

    override fun init(instance: BinPackProblem) {
        this.instance = instance
    }

    override fun initialSolution(): SpaceContainerSolution {
        // Place every box in its own container
        val containers = instance.boxes.sortedByDescending { it.area }.mapIndexed { i, box ->
            val c = SpaceContainer(i, instance.containerSize)
            c.add(box, c.spaces[0])
            c
        }
        return SpaceContainerSolution(instance.containerSize, containers)
    }

    override fun neighboringSolutions(solution: SpaceContainerSolution): List<MSSMove> {
        val containers = solution.containerObjs
        val estimatedRange = (instance.lowerBound * estimateFactor).toInt()

        // For fast progress early on, emulate the greedy algorithm, producing as few candidate moves as possible
        // later on (once we have a halfway decent solution), produce more higher-effort moves
        return if(solution.containerObjs.size > estimatedRange)
            earlyNeighborhood(containers)
        else
            lateNeighborhood(containers)
    }

    protected open fun earlyNeighborhood(containers: List<SpaceContainer>) : List<MSSMove> {
        val moves = mutableListOf<MSSMove>()

        containers.indices.forEach { source ->
            // Find candidate space only for the largest box in each container
            val largestBox = containers[source].boxes.maxByOrNull { it.area }!!
            val boxi = containers[source].boxes.indexOf(largestBox)
            targetSearch@
            for (target in 0 until source) {
                for (spacei in 0 until containers[target].spaces.size) {
                    val space = containers[target].spaces[spacei]
                    if(space.fitsRotated(largestBox)) {
                        moves.add(CrossContainerMove(source, boxi, target, spacei))
                        break@targetSearch
                    }
                }
            }
        }

        return moves
    }

    protected open fun lateNeighborhood(containers: List<SpaceContainer>) : List<MSSMove> {
        var repackMoves = mutableListOf<MSSMove>()
        var localMoves = mutableListOf<MSSMove>()

        val sourceCandidates = containers.filter { it.freeSpace.toDouble() / it.area > 0.2 }

        sourceCandidates.forEach { container ->
            val ci = container.ci
            container.boxes.forEachIndexed { bi, box ->
                // Generate local moves only for a subset of containers
                if(ci % 5 == moveIndex) {
                    container.spaces.indices.forEach { si ->
                        localMoves.add(LocalMove(ci, bi, si))
                    }
                }

                // Generate cross-container repack moves
                (0 until ci).forEach { tci ->
                    containers[tci].spaces.indices.forEach { si ->
                        repackMoves.add(RepackMove(ci, bi, tci, si))
                    }
                }
            }
        }

        //Logger.log("Move count: ${repackMoves.size} repack; ${localMoves.size} local; ${sourceCandidates.size} sc out of ${containers.size}")

        if(localMoves.size > moveBudget * 0.2) {
            localMoves.shuffle()
            localMoves = localMoves.subList(0, (moveBudget * 0.2).toInt())
        }

        if(repackMoves.size > moveBudget - localMoves.size) {
            repackMoves.shuffle()
            repackMoves = repackMoves.subList(0, moveBudget - localMoves.size)
        }

        moveIndex = (moveIndex + 1) % 5
        return localMoves + repackMoves
    }

    override fun deltaScoreMove(solution: SpaceContainerSolution, currentScore: Double, move: MSSMove): Double {
        return when(move) {
            is CrossContainerMove -> {
                val sourceContainer = solution.containerObjs[move.sourceContainer]
                val box = sourceContainer.boxes[move.sourceBox]

                val targetCostDelta = - box.area.toDouble()
                val sourceCostDelta = if(sourceContainer.boxes.size == 1) 0.0 else box.area.toDouble()

                targetCostDelta / (move.targetContainer+1) + sourceCostDelta / (move.sourceContainer+1)
            }
            // Local moves don't change the 'real' evaluation function per definition,
            // so we'll just use a different metric here and optimize for smallest number of spaces
            // (=> largest continuous spaces)
            is LocalMove -> {
                val container = solution.containerObjs[move.container]
                val box = container.boxes[move.box]
                val space = container.spaces[move.space]

                val placed = optimalPlacement(box, space, container.size)

                // box doesn't fit target space, like, _at all_
                if(placed == null)
                    0.0
                else {
                    val cloned = container.clone()
                    cloned.remove(box)
                    val success = localRepack(cloned, placed).isEmpty()
                    if(success)
                        cloned.spaces.size.toDouble() - container.spaces.size
                    else
                        0.0
                }
            }
            is RepackMove -> {
                val source = solution.containerObjs[move.sourceContainer]
                val target = solution.containerObjs[move.targetContainer]
                val box = source.boxes[move.sourceBox]
                val space = target.spaces[move.targetSpace]

                val placed = optimalPlacement(box, space, target.size)

                // box doesn't fit target space, like, _at all_
                if(placed == null)
                    0.0
                else {
                    val cloned = target.clone()
                    val emptiesSource = source.boxes.size == 1
                    val spillover = localRepack(cloned, placed)

                    // If the source would be emptied, we have to explicitly forbid packing spillover there
                    val globalRepackCost = estimateShallowGlobalRepack(
                        target.ci,
                        if(emptiesSource) source.ci else null,
                        spillover
                    )
                    val targetCost = - box.area.toDouble()
                    val sourceCost = if(emptiesSource) box.area - source.area.toDouble() else box.area.toDouble()

                    globalRepackCost + sourceCost / (move.sourceContainer + 1) + targetCost / (move.targetContainer + 1)
                }
            }
            else -> 0.0
        }
    }

    override fun perIterationSharedSetup(solution: SpaceContainerSolution) {
        if(solution.containerObjs.size <= (instance.lowerBound * estimateFactor).toInt()) {
            repackEstimationAvailableSpaces = solution.containerObjs.flatMap { c -> c.spaces.map { Pair(c.ci, it) } }
        }
    }

    override fun applyMove(solution: SpaceContainerSolution, move: MSSMove): SpaceContainerSolution {
        return when(move) {
            is CrossContainerMove -> applyCrossContainerMove(solution, move)
            is LocalMove -> applyLocalMove(solution, move)
            is RepackMove -> applyRepackMove(solution, move)
            else -> throw Exception("Unknown or unsupported move $move")
        }
    }

    protected fun applyLocalMove(solution: SpaceContainerSolution, move: LocalMove) : SpaceContainerSolution {
        val container = solution.containerObjs[move.container]
        val box = container.boxes[move.box]
        val space = container.spaces[move.space]

        container.remove(box)

        val placed = optimalPlacement(box, space, container.size)!!
        val repackSuccess = localRepack(container, placed).isEmpty()

        if(!repackSuccess)
            throw Exception("Local repack failed, not enough space available")

        return solution
    }

    protected fun applyRepackMove(solution: SpaceContainerSolution, move: RepackMove) : SpaceContainerSolution {
        val source = solution.containerObjs[move.sourceContainer]
        val target = solution.containerObjs[move.targetContainer]
        val box = source.boxes[move.sourceBox]
        val space = target.spaces[move.targetSpace]

        val newContainers = solution.containerObjs.toMutableList()

        // Remove box from source
        source.remove(box)
        consolidateSpaces(source)

        val placed = optimalPlacement(box, space, target.size)!!
        val spillover = localRepack(target, placed)

        // Remove container if now empty
        if(source.boxes.isEmpty()) {
            newContainers.removeAt(move.sourceContainer)
            newContainers.subList(move.sourceContainer, newContainers.size).forEach { container -> container.ci -= 1 }
        }

        // Re-use of move.targetContainer index is safe because targetContainer is guaranteed to be before source container, so index is unchanged
        if(spillover.isNotEmpty() && !shallowGlobalRepack(newContainers, move.targetContainer, spillover))
            throw Exception("Local repack produced spillover and global shallow repack failed")

        return SpaceContainerSolution(solution.containerSize, newContainers)
    }

    private fun applyCrossContainerMove(solution: SpaceContainerSolution, move: CrossContainerMove) : SpaceContainerSolution {
        val newContainers = solution.containerObjs.toMutableList()

        val newSource = newContainers[move.sourceContainer].clone()
        val newTarget = newContainers[move.targetContainer].clone()

        val movedBox = newSource.boxes[move.sourceBox]
        newSource.remove(movedBox)
        newTarget.add(movedBox, newTarget.spaces[move.targetSpace])
        consolidateSpaces(newTarget)

        newContainers[move.targetContainer] = newTarget

        if(newSource.boxes.isEmpty()) {
            newContainers.removeAt(move.sourceContainer)
            newContainers.subList(move.sourceContainer, newContainers.size).forEach { container -> container.ci -= 1 }
        }
        else {
            consolidateSpaces(newSource)
            newContainers[move.sourceContainer] = newSource
        }

        return SpaceContainerSolution(solution.containerSize, newContainers)
    }

    override fun scoreSolution(solution: SpaceContainerSolution) = 0.0

    private fun estimateShallowGlobalRepack(sourceContainer: Int, additionalAvoid: Int?, boxes: List<Box>): Double {
        if(boxes.isEmpty())
            return 0.0

        var delta = boxes.sumOf { it.area }.toDouble() / (1 + sourceContainer)
        val availableSpaces = repackEstimationAvailableSpaces!!
        val usedSpaces: MutableSet<Pair<Int,PlacedBox>> = mutableSetOf()

        boxes.sortedByDescending { it.area }.forEach { box ->
            val space = availableSpaces.firstOrNull { it.first != sourceContainer && it.first != additionalAvoid && it.second.fitsRotated(box) && !usedSpaces.contains(it) } ?: return Double.POSITIVE_INFINITY
            usedSpaces.add(space)
            delta -= box.area / (1 + space.first)
        }

        return delta
    }

    protected fun shallowGlobalRepack(containers: List<SpaceContainer>, sourceContainer: Int, boxes: List<Box>) : Boolean {
        boxes.sortedByDescending { it.area }.forEach { box ->
            val target = containers.firstOrNull { it.ci != sourceContainer && it.freeSpace >= box.area && it.spaces.any { space -> space.fitsRotated(box) } } ?: return false
            val space = target.spaces.first{ it.fitsRotated(box) }
            target.add(box, space)
        }
        return true
    }

    private fun localRepack(container: SpaceContainer, placed: PlacedBox) : List<Box> {
        // Check bounds
        if(placed.outOfBounds(container.size))
            throw Exception("Local repack box $placed is out of bounds")

        // Remove conflicting boxes
        val toRepack = container.boxes.filter { it.intersects(placed) }.sortedByDescending { it.area }
        toRepack.forEach { container.remove(it) }

        // Place box and adjust spaces
        container.boxes.add(placed)
        container.spaces.filter { it.intersects(placed) }.forEach {
            container.spaces.remove(it)
            container.spaces.addAll(it.shatter(placed))
        }

        // Repack conflicting boxes
        return repackIntoContainer(container, toRepack)
    }

    private fun repackIntoContainer(c: SpaceContainer, boxes: Collection<PlacedBox>) : List<Box> {
        val overflow = mutableListOf<Box>()
        consolidateSpaces(c)

        boxes.forEach { repackBox ->
            val fit = c.spaces.firstOrNull { it.fitsRotated(repackBox) }

            if(fit == null)
                overflow.add(repackBox)
            else {
                c.add(repackBox, fit)
                consolidateSpaces(c)
            }
        }

        return overflow
    }

    protected fun optimalPlacement(box: Box, space: PlacedBox, size: Int): PlacedBox? {
        val candidates = listOf(
            box.asPlaced(space.x, space.y),
            box.asPlaced(space.endX - box.w, space.y),
            box.asPlaced(space.x, space.endY - box.h),
            box.asPlaced(space.endX - box.w, space.endY - box.h),
            box.rotate().asPlaced(space.x, space.y),
            box.rotate().asPlaced(space.endX - box.w, space.y),
            box.rotate().asPlaced(space.x, space.endY - box.h),
            box.rotate().asPlaced(space.endX - box.w, space.endY - box.h)
        )
        return candidates.filter { !it.outOfBounds(size) }.maxByOrNull { space.relativeOverlap(it) }
    }

    protected fun consolidateSpaces(c: SpaceContainer) {
        var i: Int = 0
        var j: Int

        while(i < c.spaces.size) {
            j = i + 1
            var a = c.spaces[i]

            while(j < c.spaces.size) {
                val b = c.spaces[j]
                if(a.continuous(b)) {
                    c.spaces[i] = a.superBox(b)
                    c.spaces.removeAt(j)
                    a = c.spaces[i]
                }
                else
                    j += 1
            }
            i += 1
        }
    }

    protected fun renderDebugSpaces(containers: List<SpaceContainer>) {
        UIState.debugVisualizer!!.debugClear()
        containers.forEachIndexed { ci, container ->
            container.spaces.forEach { space ->
                UIState.debugVisualizer!!.debugBox(space, ci)
            }
        }
    }
}

interface MSSMove
data class CrossContainerMove(val sourceContainer: Int, val sourceBox: Int, val targetContainer: Int, val targetSpace: Int) : MSSMove
data class LocalMove(val container: Int, val box: Int, val space: Int) : MSSMove
data class RepackMove(val sourceContainer: Int, val sourceBox: Int, val targetContainer: Int, val targetSpace: Int) : MSSMove