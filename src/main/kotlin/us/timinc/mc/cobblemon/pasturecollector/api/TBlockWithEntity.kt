package us.timinc.mc.cobblemon.pasturecollector.api

import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

abstract class TBlockWithEntity<T : BlockEntity>(settings: Settings) : BlockWithEntity(settings) {
    fun getBlockEntity(serverWorld: ServerWorld, blockPos: BlockPos): T {
        val blockEntity = serverWorld.getBlockEntity(blockPos) as T
        return blockEntity
    }
}