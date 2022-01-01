package binpack.configurations

import algorithms.greedy.GreedyPacker
import binpack.Algorithm
import binpack.BinPackProblem
import binpack.BinPackSolution
import binpack.Box
import binpack.greedy.*

abstract class GenericGreedyConfig : Algorithm() {
    protected lateinit var packer: GreedyPacker<Box,Int,BinPackSolution>
    override fun optimize() = packer.optimize()
    override fun optimizeStep(limit: Int) = packer.optimizeStep(limit)
}

object GreedyOnlineNPFF : GenericGreedyConfig() {
    override val name = "Greedy-Online-NormalPosFirstFit"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(OnlineOrdering, NormalPosFirstFitPacker, instance.containerSize, instance.boxes)
    }
}

object GreedyOnlineNPCT : GenericGreedyConfig() {
    override val name = "Greedy-Online-NormalPosCircTouch"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(OnlineOrdering, NormalPosCircTouchPacker, instance.containerSize, instance.boxes)
    }
}

object GreedyAreaDescNPFF : GenericGreedyConfig() {
    override val name = "Greedy-AreaDesc-NormalPosFirstFit"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(AreaDescOrdering, NormalPosFirstFitPacker, instance.containerSize, instance.boxes)
    }
}

object GreedyAreaDescNPCT : GenericGreedyConfig() {
    override val name = "Greedy-AreaDesc-NormalPosCircTouch"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(AreaDescOrdering, NormalPosCircTouchPacker, instance.containerSize, instance.boxes)
    }
}

object GreedyAreaDescNPCTBF : GenericGreedyConfig() {
    override val name = "Greedy-AreaDesc-NormalPosCircTouch-BestFit"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(AreaDescOrdering, GoldStandardPacker(instance), instance.containerSize, instance.boxes)
    }
}

object GreedyOnlineNPCTBF : GenericGreedyConfig() {
    override val name = "Greedy-Online-NormalPosCircTouch-BestFit"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(OnlineOrdering, GoldStandardPacker(instance), instance.containerSize, instance.boxes)
    }
}