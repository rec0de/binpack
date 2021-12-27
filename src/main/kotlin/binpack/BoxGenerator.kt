package binpack

import kotlin.random.Random

object BoxGenerator {

    fun getProblemInstance(containerSize: Int, minW: Int, maxW: Int, minH: Int, maxH: Int, nBoxes: Int) = getProblemInstance(containerSize, minW, maxW, minH, maxH, nBoxes, Random.nextInt())

    fun getProblemInstance(containerSize: Int, minW: Int, maxW: Int, minH: Int, maxH: Int, nBoxes: Int, seed: Int): BinPackProblem {
        return BinPackProblem(containerSize, genBoxes(minW, maxW, minH, maxH, nBoxes, seed))
    }

    private fun genBoxes(minW: Int, maxW: Int, minH: Int, maxH: Int, nBoxes: Int, seed: Int): List<Box> {
        val r = Random(seed)
        val boxes = mutableListOf<Box>()
        for (i in (1..nBoxes)) {
            boxes.add(genBox(minW, maxW, minH, maxH, r))
        }
        return boxes;
    }

    private fun genBox(minW: Int, maxW: Int, minH: Int, maxH: Int, r: Random): Box {
        return Box(r.nextInt(minW, maxW+1), r.nextInt(minH, maxH+1))
    }
}

