class Stats {
    private val executions = mutableListOf<StatEntry>()

    fun add(entry: StatEntry) = executions.add(entry)

    val avgK1: Double
        get() = executions.sumOf{ it.k1 } / executions.size
    
    val avgRuntime: Long
        get() = executions.sumOf{ it.runtime } / executions.size

    val containersAboveLowerBound: Int
        get() = executions.sumOf{ it.containers - it.lowerBound }

    val runtimeByBoxCount: Map<Int,Double>
        get() = executions.groupBy { it.boxCount }.mapValues { entry -> entry.value.sumOf { it.runtime }.toDouble() / entry.value.size }

    val surplusByBoxCount: Map<Int,Int>
        get() = executions.groupBy { it.boxCount }.mapValues { entry -> entry.value.sumOf { it.containers - it.lowerBound } }
}

data class StatEntry(val boxCount: Int, val k1: Double, val runtime: Long, val containers: Int, val lowerBound: Int)