package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.valentin.obsidium.block.PipeConnectionHelper;
import net.valentin.obsidium.block.VolcaniteStoragePipeBlock;
import net.valentin.obsidium.block.VolcaniteStoragePipeBlockEntity;

import java.util.List;

public class VolcanitePipeEnergyUpgradeItem extends Item {
    private final int tier; // 1 = Basic, 2 = Advanced, 3 = Supreme
    private final int maxTransfer; // in RF/t

    public VolcanitePipeEnergyUpgradeItem(Properties properties, int tier, int maxTransfer) {
        super(properties.fireResistant());
        this.tier = tier;
        this.maxTransfer = maxTransfer;
    }

    public int getTier() {
        return tier;
    }

    public int getMaxTransfer() {
        return maxTransfer;
    }

    public int getMaxFluidTransfer() {
        return switch (tier) {
            case 1 -> 1000;
            case 2 -> 4000;
            case 3 -> 16000;
            default -> 250;
        };
    }

    public int getMaxItemTransfer() {
        return switch (tier) {
            case 1 -> 8;
            case 2 -> 32;
            case 3 -> 64;
            default -> 1;
        };
    }

    public String getTierName() {
        return switch (tier) {
            case 1 -> "Basic";
            case 2 -> "Advanced";
            case 3 -> "Supreme";
            default -> "Unknown";
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        Player player = context.getPlayer();

        BlockEntity clickedBe = level.getBlockEntity(clickedPos);

        // Clicked directly on a Storage Pipe or Conduit
        if (clickedBe instanceof VolcaniteStoragePipeBlockEntity pipeBe) {
            if (!level.isClientSide()) {
                BlockHitResult hit = new BlockHitResult(context.getClickLocation(), clickedFace, clickedPos, false);
                Direction targetFace = VolcaniteStoragePipeBlock.getClickedPipeFace(hit, clickedPos, level);
                boolean connected = level.getBlockState(clickedPos).getValue(VolcaniteStoragePipeBlock.getProperty(targetFace))
                        && !pipeBe.isSideDisconnected(targetFace)
                        && PipeConnectionHelper.canConnect(level, clickedPos.relative(targetFace), targetFace.getOpposite());
                if (!connected) {
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§c[Pipe] Connect this side to a chest, barrel, machine, or logistics network first."), true);
                    }
                    return InteractionResult.CONSUME;
                }

                int currentTier = pipeBe.getEnergyUpgradeTier(targetFace);
                ItemStack currentStack = pipeBe.getEnergyUpgradeItem(targetFace);

                if (currentTier >= this.tier) {
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§e[Pipe] §7" + targetFace.getName().toUpperCase() + " side already has a " + currentStack.getHoverName().getString()), true);
                    }
                    return InteractionResult.CONSUME;
                }

                if (!currentStack.isEmpty()) {
                    if (player != null && !player.getInventory().add(currentStack.copy())) {
                        level.addFreshEntity(new ItemEntity(level, clickedPos.getX() + 0.5, clickedPos.getY() + 0.5, clickedPos.getZ() + 0.5, currentStack.copy()));
                    }
                }

                pipeBe.setEnergyUpgrade(targetFace, this.tier, context.getItemInHand().copyWithCount(1));
                if (player != null && !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }

                level.playSound(null, clickedPos, SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
                if (player != null) {
                    player.displayClientMessage(Component.literal("§6[Pipe] §aInstalled §e" + getTierName() + " Conduit Upgrade §aon §f" + targetFace.getName().toUpperCase() + " §aside! (⚡ " + String.format("%,d", maxTransfer) + " RF/t | 💧 " + String.format("%,d", getMaxFluidTransfer()) + " mB/t | 📦 " + getMaxItemTransfer() + " items)"), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String tierColor = switch (tier) {
            case 1 -> "§6";
            case 2 -> "§b";
            case 3 -> "§d";
            default -> "§f";
        };
        tooltipComponents.add(Component.literal(tierColor + "★ " + getTierName() + " Conduit Upgrade").withStyle(ChatFormatting.BOLD));
        tooltipComponents.add(Component.literal("§7Comprehensive boost for Conduits & Pipes:"));
        tooltipComponents.add(Component.literal("  §e⚡ Energy: §f" + String.format("%,d", maxTransfer) + " RF/t"));
        tooltipComponents.add(Component.literal("  §b💧 Liquids: §f" + String.format("%,d", getMaxFluidTransfer()) + " mB/t"));
        tooltipComponents.add(Component.literal("  §a📦 Items: §f" + getMaxItemTransfer() + " items / cycle"));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("§8Right-click on a Pipe/Conduit face or place in Pipe GUI."));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
