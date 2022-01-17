package binpack.localsearch

import algorithms.localsearch.LocalSearchStrategy
import binpack.*
import kotlin.math.max

class MaximalSpaceStrategy() : LocalSearchStrategy<BinPackProblem, SpaceContainerSolution, MSSMove> {
    private lateinit var instance: BinPackProblem

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
        val moves = mutableListOf<MSSMove>()
        val containers = solution.containerObjs
        val lowerIndex = max(0, containers.size - 100)

        containers.indices.forEach { source ->
            containers[source].boxes.forEachIndexed { boxi, box ->
                targetSearch@
                for (target in 0 until source) {
                    for (spacei in 0 until containers[target].spaces.size) {
                        val space = containers[target].spaces[spacei]
                        if((space.fits(box) || space.fits(box.rotate()))) {
                            moves.add(MoveBoxToSpace(source, boxi, target, spacei))
                            break@targetSearch
                        }
                    }
                }
            }
        }
        return moves
    }

    override fun deltaScoreMove(solution: SpaceContainerSolution, currentScore: Double, move: MSSMove): Double {
        return if(move is MoveBoxToSpace) {
            val boxArea = solution.containerObjs[move.sourceContainer].boxes[move.sourceBox].area
            boxArea * (1.0 / move.sourceContainer - 1.0 / move.targetContainer)
        }
        else
            0.0
    }

    override fun applyMove(solution: SpaceContainerSolution, move: MSSMove): SpaceContainerSolution {
        if(move is MoveBoxToSpace) {
            val newContainers = solution.containerObjs.toMutableList()

            val newSource = newContainers[move.sourceContainer].clone()
            val newTarget = newContainers[move.targetContainer].clone()

            val movedBox = newSource.boxes[move.sourceBox]
            newSource.remove(movedBox)
            newTarget.add(movedBox, newTarget.spaces[move.targetSpace])

            newContainers[move.targetContainer] = newTarget

            if(newSource.boxes.isEmpty())
                newContainers.removeAt(move.sourceContainer)
            else
                newContainers[move.sourceContainer] = newSource

            return SpaceContainerSolution(solution.containerSize, newContainers)
        }
        return solution
    }

    override fun scoreSolution(solution: SpaceContainerSolution): Double {
        return solution.containerObjs.mapIndexed { index, container -> container.freeSpace.toDouble() * (1.0 / (index+1))}.sum()
    }
}

interface MSSMove

data class MoveBoxToSpace(val sourceContainer: Int, val sourceBox: Int, val targetContainer: Int, val targetSpace: Int) : MSSMove