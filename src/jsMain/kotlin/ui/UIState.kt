package ui

import binpack.Algorithm
import binpack.BinPackProblem
import binpack.BinPackSolution
import binpack.BoxGenerator
import binpack.configurations.*
import binpack.localsearch.OverlapSpaceStrategy
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.asList
import kotlin.math.max
import kotlin.math.round
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

actual object UIState {
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

    val algorithms = listOf<Algorithm>(
        GreedyOnlineNPFF,
        GreedyOnlineNPCT,
        GreedyOnlineNPCTBF,
        GreedyOnlineSpaceFF,
        GreedyAreaDescNPFF,
        GreedyAreaDescNPCT,
        GreedyAreaDescNPCTBF,
        GreedyAreaDescSpaceFF,
        GreedyAdaptiveBFSpace,
        LocalSearchLocalSequence,
        LocalSearchRepackSpace,
        LocalSearchRelaxedSpace)

    var activeAlgorithm = algorithms[0]

    lateinit var instance: BinPackProblem
    lateinit var solution: BinPackSolution
    lateinit var visualizer: Visualizer<BinPackSolution>
    actual var debugVisualizer: DebugVisualizer? = null

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

            if(res.second)
                stop()
        }

        runtime += elapsed.inWholeMilliseconds
        visualizer.refresh(solution)
        updateStats()

        val delay = max(5, minFrameDelay - elapsed.inWholeMilliseconds).toInt()
        if(running)
            window.setTimeout({ tick() }, delay)
    }

    @OptIn(ExperimentalTime::class)
    fun singleStep() {
        val elapsed = measureTime {
            val res = activeAlgorithm.optimizeStep(stepSize)
            solution = res.first
        }

        runtime += elapsed.inWholeMilliseconds
        visualizer.refresh(solution)
        updateStats()
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
        statListEntry.innerHTML = "<td>${activeAlgorithm.shortName}</td> <td>${containers}C</td> <td>$k1%</td> <td>${runtime}s</td>"
        statListEntry.classList.remove("invisible")
    }

    fun setActiveAlgorithm(index: Int) {
        activeAlgorithm = algorithms[index]

        visualizer.fillTranslucent = activeAlgorithm is LocalSearchRelaxedSpace

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
        visualizer.debugClear()
    }
}

