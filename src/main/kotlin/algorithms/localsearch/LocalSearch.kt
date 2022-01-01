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
        val explorationLimit = 200
        val noImprovementLimit = 5

        while(step < stepLimit) {
            // find best neighboring solution
            var consideredMoves = strategy.neighboringSolutions(solution).shuffled()

            if(consideredMoves.size > explorationLimit)
                consideredMoves = consideredMoves.subList(0, explorationLimit)

            val bestNextStep = consideredMoves.minByOrNull { strategy.scoreMove(solution, it) } ?: break // break if no next step
            val newSolution = strategy.applyMove(solution, bestNextStep)
            val newCost = strategy.scoreSolution(newSolution)

            // break if no improvement
            if(newCost >= bestCost) {
                noImprovement++
                console.log("No improvement for $noImprovement moves, limit is $noImprovementLimit")
                if(noImprovement == noImprovementLimit)
                    return Pair(solution, true)
                else
                    continue
            }

            console.log("Applied move: $bestNextStep")

            // update solution
            solution = newSolution
            bestCost = newCost
            step++
            noImprovement = 0
        }

        return Pair(solution, false)
    }
}