package binpack

/*
abstract class GeometricBinPackStrategy : LocalSearchStrategy<BinPackProblem, GeoBinPackSolution> {
    override fun initialSolution(instance: BinPackProblem): GeoBinPackSolution {
        val map = mutableMapOf<Box,BoxPosition>()
        var containerIndex = 0
        var containerXOffset = 0

        instance.boxes.forEach { box ->
            // choose y coordinate randomly
            val y = (0..(instance.containerSize - box.h)).random()

            if(containerXOffset + box.w > instance.containerSize) {
                containerIndex++
                containerXOffset = 0
            }

            map[box] = BoxPosition(containerIndex, containerXOffset, y)

            containerXOffset += box.w
        }

        return GeoBinPackSolution(map)
    }

    override fun scoreSolution(solution: GeoBinPackSolution): Double {
        return 0.0
    }
}

data class GeoBinPackSolution(val mapping: Map<Box,BoxPosition>)

data class BoxPosition(val container: Int, val x: Int, val y: Int)*/