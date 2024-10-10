package us.timinc.mc.cobblemon.pasturecollector.api

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

abstract class OmniModule<T : Config>(val modId: String, val clazz: Class<T>) :
    ModInitializer {
    var logger: Logger = Logger.getLogger(modId)
    lateinit var config: T

    override fun onInitialize() {
        config = ConfigBuilder.load(clazz, modId)

        register()
    }

    abstract fun register()

    fun registerItem(item: Item, name: String): Item {
        return Registry.register(Registries.ITEM, modIdentifier(name), item)
    }

    fun registerBlock(block: Block, name: String, noItem: Boolean = false): Block {
        val registered = Registry.register(Registries.BLOCK, modIdentifier(name), block)
        if (!noItem) {
            Registry.register(
                Registries.ITEM, modIdentifier(name), BlockItem(block, FabricItemSettings())
            )
        }
        return registered
    }

    fun <F : BlockEntity> registerBlockEntity(
        block: Block, name: String, builder: (BlockPos, BlockState) -> F
    ): BlockEntityType<F> {
        return Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            modIdentifier(name),
            FabricBlockEntityTypeBuilder.create({ pos: BlockPos, state: BlockState ->
                builder(
                    pos, state
                )
            }, block).build(null)
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun modIdentifier(name: String): Identifier {
        return Identifier(modId, name)
    }

    fun debug(msg: String, runId: UUID) {
        if (!config.debug) return
        logger.log(Level.INFO, "${runId}: $msg")
    }

    fun getDebug(): (String) -> Unit {
        val runId = UUID.randomUUID()
        return { msg: String -> debug(msg, runId) }
    }
}