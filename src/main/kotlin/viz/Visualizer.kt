package viz

import binpack.PlacedBox
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.math.max
import kotlin.math.min

abstract class Visualizer<Solution> {
    protected var displaySize = 20.0
    protected var rowSize = 1
    protected var scale = 10.0
    protected abstract val ctx: CanvasRenderingContext2D

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
}