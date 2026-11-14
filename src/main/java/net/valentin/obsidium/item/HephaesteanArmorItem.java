package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import team.reborn.energy.api.base.SimpleEnergyItem;

import java.util.List;

public class HephaesteanArmorItem extends ArmorItem implements SimpleEnergyItem {

    public static final long CAPACITY = 1_000_000L;
    public static final long MAX_INPUT = 50_000L;

    public HephaesteanArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) { return CAPACITY; }

    @Override
    public long getEnergyMaxInput(ItemStack stack) { return MAX_INPUT; }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) { return 0; }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (this.type == Type.CHESTPLATE && entity instanceof Player player) {
            boolean hasEnergy = getStoredEnergy(stack) > 1000L;
            if (hasEnergy) {
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }
                if (player.getAbilities().flying && level.getGameTime() % 20 == 0) {
                    tryUseEnergy(stack, 250L);
                    if (level.isClientSide()) {
                        level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, player.getX(), player.getY() + 0.1, player.getZ(), 0, -0.1, 0);
                    }
                }
            } else if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round((double) getStoredEnergy(stack) * 13.0 / (double) CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) { return 0x00E5FF; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long stored = getStoredEnergy(stack);
        tooltip.add(Component.literal("§b⚡ Energy: §f" + String.format("%,d", stored) + " §b/ §f" + String.format("%,d", CAPACITY) + " RF").withStyle(ChatFormatting.AQUA));
        if (this.type == Type.CHESTPLATE) {
            tooltip.add(Component.literal("§6Geyser Flight: §7Enables creative flight while powered (250 RF/s).").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.literal("§eHephaestean Shielding: §7Supreme volcanic divine protection.").withStyle(ChatFormatting.YELLOW));
    }
}

