package binpack.greedy

import algorithms.greedy.GreedyPackStrategy
import binpack.BinPackSolution
import binpack.Box
import binpack.PlacedBox

abstract class GenericBinPacker : GreedyPackStrategy<Box, Int, BinPackSolution> {
    protected var size = 0
    protected var containerIndex = 0
    protected var containers = mutableListOf<MutableList<PlacedBox>>()

    override fun init(constraints: Int) {
        reset()
        size = constraints
    }

    override fun initialSolution(): BinPackSolution {
        reset()
        return BinPackSolution(size, containers)
    }

    open fun reset() {
        containers.clear()
        containerIndex = 0
    }

    fun getSolution(): BinPackSolution {
        return BinPackSolution(size, containers)
    }
}