package algorithms.localsearch

interface LocalSearchStrategy<Problem, Solution, Move> {
    fun init(instance: Problem)
    fun initialSolution(): Solution
    fun neighboringSolutions(solution: Solution): Iterable<Move>
    fun scoreMove(solution: Solution, move: Move): Double
    fun applyMove(solution: Solution, move: Move): Solution
    fun scoreSolution(solution: Solution): Double
}