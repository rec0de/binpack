import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import ui.UIState
import viz.BinPackVisualizer

fun main() {
    window.addEventListener("load", {
        val canvas = document.getElementById("c") as HTMLCanvasElement
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        val stepBtn = document.getElementById("btnStep") as HTMLButtonElement
        val runBtn = document.getElementById("btnRun") as HTMLButtonElement
        val resetBtn = document.getElementById("btnReset") as HTMLButtonElement
        val genBtn = document.getElementById("btnGenInstance") as HTMLButtonElement
        val statsBtn = document.getElementById("btnUpdateStats") as HTMLButtonElement
        val algoSelect = document.getElementById("inpAlgorithm") as HTMLSelectElement

        canvas.width = document.body!!.clientWidth

        UIState.visualizer = BinPackVisualizer(ctx)
        UIState.refreshInstance()

        val options = UIState.algorithms.map { it.name }
        options.forEach { algo ->
            val opt = document.createElement("option")
            opt.innerHTML = algo
            algoSelect.appendChild(opt)
        }

        algoSelect.onchange = {
            UIState.setActiveAlgorithm(algoSelect.selectedIndex)
        }

        stepBtn.onclick = {
            UIState.solution = UIState.activeAlgorithm.optimizeStep(1).first
            UIState.visualizer.refresh(UIState.solution)
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
            genOpt.boxCount = (document.getElementById("inpBoxCount") as HTMLInputElement).value.toInt()
            genOpt.containerSize = (document.getElementById("inpContainerSize") as HTMLInputElement).value.toInt()
            genOpt.minH = (document.getElementById("inpMinHeight") as HTMLInputElement).value.toInt()
            genOpt.minW = (document.getElementById("inpMinWidth") as HTMLInputElement).value.toInt()
            genOpt.maxH = (document.getElementById("inpMaxHeight") as HTMLInputElement).value.toInt()
            genOpt.maxW = (document.getElementById("inpMaxWidth") as HTMLInputElement).value.toInt()
            UIState.refreshInstance()
            0
        }

        statsBtn.onclick = { UIState.updateStats() }
        resetBtn.onclick = { UIState.reset() }
    })
}
