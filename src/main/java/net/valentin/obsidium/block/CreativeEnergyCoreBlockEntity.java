package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

public class CreativeEnergyCoreBlockEntity extends VolcaniteEnergyCoreBlockEntity {
    public CreativeEnergyCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.CREATIVE_ENERGY_CORE_BE, pos, state);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntityType<?> getType() {
        return ObsidiumMod.CREATIVE_ENERGY_CORE_BE;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CreativeEnergyCoreBlockEntity entity) {
        if (level.isClientSide()) return;

        entity.energyStorage.amount = 900_000_000L;
        for (Direction dir : Direction.values()) {
            EnergyStorage target = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
            if (target != null) {
                EnergyStorageUtil.move(entity.energyStorage, target, 10_000_000L, null);
                entity.energyStorage.amount = 900_000_000L;
            }
        }
        if (!entity.inventory.isEmpty()) {
            net.minecraft.world.item.ItemStack stack = entity.inventory.getItem(0);
            SingleSlotStorage<ItemVariant> slot = InventoryStorage.of(entity.inventory, null).getSlot(0);
            ContainerItemContext ctx = ContainerItemContext.ofSingleSlot(slot);
            EnergyStorage itemStorage = EnergyStorage.ITEM.find(stack, ctx);
            if (itemStorage != null && itemStorage.supportsInsertion()) {
                EnergyStorageUtil.move(entity.energyStorage, itemStorage, 10_000_000L, null);
                entity.energyStorage.amount = 900_000_000L;
            }
        }
    }
}
