import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import ui.BinPackVisualizer
import ui.UIState
import kotlin.random.Random
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun main() {
    window.addEventListener("load", {
        val canvas = document.getElementById("c") as HTMLCanvasElement
        val debug = document.getElementById("debug") as HTMLCanvasElement
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        val stepBtn = document.getElementById("btnStep") as HTMLButtonElement
        val runBtn = document.getElementById("btnRun") as HTMLButtonElement
        val resetBtn = document.getElementById("btnReset") as HTMLButtonElement
        val genBtn = document.getElementById("btnGenInstance") as HTMLButtonElement
        val genRanBtn = document.getElementById("btnGenInstanceRandom") as HTMLButtonElement
        val algoSelect = document.getElementById("inpAlgorithm") as HTMLSelectElement

        canvas.width = document.body!!.clientWidth
        debug.width = document.body!!.clientWidth

        UIState.visualizer = BinPackVisualizer(ctx, debug.getContext("2d") as CanvasRenderingContext2D)
        UIState.debugVisualizer = UIState.visualizer
        UIState.refreshInstance()

        (document.getElementById("btnDebugClear") as HTMLButtonElement).onclick = { UIState.visualizer.debugClear() }

        val options = UIState.algorithms.map { it.name }
        val statsContainer = document.getElementById("statsByAlgo") as HTMLTableSectionElement
        options.forEach { algo ->
            val opt = document.createElement("option")
            opt.innerHTML = algo
            algoSelect.appendChild(opt)

            val statEntry = document.createElement("tr")
            statEntry.id = "statEntry-$algo"
            statEntry.classList.add("invisible")
            statsContainer.appendChild(statEntry)
        }

        runBtn.onclick = {

            UIState.minFrameDelay = (document.getElementById("inpFrameDelay") as HTMLInputElement).value.toInt()
            UIState.stepSize = (document.getElementById("inpStepSize") as HTMLInputElement).value.toInt()

            if(UIState.running) {
                UIState.stop()
            }
            else {
                UIState.running = true
                runBtn.innerText = "stop"
                UIState.tick()
            }
        }

        genBtn.onclick = {
            val genOpt = UIState.GeneratorOptions
            genOpt.seed = (document.getElementById("inpSeed") as HTMLInputElement).value.toInt()
            genOpt.boxCount = (document.getElementById("inpBoxCount") as HTMLInputElement).value.toInt()
            genOpt.containerSize = (document.getElementById("inpContainerSize") as HTMLInputElement).value.toInt()
            genOpt.minH = (document.getElementById("inpMinHeight") as HTMLInputElement).value.toInt()
            genOpt.minW = (document.getElementById("inpMinWidth") as HTMLInputElement).value.toInt()
            genOpt.maxH = (document.getElementById("inpMaxHeight") as HTMLInputElement).value.toInt()
            genOpt.maxW = (document.getElementById("inpMaxWidth") as HTMLInputElement).value.toInt()
            UIState.refreshInstance()
            0
        }

        genRanBtn.onclick = {
            val genOpt = UIState.GeneratorOptions
            genOpt.seed = Random.nextInt()
            (document.getElementById("inpSeed") as HTMLInputElement).value = genOpt.seed.toString()
            genOpt.boxCount = (document.getElementById("inpBoxCount") as HTMLInputElement).value.toInt()
            genOpt.containerSize = (document.getElementById("inpContainerSize") as HTMLInputElement).value.toInt()
            genOpt.minH = (document.getElementById("inpMinHeight") as HTMLInputElement).value.toInt()
            genOpt.minW = (document.getElementById("inpMinWidth") as HTMLInputElement).value.toInt()
            genOpt.maxH = (document.getElementById("inpMaxHeight") as HTMLInputElement).value.toInt()
            genOpt.maxW = (document.getElementById("inpMaxWidth") as HTMLInputElement).value.toInt()
            UIState.refreshInstance()
            0
        }

        algoSelect.onchange = { UIState.setActiveAlgorithm(algoSelect.selectedIndex) }
        stepBtn.onclick = { UIState.singleStep() }
        resetBtn.onclick = { UIState.reset() }
    })
}

