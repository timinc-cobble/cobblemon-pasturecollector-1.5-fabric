package us.timinc.mc.cobblemon.pasturecollector.blocks

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.giveOrDropItemStack
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.loot.LootDataType
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import net.minecraft.world.World
import us.timinc.mc.cobblemon.pasturecollector.PastureCollector
import us.timinc.mc.cobblemon.pasturecollector.PastureCollector.getDebug
import us.timinc.mc.cobblemon.pasturecollector.PastureCollector.modIdentifier
import us.timinc.mc.cobblemon.pasturecollector.api.ImplementedInventory
import us.timinc.mc.cobblemon.pasturecollector.api.Loot
import us.timinc.mc.cobblemon.pasturecollector.api.TBlockWithEntity
import kotlin.math.min


class PastureCollectorBlock(settings: Settings) : TBlockWithEntity<PastureCollectorBlockEntity>(settings) {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun randomTick(blockState: BlockState, serverWorld: ServerWorld, blockPos: BlockPos, random: Random) {

        attemptToGetDrop(serverWorld, blockPos)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getRenderType(blockState: BlockState?): BlockRenderType {
        return BlockRenderType.MODEL
    }

    override fun hasRandomTicks(blockState: BlockState): Boolean = true

    private fun attemptToGetDrop(serverWorld: ServerWorld, blockPos: BlockPos) {
        val entity = getBlockEntity(serverWorld, blockPos)
        entity.attemptToGetDrop(serverWorld, blockPos)
    }

    override fun createBlockEntity(blockPos: BlockPos, blockState: BlockState): BlockEntity =
        PastureCollectorBlockEntity(blockPos, blockState)

    override fun onBreak(world: World, blockPos: BlockPos, blockState: BlockState, playerEntity: PlayerEntity) {
        super.onBreak(world, blockPos, blockState, playerEntity)
        if (world !is ServerWorld) return
        val entity = getBlockEntity(world, blockPos)
        entity.spillContents(world)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onUse(
        blockState: BlockState,
        world: World,
        blockPos: BlockPos,
        playerEntity: PlayerEntity,
        hand: Hand,
        blockHitResult: BlockHitResult
    ): ActionResult {
        if (world !is ServerWorld) return ActionResult.SUCCESS
        val entity = getBlockEntity(world, blockPos)
        entity.giveContentsToPlayer(playerEntity)
        return ActionResult.SUCCESS
    }
}

class PastureCollectorBlockEntity(private val blockPos: BlockPos, blockState: BlockState) : BlockEntity(
    PastureCollector.PASTURECOLLECTOR_BLOCKENTITYTYPE, blockPos, blockState
), ImplementedInventory, SidedInventory {
    private var inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(1, ItemStack.EMPTY)

    private fun getNearbyPastures(): List<PokemonPastureBlockEntity> {
        val world = this.world ?: return emptyList()
        if (world.isClient) return emptyList()
        val serverWorld = world as ServerWorld

        val positionsToCheck = mutableListOf<BlockPos>(
            blockPos.north(), blockPos.east(), blockPos.south(), blockPos.west()
        )
        val listOfNearbyPastures = mutableListOf<PokemonPastureBlockEntity>()
        for (positionToCheck in positionsToCheck) {
            val targetBlockState = serverWorld.getBlockState(positionToCheck)
            val targetBlockEntity = serverWorld.getBlockEntity(positionToCheck)
            if (targetBlockState.block == CobblemonBlocks.PASTURE && targetBlockEntity is PokemonPastureBlockEntity) {
                listOfNearbyPastures.add(targetBlockEntity)
            }
        }
        return listOfNearbyPastures
    }

    private fun getSpeciesDropId(pokemon: Pokemon) =
        modIdentifier("gameplay/pasture_collector/species/${pokemon.species.resourceIdentifier.path}")

    fun attemptToGetDrop(world: ServerWorld, blockPos: BlockPos) {
        val debug = getDebug()
        debug("Attempting to get drop for $blockPos.")
        val lootManager = world.server.lootManager
        val pokemon = getNearbyPastures()
            .flatMap { pasture ->
                pasture.tetheredPokemon
                    .mapNotNull { it.getPokemon() }
                    .filter { pokemon ->
                        lootManager.getIds(LootDataType.LOOT_TABLES).contains(getSpeciesDropId(pokemon))
                    }
            }
            .randomOrNull()

        if (pokemon == null) {
            debug("No eligible Pokemon present.")
            return
        }

        debug("Picked ${pokemon.uuid}.")

        val list = Loot.generateLoot(getSpeciesDropId(pokemon), world, blockPos)
        if (list.isEmpty) {
            debug("Nothing dropped.")
            return
        }

        debug("Dropped: $list")
        list.forEach { stack ->
            addStackWhereFits(stack)
        }
        debug("Inventory: $inventory")
        debug("Leftover: $list")
        markDirty()

        val particle =
            if (list.any { !it.isEmpty }) ParticleTypes.CAMPFIRE_COSY_SMOKE else ParticleTypes.COMPOSTER
        world.spawnParticles(
            particle,
            blockPos.x.toDouble() + 0.5,
            blockPos.y.toDouble() + 1.0,
            blockPos.z.toDouble() + 0.5,
            3,
            0.1,
            0.0,
            0.1,
            0.0
        )
    }

    private fun addStackWhereFits(stackToAdd: ItemStack) {
        for (i in 0 until inventory.size) {
            val stackInSlot = inventory[i]
            if (stackInSlot.isEmpty) {
                // This is a good slot because empty, just dump it in here.
                setStack(i, stackToAdd.copyAndEmpty())
                return
            }
            if (!stackToAdd.isStackable) continue
            if (stackInSlot.item == stackToAdd.item) {
                // Might be a good candidate, same item type.
                if (stackInSlot.count >= stackInSlot.maxCount) continue
                // Has space, let's stack.
                val transferCount = min(stackToAdd.count, stackInSlot.maxCount - stackInSlot.count)
                stackToAdd.decrement(transferCount)
                stackInSlot.increment(transferCount)
                if (stackToAdd.isEmpty) return
            }
        }
    }

    override fun markDirty() {}

    override fun getAvailableSlots(direction: Direction): IntArray = intArrayOf(0)

    override fun canInsert(i: Int, itemStack: ItemStack?, direction: Direction?): Boolean = false

    override fun canExtract(i: Int, itemStack: ItemStack, direction: Direction): Boolean = true

    override fun getItems(): DefaultedList<ItemStack> = inventory

    override fun readNbt(nbt: NbtCompound) {
        inventory.clear()
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
    }

    public override fun writeNbt(nbt: NbtCompound) {
        Inventories.writeNbt(nbt, inventory)
        super.writeNbt(nbt)
    }

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener>? = BlockEntityUpdateS2CPacket.create(this)

    fun spillContents(world: World) {
        inventory.forEach { item ->
            world.spawnEntity(ItemEntity(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), item))
        }
        inventory.clear()
    }

    fun giveContentsToPlayer(playerEntity: PlayerEntity) {
        inventory.forEach(playerEntity::giveOrDropItemStack)
        inventory.clear()
    }
}