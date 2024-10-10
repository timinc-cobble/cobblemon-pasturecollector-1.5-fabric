package us.timinc.mc.cobblemon.pasturecollector.api

import com.cobblemon.mod.common.util.toVec3d
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

object Loot {
    fun generateLoot(identifier: Identifier, world: ServerWorld, blockPos: BlockPos): ObjectArrayList<ItemStack> {
        val vecPos = blockPos.toVec3d()
        val lootManager = world.server.lootManager
        val lootTable = lootManager.getLootTable(identifier)
        return lootTable.generateLoot(
            LootContextParameterSet(
                world,
                mapOf(
                    LootContextParameters.ORIGIN to vecPos
                ),
                mapOf(),
                0F
            )
        )
    }
}