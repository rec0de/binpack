
import binpack.Algorithm
import binpack.BinPackProblem
import binpack.BinPackSolution
import binpack.configurations.GreedyAreaDescNPFF
import binpack.configurations.GreedyOnlineNPFF
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

object TestBattery {

    private val configurations = listOf<Algorithm>(GreedyOnlineNPFF, GreedyAreaDescNPFF)

    private fun log(msg: String) = console.log(msg)

    @OptIn(ExperimentalTime::class)
    private fun runSingle(config: Algorithm, instance: BinPackProblem): Boolean {
        log("Running ${config.name}...")
        val solution: BinPackSolution
        val elapsed = measureTime {
            solution = config.optimize(instance)
        }

        val valid = solution.verify()

        log("Runtime: ${elapsed.inWholeMilliseconds}ms")
        log("Containers: ${solution.containers.size}")
        log("K-1 Density: ${solution.k1PackDensity()}")
        log("Valid: $valid")
        return valid
    }
}