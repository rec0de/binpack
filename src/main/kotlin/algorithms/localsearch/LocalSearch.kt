package algorithms.localsearch

object LocalSearch {
    fun <Problem, Solution, Move> optimize(strategy: LocalSearchStrategy<Problem, Solution, Move>, instance: Problem): Solution {
        val solution = strategy.initialSolution(instance)
        return optimizeStep(strategy, solution, Int.MAX_VALUE)
    }

    fun <Problem, Solution, Move> optimizeStep(strategy: LocalSearchStrategy<Problem, Solution, Move>, partialSolution: Solution, stepLimit: Int): Solution {
        var solution = partialSolution
        var cost = strategy.scoreSolution(solution)
        var step = 0
        var noImprovement = 0
        val explorationLimit = 200
        val noImprovementLimit = 5

        while(step < stepLimit) {
            // find best neighboring solution
            val consideredMoves = strategy.neighboringSolutions(solution).shuffled().subList(0, explorationLimit)
            val bestNextStep = consideredMoves.minByOrNull { strategy.scoreSolution(strategy.applyMove(solution, it)) } ?: break // break if no next step
            val newSolution = strategy.applyMove(solution, bestNextStep)
            val newCost = strategy.scoreSolution(newSolution)

            // break if no improvement
            if(newCost >= cost) {
                noImprovement++
                console.log("No improvement for $noImprovement moves, limit is $noImprovementLimit")
                if(noImprovement == noImprovementLimit)
                    break
                else
                    continue
            }

            console.log("Applied move: $bestNextStep")

            // update solution
            solution = newSolution
            cost = newCost
            step++
            noImprovement = 0
        }

        return solution
    }
}