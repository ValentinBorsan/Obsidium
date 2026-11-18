package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Common configuration contract for Naiad Fluid Pipes and Volcanite Energy Wires.
 */
public interface PipeConfigurable {
    int KIND_FLUID = 0;
    int KIND_ENERGY = 1;

    int REDSTONE_ALWAYS = 0;
    int REDSTONE_HIGH = 1;
    int REDSTONE_LOW = 2;
    int REDSTONE_OFF = 3;

    int getPipeKind();

    int getPipeColor(); // -1 = default, 0..15 = DyeColor id
    void setPipeColor(int color);

    int getRedstoneMode();
    void setRedstoneMode(int mode);

    boolean isRoundRobin();
    void setRoundRobin(boolean roundRobin);

    int getThroughputPercent(); // 25, 50, 75, 100
    void setThroughputPercent(int percent);

    int getFaceModeOrdinal(Direction dir); // 0=NORMAL/NONE, 1=EXTRACT, 2=INSERT
    void cycleFaceMode(Direction dir);

    boolean isQuantumLinked();
    DyeColor getQuantumColor();
    void setQuantumColor(DyeColor color);
    int getQuantumChannel();
    void setQuantumChannel(int channel);
    boolean isQuantumPrivate();
    @Nullable UUID getQuantumOwner();
    default boolean canConfigure(UUID player) {
        return !isQuantumPrivate() || getQuantumOwner() == null || getQuantumOwner().equals(player);
    }
    void setQuantumPrivate(boolean priv);
    void ensureQuantumOwner(@Nullable UUID owner);

    @Nullable
    Container getFilterContainer();

    default boolean isRedstoneAllowed(Level level, BlockPos pos) {
        return switch (getRedstoneMode()) {
            case REDSTONE_HIGH -> level.hasNeighborSignal(pos);
            case REDSTONE_LOW -> !level.hasNeighborSignal(pos);
            case REDSTONE_OFF -> false;
            default -> true;
        };
    }

    static int nextThroughput(int current) {
        return switch (current) {
            case 25 -> 50;
            case 50 -> 75;
            case 75 -> 100;
            default -> 25;
        };
    }
}
