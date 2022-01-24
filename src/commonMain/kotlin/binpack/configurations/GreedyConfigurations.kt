package binpack.configurations

import algorithms.greedy.GreedyPacker
import binpack.*
import binpack.greedy.*
import ui.UIState

abstract class GenericGreedyConfig : Algorithm() {
    protected lateinit var packer: GreedyPacker<Box,Int,BinPackSolution>
    override fun optimize() = packer.optimize()
    override fun optimizeStep(limit: Int) = packer.optimizeStep(limit)
}

object GreedyOnlineNPFF : GenericGreedyConfig() {
    override val name = "Greedy-Online-NormalPosFirstFit"
    override val shortName = "Gr. Online NPFF"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(OnlineOrdering, NormalPosFirstFitPacker, instance.containerSize, instance.boxes)
    }
}

object GreedyOnlineNPCT : GenericGreedyConfig() {
    override val name = "Greedy-Online-NormalPosCircTouch"
    override val shortName = "Gr. Online CircTouch"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(OnlineOrdering, NormalPosCircTouchPacker, instance.containerSize, instance.boxes)
    }
}

object GreedyAreaDescNPFF : GenericGreedyConfig() {
    override val name = "Greedy-AreaDesc-NormalPosFirstFit"
    override val shortName = "Gr. AreaDesc NPFF"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(AreaDescOrdering, NormalPosFirstFitPacker, instance.containerSize, instance.boxes)
    }
}

object GreedyAreaDescNPCT : GenericGreedyConfig() {
    override val name = "Greedy-AreaDesc-NormalPosCircTouch"
    override val shortName = "Gr. AreaDesc CircTouch"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(AreaDescOrdering, NormalPosCircTouchPacker, instance.containerSize, instance.boxes)
    }
}

object GreedyAreaDescNPCTBF : GenericGreedyConfig() {
    override val name = "Greedy-AreaDesc-NormalPosCircTouch-BestFit"
    override val shortName = "Gr. AreaDesc CircTouch BF"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(AreaDescOrdering, GoldStandardPacker(instance), instance.containerSize, instance.boxes)
    }
}

object GreedyOnlineNPCTBF : GenericGreedyConfig() {
    override val name = "Greedy-Online-NormalPosCircTouch-BestFit"
    override val shortName = "Gr. Online CircTouch BF"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(OnlineOrdering, GoldStandardPacker(instance), instance.containerSize, instance.boxes)
    }
}

object GreedyAreaDescSpaceFF : GenericGreedyConfig() {
    override val name = "Greedy-AreaDesc-SpaceFF"
    override val shortName = "Gr. AreaDesc SpaceFF"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(AreaDescOrdering, SpaceFirstFitPacker, instance.containerSize, instance.boxes)
    }
}

object GreedyOnlineSpaceFF : GenericGreedyConfig() {
    override val name = "Greedy-Online-SpaceFF"
    override val shortName = "Gr. Online SpaceFF"
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(OnlineOrdering, SpaceFirstFitPacker, instance.containerSize, instance.boxes)
    }
}

object GreedyAdaptiveBFSpace : Algorithm() {
    private lateinit var packer: GreedyPacker<Box,Int,ContainerSolution<SpaceContainer>>
    override val name = "Greedy-AdaptiveBF-Space"
    override val shortName = "Gr. AdaptiveBF Space"
    override fun optimize() = packer.optimize()
    override fun optimizeStep(limit: Int) = packer.optimizeStep(limit)
    override fun init(instance: BinPackProblem) {
        packer = GreedyPacker(AdaptiveBestFitOrdering, SpaceFirstFitPacker, instance.containerSize, instance.boxes)
    }
}