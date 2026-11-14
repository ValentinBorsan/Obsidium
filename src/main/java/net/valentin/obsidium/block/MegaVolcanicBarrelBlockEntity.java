package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

public class MegaVolcanicBarrelBlockEntity extends LiquidTankBlockEntity {
    public static final int CAPACITY = ReinforcedVolcanicLiquidBarrelBlockEntity.REINFORCED_BARREL_CAPACITY * 4;

    public MegaVolcanicBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.MEGA_VOLCANIC_BARREL_BE, pos, state);
        this.maxCapacity = CAPACITY;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.mega_volcanic_barrel");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new LiquidTankMenu(syncId, playerInventory, this.getInventory(), this.data);
    }
}
