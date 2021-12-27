package algorithms.localsearch

interface LocalSearchStrategy<Problem, Solution, Move> {
    fun initialSolution(instance: Problem): Solution
    fun neighboringSolutions(solution: Solution): Iterable<Move>
    fun applyMove(solution: Solution, move: Move): Solution
    fun scoreSolution(solution: Solution): Double
}