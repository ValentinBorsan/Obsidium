package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

public class PoseidonAbyssalCasingBlockEntity extends BlockEntity {
    private @Nullable BlockPos controllerPos = null;

    public PoseidonAbyssalCasingBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.POSEIDON_ABYSSAL_CASING_BE, pos, state);
    }

    public void setControllerPos(@Nullable BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
    }

    public @Nullable BlockPos getControllerPos() {
        return controllerPos;
    }

    public @Nullable PoseidonAbyssalPumpBlockEntity getController() {
        if (level == null || controllerPos == null) return null;
        if (!level.isLoaded(controllerPos)) return null;
        if (level.getBlockEntity(controllerPos) instanceof PoseidonAbyssalPumpBlockEntity pump && pump.isFormed()) {
            return pump;
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controllerPos != null) {
            tag.putLong("ControllerPos", controllerPos.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ControllerPos")) {
            controllerPos = BlockPos.of(tag.getLong("ControllerPos"));
        } else {
            controllerPos = null;
        }
    }
}
