package us.timinc.mc.cobblemon.pasturecollector

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntityType
import us.timinc.mc.cobblemon.pasturecollector.api.OmniModule
import us.timinc.mc.cobblemon.pasturecollector.blocks.PastureCollectorBlock
import us.timinc.mc.cobblemon.pasturecollector.blocks.PastureCollectorBlockEntity
import us.timinc.mc.cobblemon.pasturecollector.config.PastureCollectorConfig

object PastureCollector : OmniModule<PastureCollectorConfig>(
    "pasturecollector",
    PastureCollectorConfig::class.java
) {
    @Suppress("MemberVisibilityCanBePrivate")
    val PASTURECOLLECTOR_BLOCK_: Block = PastureCollectorBlock(FabricBlockSettings.copy(Blocks.STONE))
    lateinit var PASTURECOLLECTOR_BLOCKENTITYTYPE: BlockEntityType<PastureCollectorBlockEntity>

    override fun register() {
        PASTURECOLLECTOR_BLOCKENTITYTYPE =
            registerBlockEntity(PASTURECOLLECTOR_BLOCK_, "nest", ::PastureCollectorBlockEntity)
        registerBlock(PASTURECOLLECTOR_BLOCK_, "pasture_collector")
    }
}