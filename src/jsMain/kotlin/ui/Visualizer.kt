package ui

import binpack.PlacedBox
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.max
import kotlin.math.min

abstract class Visualizer<Solution> : DebugVisualizer {
    protected var displaySize = 20.0
    protected var rowSize = 1
    protected var scale = 10.0
    protected abstract val ctx: CanvasRenderingContext2D
    protected abstract val debug: CanvasRenderingContext2D
    private var debugDisabled = false

    abstract fun render(solution: Solution)

    abstract fun refresh(solution: Solution)

    protected fun layout(numContainers: Int, containerSize: Int) {
        rowSize = 1
        var rowCount = 1

        while(rowSize * rowCount < numContainers) {
            rowSize++
            rowCount = max(1, ctx.canvas.height / (ctx.canvas.width / rowSize))
        }

        val displaySizeRow = max(20, (ctx.canvas.width / rowSize) - 20)
        val displaySizeCol = max(20, (ctx.canvas.height / rowCount) - 20)

        displaySize = min(displaySizeCol, displaySizeRow).toDouble()
        scale = displaySize / containerSize
    }

    protected fun renderContainer(sx: Double, sy: Double) {
        ctx.strokeStyle = "#000"
        ctx.strokeRect(sx, sy, displaySize, displaySize)
    }

    protected fun renderBox(box: PlacedBox, sx: Double, sy: Double) {
        ctx.fillRect(sx + box.x * scale, sy + box.y * scale, box.w * scale, box.h * scale)
        ctx.strokeRect(sx + box.x * scale, sy + box.y * scale, box.w * scale, box.h * scale)
    }

    fun setDebug(flag: Boolean) {
        debugDisabled = flag
    }

    override fun debugClear() {
        debug.clearRect(0.0, 0.0, debug.canvas.width.toDouble(), debug.canvas.height.toDouble())
    }

    override fun debugBox(box: PlacedBox, ci: Int) {
        if(debugDisabled)
            return
        val row = ci / rowSize
        val col = ci % rowSize
        val sx = (displaySize + 20) * col
        val sy = (displaySize + 20) * row
        debug.fillStyle = "#00aa00"
        debug.strokeStyle = "#004400"
        debug.fillRect(sx + box.x * scale, sy + box.y * scale, box.w * scale, box.h * scale)
        debug.strokeRect(sx + box.x * scale, sy + box.y * scale, box.w * scale, box.h * scale)
    }

    override fun debugLine(sx: Int, sy: Int, ex: Int, ey: Int, ci: Int) {
        if(debugDisabled)
            return
        val row = ci / rowSize
        val col = ci % rowSize
        val ox = (displaySize + 20) * col
        val oy = (displaySize + 20) * row
        debug.strokeStyle = "#0000ff"
        debug.beginPath()
        debug.moveTo(ox + sx * scale, oy + sy * scale)
        debug.lineTo(ox + ex * scale, oy + ey * scale)
        debug.stroke()
    }
}