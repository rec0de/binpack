package binpack.greedy

import algorithms.greedy.GreedyPackStrategy
import binpack.*

abstract class GenericBinPacker<C : Container>(private val highEffort: Boolean, private val lowerBound: Int = 0) : GreedyPackStrategy<Box, Int, BinPackSolution> {
    protected var size = 0
    private var containerIndex = 0
    private val containers = mutableListOf<C>()
    private val availableContainers = mutableListOf<C>()

    override fun init(constraints: Int) {
        reset()
        size = constraints
    }

    override fun initialSolution(): ContainerSolution<C> {
        reset()
        return ContainerSolution(size, containers)
    }

    override fun packItem(item: Box): BinPackSolution = if(highEffort) packItemBestFit(item) else packItemFirstFit(item)

    private fun packItemFirstFit(item: Box): ContainerSolution<C> {
        var placed: PlacedBox? = null
        var ci = 0
        var i = 0

        while(placed == null && i < availableContainers.size) {
            val tryContainer = availableContainers[i]
            placed = packIntoContainer(item, tryContainer)?.first
            ci = tryContainer.ci
            i += 1
        }

        // Start new container if no fit found
        if(placed == null) {
            ci = containers.size
            val container = createEmptyContainer(ci, size)
            containers.add(container)
            availableContainers.add(container)
            placed = packIntoContainer(item, containers[ci])!!.first
        }

        containers[ci].add(placed)

        if(!containers[ci].hasAccessibleSpace)
            availableContainers.remove(containers[ci])

        return ContainerSolution(size, containers)
    }

    private fun packItemBestFit(item: Box): ContainerSolution<C> {
        val placed = availableContainers.map { Pair(packIntoContainer(item, it), it.ci) }.maxByOrNull { it.first?.second ?: Double.NEGATIVE_INFINITY }
        val ci: Int

        // Start new container if no fit found
        if(placed?.first == null) {
            val container = createEmptyContainer(containers.size, size)
            ci = containers.size
            containers.add(container)
            availableContainers.add(container)
            container.add(packIntoContainer(item, container)!!.first)
        }
        else {
            ci = placed.second
            containers[ci].add(placed.first!!.first)
        }

        if(!containers[ci].hasAccessibleSpace)
            availableContainers.remove(containers[ci])

        return ContainerSolution(size, containers)
    }

    abstract fun packIntoContainer(item: Box, container: C): Pair<PlacedBox,Double>?

    abstract fun createEmptyContainer(ci: Int, size: Int): C

    open fun reset() {
        containers.clear()
        availableContainers.clear()
        containerIndex = 0

        // Initialize number of containers equal to lower bound
        (0 until lowerBound).forEach {
            val c = createEmptyContainer(it, size)
            containers.add(c)
            availableContainers.add(c)
        }
    }

    fun getSolution(): ContainerSolution<C> {
        return ContainerSolution(size, containers)
    }
}