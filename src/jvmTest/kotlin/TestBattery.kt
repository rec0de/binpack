import binpack.Algorithm
import binpack.BinPackProblem
import binpack.BinPackSolution
import binpack.BoxGenerator
import binpack.configurations.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.test.Test

class TestBattery {

    private val algorithms = listOf<Algorithm>(GreedyOnlineNPFF, GreedyOnlineNPCT, GreedyOnlineNPCTBF, GreedyOnlineSpaceFF, GreedyAreaDescNPFF, GreedyAreaDescNPCT, GreedyAreaDescNPCTBF, GreedyAreaDescSpaceFF, LocalSearchLocalSequence, LocalSearchMaximalSpace)
    private val configurations = listOf(
        BoxGenerator.ProblemSpecification(20, 1, 7, 1, 7, 200, 10),
        BoxGenerator.ProblemSpecification(20, 2, 9, 2, 9, 1000, 5),
        BoxGenerator.ProblemSpecification(40, 5, 20, 5, 20, 500, 5),
        BoxGenerator.ProblemSpecification(40, 1, 5, 2, 30, 500, 5),
        BoxGenerator.ProblemSpecification(100, 1, 100, 1, 100, 1000, 5),
        BoxGenerator.ProblemSpecification(10, 1, 6, 2, 6, 3000, 5)
    )

    private val stats = mutableMapOf<String, Stats>()

    @Test
    fun runAll() {
        configurations.forEach { config ->
            (0 until config.sample).forEach { iteration ->
                Logger.log("Running benchmark on $config with seed ${config.defaultSeed + iteration}")
                algorithms.forEach { algo ->
                    runSingle(algo, BoxGenerator.getProblemInstance(config, config.defaultSeed + iteration))
                }
            }
        }

        Logger.log("\n\nAggregated Results by Algorithm:")
        stats.forEach { (name, stats) ->
            Logger.log("\t$name")
            Logger.log("\t\tAvg K-1 Density  : ${stats.avgK1}")
            Logger.log("\t\tAvg Runtime      : ${stats.avgRuntime}ms")
            Logger.log("\t\tContainer Surplus: ${stats.containersAboveLowerBound}")
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun runSingle(algo: Algorithm, instance: BinPackProblem): Boolean {
        Logger.log("\t${algo.name}")
        val solution: BinPackSolution
        val elapsed = measureTime {
            algo.init(instance)
            solution = algo.optimize()
        }

        val valid = solution.verify() && solution.boxCount == instance.boxes.size

        Logger.log("\t\tRuntime    : ${elapsed.inWholeMilliseconds}ms")
        Logger.log("\t\tContainers : ${solution.containers.size}")
        Logger.log("\t\tK-1 Density: ${solution.k1PackDensity()}")
        Logger.log("\t\tValid: $valid")

        if(!valid) {
            Logger.log("INVALID SOLUTION")
            Logger.log("Problem size: ${instance.boxes.size}, Solution size: ${solution.boxCount}")
            val oob = solution.containers.any{ c -> c.any{ b -> b.outOfBounds(instance.containerSize) } }
            Logger.log("Out of bounds boxes?: $oob")
        }

        if(!stats.containsKey(algo.name))
            stats[algo.name] = Stats()

        val entry = StatEntry(solution.boxCount, solution.k1PackDensity(), elapsed.inWholeMilliseconds, solution.containers.size, instance.lowerBound)
        stats[algo.name]!!.add(entry)

        assert(valid)
        return valid
    }
}