package viz

import algorithms.localsearch.LocalSearch
import binpack.*
import binpack.localsearch.GravityBinPackStrategy
import org.w3c.dom.CanvasRenderingContext2D

class GravityPackVisualizer(val instance: BinPackProblem, private val ctx: CanvasRenderingContext2D) {
    var solution = GravityBinPackStrategy.initialSolution(instance)
    private var containers: MutableList<MutableList<PlacedBox>> = mutableListOf()
    private val viz = BinPackVisualizer(ctx)

    init {
        GravityBinPackStrategy.scoreSolution(solution, this)
        viz.refresh(BinPackSolution(instance.containerSize, containers))
    }

    fun step(n: Int) {
        console.log("Single optimization step")
        console.log("base score: ${GravityBinPackStrategy.scoreSolution(solution)}")
        solution = LocalSearch.optimizeStep(GravityBinPackStrategy, solution, n).first
        render()
    }

    fun boxCallback(box: Box, x: Int, y: Int, c: Int) {
        if(containers.size == c)
            containers.add(mutableListOf())
        containers[c].add(box.asPlaced(x, y))
    }

    fun freeCallback(w: Int, h: Int, x: Int, y: Int, c: Int) {}

    fun render() {
        containers.clear()
        console.log("new score: ${GravityBinPackStrategy.scoreSolution(solution, this)}")
        viz.render(BinPackSolution(instance.containerSize, containers))
    }
}