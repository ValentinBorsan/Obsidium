package net.valentin.obsidium.block;

import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public interface PipeFacadeHolder {
    @Nullable
    Block getFacadeBlock();

    void setFacadeBlock(@Nullable Block block);
}

