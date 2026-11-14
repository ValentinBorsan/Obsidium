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
import net.valentin.obsidium.block.VolcaniteStoragePipeBlock;
import net.valentin.obsidium.block.VolcaniteStoragePipeBlockEntity;

import java.util.List;

public class VolcaniteStackUpgradeItem extends Item {
    public VolcaniteStackUpgradeItem(Properties properties) {
        super(properties.fireResistant());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        Player player = context.getPlayer();

        BlockEntity clickedBe = level.getBlockEntity(clickedPos);
        if (clickedBe instanceof VolcaniteStoragePipeBlockEntity pipeBe) {
            if (!level.isClientSide()) {
                BlockHitResult hit = new BlockHitResult(context.getClickLocation(), clickedFace, clickedPos, false);
                Direction targetFace = VolcaniteStoragePipeBlock.getClickedPipeFace(hit, clickedPos, level);

                ItemStack currentStack = pipeBe.getEnergyUpgradeItem(targetFace);
                if (!currentStack.isEmpty() && currentStack.getItem() instanceof VolcaniteStackUpgradeItem) {
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§e[Pipe] §7" + targetFace.getName().toUpperCase() + " side already has a Stack Upgrade!"), true);
                    }
                    return InteractionResult.CONSUME;
                }

                if (!currentStack.isEmpty()) {
                    if (player != null && !player.getInventory().add(currentStack.copy())) {
                        level.addFreshEntity(new ItemEntity(level, clickedPos.getX() + 0.5, clickedPos.getY() + 0.5, clickedPos.getZ() + 0.5, currentStack.copy()));
                    }
                }

                pipeBe.setEnergyUpgrade(targetFace, 3, context.getItemInHand().copyWithCount(1));
                if (player != null && !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }

                level.playSound(null, clickedPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.9F, 1.3F);
                if (player != null) {
                    player.displayClientMessage(Component.literal("§6[Pipe] §a✦ Installed §dStack Upgrade §aon §f" + targetFace.getName().toUpperCase() + " §aside! (📦 Transports full 64-item stacks!)"), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("§d★ Volcanite Stack Upgrade").withStyle(ChatFormatting.BOLD));
        tooltipComponents.add(Component.literal("§7Forged with Netherite Scrap & Compressed Volcanite:"));
        tooltipComponents.add(Component.literal("  §a📦 Item Transfer: §f64 items / full stack per cycle!"));
        tooltipComponents.add(Component.literal("  §b💧 Fluid Transfer: §f16,000 mB / cycle"));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("§8Works in all Pipe Modules (Extract & Insert)."));
        tooltipComponents.add(Component.literal("§eRight-click on a Pipe face or place into Module GUI."));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}

