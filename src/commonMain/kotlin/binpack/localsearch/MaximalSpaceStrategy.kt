package binpack.localsearch

import algorithms.localsearch.LocalSearchStrategy
import binpack.BinPackProblem
import binpack.PlacedBox
import binpack.SpaceContainer
import binpack.SpaceContainerSolution

class MaximalSpaceStrategy : LocalSearchStrategy<BinPackProblem, SpaceContainerSolution, MSSMove> {
    private lateinit var instance: BinPackProblem
    private val estimateFactor: Double = 1.5

    override fun init(instance: BinPackProblem) {
        this.instance = instance
    }

    override fun initialSolution(): SpaceContainerSolution {
        // Place every box in its own container
        val containers = instance.boxes.mapIndexed { i, box ->
            val c = SpaceContainer(i, instance.containerSize)
            c.add(box, c.spaces[0])
            c
        }
        return SpaceContainerSolution(instance.containerSize, containers)
    }

    override fun neighboringSolutions(solution: SpaceContainerSolution): Iterable<MSSMove> {
        val containers = solution.containerObjs
        val estimatedRange = (instance.lowerBound * estimateFactor).toInt()

        // For fast progress early on, emulate the greedy algorithm, producing as few candidate moves as possible
        // later on (once we have a halfway decent solution), produce more higher-effort moves
        return if(solution.containerObjs.size > estimatedRange)
            earlyNeighborhood(containers)
        else
            lateNeighborhood(containers)
    }

    private fun earlyNeighborhood(containers: List<SpaceContainer>) : List<MSSMove> {
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

    private fun lateNeighborhood(containers: List<SpaceContainer>) : List<MSSMove> {
        val moves = mutableListOf<MSSMove>()

        containers.forEachIndexed { ci, container ->
            container.boxes.indices.forEach{ bi ->
                // Generate local moves
                container.spaces.indices.forEach { si ->
                    moves.add(LocalMove(ci, bi, si))
                }

                // Generate cross-container repack moves
                containers.forEachIndexed { tci, targetContainer ->
                    targetContainer.spaces.indices.forEach { si ->
                        moves.add(RepackMove(ci, bi, tci, si))
                    }
                }
            }
        }

        return moves
    }

    override fun deltaScoreMove(solution: SpaceContainerSolution, currentScore: Double, move: MSSMove): Double {
        return when(move) {
            is CrossContainerMove -> {
                val sourceContainer = solution.containerObjs[move.sourceContainer]
                val box = sourceContainer.boxes[move.sourceBox]
                val space = solution.containerObjs[move.targetContainer].spaces[move.targetSpace]
                //box.area * (1.0 / move.sourceContainer - 1.0 / move.targetContainer)

                //val targetCostDelta = shatter(space, box).sumOf { it.area }.toDouble() - space.area
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

                // box doesn't fit target space, like, _at all_
                if(box.asPlaced(space.x, space.y).outOfBounds(solution.containerSize))
                    0.0
                else {
                    val cloned = container.clone()
                    cloned.remove(box)
                    val success = localRepack(cloned, box.asPlaced(space.x, space.y))
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

                // box doesn't fit target space, like, _at all_
                if(box.asPlaced(space.x, space.y).outOfBounds(solution.containerSize))
                    0.0
                else {
                    val cloned = target.clone()

                    val success = localRepack(cloned, box.asPlaced(space.x, space.y))
                    if(success)
                        box.area.toDouble() / (move.sourceContainer + 1) - box.area.toDouble() / (move.targetContainer + 1)
                    else
                        0.0
                }
            }
            else -> 0.0
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

    private fun applyLocalMove(solution: SpaceContainerSolution, move: LocalMove) : SpaceContainerSolution {
        val container = solution.containerObjs[move.container]
        val box = container.boxes[move.box]
        val space = container.spaces[move.space]

        container.remove(box)

        // rotation?
        val placed = box.asPlaced(space.x, space.y)

        val repackSuccess = localRepack(container, placed)

        if(!repackSuccess)
            throw Exception("Local repack failed, not enough space available")

        return solution
    }

    private fun applyRepackMove(solution: SpaceContainerSolution, move: RepackMove) : SpaceContainerSolution {
        val source = solution.containerObjs[move.sourceContainer]
        val target = solution.containerObjs[move.targetContainer]
        val box = source.boxes[move.sourceBox]
        val space = target.spaces[move.targetSpace]

        val newContainers = solution.containerObjs.toMutableList()

        // Remove box from source
        source.remove(box)
        consolidateSpaces(source)

        // rotation?
        val placed = box.asPlaced(space.x, space.y)

        val repackSuccess = localRepack(target, placed)

        if(!repackSuccess)
            throw Exception("Repack failed, not enough space available")

        if(source.boxes.isEmpty())
            newContainers.removeAt(move.sourceContainer)

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

        if(newSource.boxes.isEmpty())
            newContainers.removeAt(move.sourceContainer)
        else {
            consolidateSpaces(newSource)
            newContainers[move.sourceContainer] = newSource
        }

        /*UIState.debugVisualizer!!.debugClear()
        newContainers.forEachIndexed { ci, container ->
            container.spaces.forEach { space ->
                UIState.debugVisualizer!!.debugBox(space, ci)
            }
        }*/

        return SpaceContainerSolution(solution.containerSize, newContainers)
    }

    override fun scoreSolution(solution: SpaceContainerSolution): Double {
        return solution.containerObjs.mapIndexed { index, container -> container.spaces.sumOf{ it.area } * (1.0 / (index+1))}.sum()
    }

    private fun localRepack(container: SpaceContainer, placed: PlacedBox) : Boolean {
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

    private fun repackIntoContainer(c: SpaceContainer, boxes: Collection<PlacedBox>) : Boolean {
        consolidateSpaces(c)

        boxes.forEach { repackBox ->
            val fit = c.spaces.firstOrNull { it.fitsRotated(repackBox) } ?: return false
            c.add(repackBox, fit)
            consolidateSpaces(c)
        }

        return true
    }

    private fun consolidateSpaces(c: SpaceContainer) {
        var i: Int = 0
        var j: Int

        while(i < c.spaces.size) {
            j = i + 1
            var a = c.spaces[i]

            while(j < c.spaces.size) {
                val b = c.spaces[j]
                if(a.continuous(b)) {
                    //Logger.log("Box $a is continuous with $b, replacing with ${a.superBox(b)}")
                    c.spaces[i] = a.superBox(b)
                    c.spaces.removeAt(j)
                    a = c.spaces[i]
                }
                else
                    j += 1
            }
            i += 1
        }

        //Logger.log("Consolidation removed ${prev - c.spaces.size} spaces")
    }

    private fun checkInvariants(id: String, c: SpaceContainer) {
        if(c.spaces.any { a -> c.spaces.any{ b -> b != a && a.intersects(b)} })
            throw Exception("Invariant violated: Intersecting spaces @ $id")
        if(c.boxes.any { a -> c.boxes.any{ b -> b != a && a.intersects(b)} })
            throw Exception("Invariant violated: Intersecting boxes @ $id")
    }
}

interface MSSMove
data class CrossContainerMove(val sourceContainer: Int, val sourceBox: Int, val targetContainer: Int, val targetSpace: Int) : MSSMove
data class LocalMove(val container: Int, val box: Int, val space: Int) : MSSMove
data class RepackMove(val sourceContainer: Int, val sourceBox: Int, val targetContainer: Int, val targetSpace: Int) : MSSMove