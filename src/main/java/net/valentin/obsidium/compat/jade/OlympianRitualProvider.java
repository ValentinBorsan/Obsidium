package net.valentin.obsidium.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.AstrapeRitualObeliskBlockEntity;
import net.valentin.obsidium.block.OlympianAltarBlockEntity;
import net.valentin.obsidium.config.ModConfig;
import net.valentin.obsidium.mythology.DivinePact;
import net.valentin.obsidium.mythology.DivinePactHelper;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade overlay for the Olympian ritual multiblock: altar offering, active ritual and
 * progress, plus the scroll/offering held by each Astrape Ritual Obelisk.
 */
public enum OlympianRitualProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ObsidiumMod.MOD_ID, "olympian_ritual");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof OlympianAltarBlockEntity altar) {
            ItemStack offering = altar.getPlacedItem();
            if (!offering.isEmpty()) {
                data.putString("ObsOffering", offering.getHoverName().getString());
                DivinePact bound = DivinePactHelper.getPact(offering);
                if (bound != DivinePact.NONE) data.putString("ObsBoundPact", bound.getId());
            }
            data.putBoolean("ObsRunning", altar.isRitualRunning());
            if (altar.isRitualRunning()) {
                int duration = Math.max(1, ModConfig.get().altarRitualDurationTicks);
                data.putInt("ObsPercent", Math.min(100, altar.getRitualProgress() * 100 / duration));
                data.putString("ObsPact", altar.getTargetPact().getId());
            }
        } else if (be instanceof AstrapeRitualObeliskBlockEntity obelisk) {
            ItemStack held = obelisk.getHeldItem();
            if (!held.isEmpty()) data.putString("ObsOffering", held.getHoverName().getString());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains("ObsOffering")) {
            tooltip.add(Component.translatable("jade.obsidium.altar.item", data.getString("ObsOffering")).withStyle(ChatFormatting.AQUA));
        }
        if (data.contains("ObsBoundPact")) {
            DivinePact bound = DivinePact.fromId(data.getString("ObsBoundPact"));
            tooltip.add(bound.getFormattedBadge());
        }
        if (!(accessor.getBlockEntity() instanceof OlympianAltarBlockEntity)) return;
        if (data.getBoolean("ObsRunning")) {
            DivinePact pact = DivinePact.fromId(data.getString("ObsPact"));
            tooltip.add(Component.translatable("jade.obsidium.altar.ritual",
                    Component.literal(pact.getDisplayName()).withStyle(pact.getColor()),
                    data.getInt("ObsPercent")).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("jade.obsidium.altar.idle").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
