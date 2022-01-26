package algorithms.localsearch

class LocalSearch<Problem, Solution, Move>(private val strategy: LocalSearchStrategy<Problem, Solution, Move>, instance: Problem) {

    private var solution: Solution
    private var bestCost: Double

    init {
        strategy.init(instance)
        solution = strategy.initialSolution()
        bestCost = strategy.scoreSolution(solution)
    }

    fun optimize() = optimizeStep(Int.MAX_VALUE).first

    fun optimizeStep(stepLimit: Int): Pair<Solution, Boolean> {
        var step = 0
        var noImprovement = 0
        val noImprovementLimit = 5

        while(step < stepLimit) {
            strategy.perIterationSharedSetup(solution)

            // find best neighboring solution
            val consideredMoves = strategy.neighboringSolutions(solution)

            val bestNextStep = consideredMoves.minByOrNull { strategy.deltaScoreMove(solution, bestCost, it) }
            val bestNextStepDelta = if(bestNextStep == null) 0.0 else strategy.deltaScoreMove(solution, bestCost, bestNextStep)

            // break if no improvement
            if(bestNextStepDelta >= 0) {
                noImprovement++
                //Logger.log("No improvement for $noImprovement moves, limit is $noImprovementLimit")
                if(noImprovement == noImprovementLimit)
                    return Pair(solution, true)
                else
                    continue
            }
            else {
                //Logger.log("Applied move: $bestNextStep for delta of $bestNextStepDelta")
                solution = strategy.applyMove(solution, bestNextStep!!)
                bestCost = strategy.scoreSolution(solution)
                Logger.log("Best cost: $bestCost")
                noImprovement = 0
            }

            step++
        }

        return Pair(solution, false)
    }
}