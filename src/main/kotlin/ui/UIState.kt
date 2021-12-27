package ui

import algorithms.greedy.GreedyPacker
import binpack.BinPackProblem
import binpack.BinPackSolution
import binpack.Box
import binpack.BoxGenerator
import binpack.greedy.AreaDescOrdering
import binpack.greedy.NormalPosFirstFitPacker
import kotlinx.browser.document
import kotlinx.browser.window
import viz.Visualizer
import kotlin.math.max
import kotlin.math.round
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

object UIState {
    object GeneratorOptions {
        var containerSize = 20
        var boxCount = 3
        var minW = 3
        var maxW = 14
        var minH = 3
        var maxH = 14
    }

    var running = false
    var stepSize = 1
    var minFrameDelay = 200

    lateinit var instance: BinPackProblem
    lateinit var solution: BinPackSolution
    lateinit var greedy: GreedyPacker<Box,Int,BinPackSolution>
    lateinit var visualizer: Visualizer<BinPackSolution>

    private fun newInstance() = BoxGenerator.getProblemInstance(
            GeneratorOptions.containerSize,
            GeneratorOptions.minW,
            GeneratorOptions.maxW,
            GeneratorOptions.minH,
            GeneratorOptions.maxH,
            GeneratorOptions.boxCount
    )

    @OptIn(ExperimentalTime::class)
    fun tick() {
        val elapsed = measureTime {
            solution = greedy.optimizeStep(stepSize)
            visualizer.refresh(solution)
            updateStats()
        }

        val delay = max(5, minFrameDelay - elapsed.inWholeMilliseconds).toInt()
        if(running)
            window.setTimeout({ tick() }, delay)
    }

    fun updateStats() {
        document.getElementById("statsLowerBound")!!.innerHTML = solution.lowerBound().toString()
        document.getElementById("statsNumContainers")!!.innerHTML = solution.containers.size.toString()
        document.getElementById("statsK1Density")!!.innerHTML = (round((solution.k1PackDensity() * 1000)) / 10).toString()
    }

    fun refreshInstance() {
        instance = newInstance()
        greedy = GreedyPacker(AreaDescOrdering, NormalPosFirstFitPacker, instance.containerSize, instance.boxes)
        solution = BinPackSolution(instance.containerSize, emptyList())
        visualizer.refresh(solution)
    }
}

