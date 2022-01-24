package ui

import binpack.BinPackSolution
import org.w3c.dom.CanvasRenderingContext2D

class BinPackVisualizer(override val ctx: CanvasRenderingContext2D, override val debug: CanvasRenderingContext2D) : Visualizer<BinPackSolution>() {

    override fun refresh(solution: BinPackSolution) {
        layout(solution.containers.size, solution.containerSize)
        render(solution)
    }

    override fun render(solution: BinPackSolution) {
        ctx.clearRect(0.0, 0.0, ctx.canvas.width.toDouble(), ctx.canvas.height.toDouble())

        solution.containers.forEachIndexed { index, container ->
            val row = index / rowSize
            val col = index % rowSize
            val sx = (displaySize + 20) * col
            val sy = (displaySize + 20) * row
            renderContainer(sx, sy)

            ctx.strokeStyle = "#f00"
            ctx.fillStyle = "#a00"
            container.forEach { box ->
                renderBox(box, sx, sy)
            }
        }
    }
}