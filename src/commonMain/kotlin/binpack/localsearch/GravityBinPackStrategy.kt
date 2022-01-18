package binpack.localsearch

import algorithms.localsearch.LocalSearchStrategy
import binpack.BinPackProblem
import kotlin.math.pow

interface GravityPackMove

data class RotateMove(val index: Int) : GravityPackMove {
    override fun toString() = "[rotate $index]"
}
data class SwapMove(val a: Int, val b: Int) : GravityPackMove {
    override fun toString() = "[swap $a<->$b]"
}