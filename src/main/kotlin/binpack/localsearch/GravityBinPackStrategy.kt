package binpack.localsearch

import algorithms.localsearch.LocalSearchStrategy
import binpack.BinPackProblem
import kotlin.math.pow

object GravityBinPackStrategy : LocalSearchStrategy<BinPackProblem, BinPackProblem, GravityPackMove> {
    private lateinit var instance: BinPackProblem

    override fun init(instance: BinPackProblem) {
        this.instance = instance
    }

    override fun initialSolution() = instance

    override fun neighboringSolutions(solution: BinPackProblem): Iterable<GravityPackMove> {
        val rotateMoves = solution.boxes.indices.reversed().map { i -> RotateMove(i) }
        val swapMoves = solution.boxes.indices.reversed().flatMap { i ->
            (0 until i).map { j -> SwapMove(i, j) }
        }
        return rotateMoves + swapMoves
    }

    override fun applyMove(solution: BinPackProblem, move: GravityPackMove): BinPackProblem {
        val newBoxes = solution.boxes.toMutableList()
        when(move) {
            is RotateMove -> newBoxes.add(move.index, newBoxes.removeAt(move.index).rotate())

            is SwapMove -> {
                val tmp = newBoxes[move.a]
                newBoxes[move.a] = newBoxes[move.b]
                newBoxes[move.b] = tmp
            }
            else -> throw Exception("Unknown GravityBinPack move")
        }

        return BinPackProblem(solution.containerSize, newBoxes)
    }

    override fun scoreMove(solution: BinPackProblem, move: GravityPackMove) = scoreSolution(applyMove(solution, move))

    override fun scoreSolution(solution: BinPackProblem): Double {
        val size = solution.containerSize
        val containerStats = mutableListOf<Int>()
        var containerIndex = 0
        var containerFill = 0
        var segments = listOf(Pair(0,0))

        solution.boxes.forEach { box ->
            // place box at lowest height as far back as it fits
            //console.log(segments.joinToString { "(${it.first},${it.second})" })
            var minX = segments.filter { it.second < box.h }.maxOf { it.first }
            var offset = 0
            var segmentIndex = 1

            // if it doesn't fit, try sliding up
            while(minX + box.w > size) {
                offset = segments[segmentIndex].second

                // Start new container if no fit was found
                if(offset + box.h > size) {
                    containerStats.add(containerFill) // containerUtilization(size, segments, containerIndex, viz)
                    segmentIndex = 0
                    containerIndex++
                    containerFill = 0
                    minX = 0
                    offset = 0
                    segments = listOf(Pair(0,0))
                }
                else {
                    minX = segments.filter { it.second >= offset && it.second < box.h + offset }.maxOf { it.first }
                    segmentIndex++
                }
            }

            // update segments
            val lastUsedSegment = segments.filter { it.second >= offset && it.second <= box.h + offset }.maxByOrNull { it.second }!!
            segments = listOf(Pair(minX + box.w, offset), Pair(lastUsedSegment.first, offset + box.h)) + segments.filter{ it.second < offset || it.second > box.h + offset }
            segments = segments.sortedBy { it.second }

            containerFill += box.area
        }

        containerStats.add(containerFill) // containerUtilization(size, segments, containerIndex, viz)
        return containerStats.size * 1000 - containerUtilization(size, segments) * 100 - containerStats.map { fill -> fill.toDouble() / (size*size) }.mapIndexed { i, fill -> fill * (100/(2.0).pow(i)) }.sum()
    }

    private fun containerUtilization(size: Int, segments: List<Pair<Int,Int>>): Double {
        val totalArea = size * size
        var freeArea = 0
        val segments = segments.sortedBy { it.second }

        for(i in segments.indices) {
            val width = size - segments[i].first

            val height = if(i == segments.size - 1)
                    size - segments[i].second
                else
                    segments[i+1].second - segments[i].second

            //viz?.freeCallback(width, height, segments[i].first, segments[i].second, ci)
            freeArea += width * height
        }

        return freeArea.toDouble() / totalArea
    }
}

interface GravityPackMove

data class RotateMove(val index: Int) : GravityPackMove {
    override fun toString() = "[rotate $index]"
}
data class SwapMove(val a: Int, val b: Int) : GravityPackMove {
    override fun toString() = "[swap $a<->$b]"
}