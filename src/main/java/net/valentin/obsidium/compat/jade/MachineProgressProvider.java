package net.valentin.obsidium.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.*;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Shows the current processing progress of Obsidium machines in the Jade overlay.
 * Energy (Team Reborn Energy) and item/fluid storage are already displayed natively by Jade.
 */
public enum MachineProgressProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "machine_progress");
    private static final String KEY_PROGRESS = "ObsidiumProgress";
    private static final String KEY_MAX = "ObsidiumMaxProgress";

    /** Returns the maximum progress of a supported machine, or 0 if unsupported. */
    static int maxProgress(BlockEntity be) {
        if (be instanceof VolcaniteGrinderBlockEntity) return 100;
        if (be instanceof VolcaniteFurnaceBlockEntity) return 100;
        if (be instanceof AdvancedVolcaniteFurnaceBlockEntity) return 100;
        if (be instanceof DionysianDistilleryBlockEntity) return DionysianDistilleryBlockEntity.MAX_PROGRESS;
        if (be instanceof DionysianPressBlockEntity) return DionysianPressBlockEntity.MAX_PROGRESS;
        if (be instanceof DionysianAgingBarrelBlockEntity) return DionysianAgingBarrelBlockEntity.AGING_TIME;
        if (be instanceof HydrothermalVitrifierBlockEntity) return HydrothermalVitrifierBlockEntity.MAX_PROGRESS;
        if (be instanceof HydrothermalCondenserBlockEntity) return HydrothermalCondenserBlockEntity.BASE_CONDENSE_TICKS;
        if (be instanceof HydrothermalLithofierBlockEntity lithofier) return lithofier.getMaxProgressForMode();
        if (be instanceof MagmaticCrucibleBlockEntity) return MagmaticCrucibleBlockEntity.BASE_MELT_TICKS;
        if (be instanceof VolcanicDistillationChamberBlockEntity) return VolcanicDistillationChamberBlockEntity.MAX_PROGRESS;
        if (be instanceof MagmaForgeCoreBlockEntity) return MagmaForgeCoreBlockEntity.MAX_PROGRESS;
        if (be instanceof DemetrianComposterBlockEntity) return 40;
        return 0;
    }

    private static String progressKey(BlockEntity be) {
        if (be instanceof DionysianAgingBarrelBlockEntity) return "AgingProgress";
        if (be instanceof MagmaForgeCoreBlockEntity) return "CraftTimer";
        return "Progress";
    }

    @Override
    public boolean shouldRequestData(BlockAccessor accessor) {
        return maxProgress(accessor.getBlockEntity()) > 0;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        int max = maxProgress(be);
        if (max <= 0) return;
        CompoundTag saved = be.saveWithoutMetadata(accessor.getLevel().registryAccess());
        data.putInt(KEY_PROGRESS, saved.getInt(progressKey(be)));
        data.putInt(KEY_MAX, max);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(KEY_MAX)) return;
        int max = data.getInt(KEY_MAX);
        int progress = data.getInt(KEY_PROGRESS);
        if (max <= 0) return;
        if (progress <= 0) {
            tooltip.add(Component.translatable("jade.obsidium.idle").withStyle(ChatFormatting.GRAY));
            return;
        }
        int percent = Math.min(100, progress * 100 / max);
        int secondsLeft = Math.max(0, (max - progress) / 20);
        tooltip.add(Component.translatable("jade.obsidium.progress", percent, secondsLeft).withStyle(ChatFormatting.GOLD));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
