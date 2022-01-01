package ui

import binpack.Algorithm
import binpack.BinPackProblem
import binpack.BinPackSolution
import binpack.BoxGenerator
import binpack.configurations.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.asList
import viz.Visualizer
import kotlin.math.max
import kotlin.math.round
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

object UIState {
    object GeneratorOptions {
        var seed = 1337
        var containerSize = 20
        var boxCount = 3
        var minW = 3
        var maxW = 14
        var minH = 3
        var maxH = 14
    }

    private var runtime = 0L
    var running = false
    var stepSize = 1
    var minFrameDelay = 100

    val algorithms = listOf<Algorithm>(GreedyOnlineNPFF, GreedyOnlineNPCT, GreedyOnlineNPCTBF, GreedyAreaDescNPFF, GreedyAreaDescNPCT, GreedyAreaDescNPCTBF, LocalSearchFirstFit, LocalSearchCircTouch)
    var activeAlgorithm = algorithms[0]

    lateinit var instance: BinPackProblem
    lateinit var solution: BinPackSolution
    lateinit var visualizer: Visualizer<BinPackSolution>

    private fun newInstance() = BoxGenerator.getProblemInstance(
            GeneratorOptions.containerSize,
            GeneratorOptions.minW,
            GeneratorOptions.maxW,
            GeneratorOptions.minH,
            GeneratorOptions.maxH,
            GeneratorOptions.boxCount,
            GeneratorOptions.seed
    )

    @OptIn(ExperimentalTime::class)
    fun tick() {
        val elapsed = measureTime {
            val res = activeAlgorithm.optimizeStep(stepSize)
            solution = res.first
            visualizer.refresh(solution)
            updateStats()

            if(res.second)
                stop()
        }

        runtime += elapsed.inWholeMilliseconds
        val delay = max(5, minFrameDelay - elapsed.inWholeMilliseconds).toInt()
        if(running)
            window.setTimeout({ tick() }, delay)
    }

    fun stop() {
        running = false
        (document.getElementById("btnRun") as HTMLButtonElement).innerText = "run"
    }

    fun updateStats() {
        val containers = solution.containers.size.toString()
        val k1 = (round((solution.k1PackDensity() * 1000)) / 10).toString()
        val runtime = (round(runtime.toDouble() / 100) / 10).toString()

        document.getElementById("statsNumContainers")!!.innerHTML = containers
        document.getElementById("statsK1Density")!!.innerHTML = k1
        document.getElementById("statsRuntime")!!.innerHTML = runtime

        val statListEntry = document.getElementById("statEntry-${activeAlgorithm.name}")!!
        statListEntry.innerHTML = "${containers}C / $k1% / ${runtime}s"
        statListEntry.classList.remove("invisible")
    }

    fun setActiveAlgorithm(index: Int) {
        activeAlgorithm = algorithms[index]
        reset()
    }

    fun refreshInstance() {
        instance = newInstance()
        document.getElementById("statsLowerBound")!!.innerHTML = instance.lowerBound.toString()

        document.getElementById("statsByAlgo")!!.children.asList().forEach { it.classList.add("invisible") }

        reset()
    }

    fun reset() {
        stop()
        runtime = 0L
        activeAlgorithm.init(instance)
        solution = BinPackSolution(instance.containerSize, emptyList())
        visualizer.refresh(solution)
    }
}

