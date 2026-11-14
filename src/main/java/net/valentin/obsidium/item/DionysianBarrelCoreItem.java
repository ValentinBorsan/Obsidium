package net.valentin.obsidium.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.valentin.obsidium.block.MegaVolcanicBarrelBlock;

import java.util.List;

public class DionysianBarrelCoreItem extends Item {
    public DionysianBarrelCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();

        if (player != null && MegaVolcanicBarrelBlock.tryAssembleMultiblock(level, pos, player, hand)) {
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.obsidium.dionysian_barrel_core.desc"));
        tooltip.add(Component.translatable("tooltip.obsidium.dionysian_barrel_core.structure_1"));
        tooltip.add(Component.translatable("tooltip.obsidium.dionysian_barrel_core.structure_2"));
        tooltip.add(Component.translatable("tooltip.obsidium.dionysian_barrel_core.structure_3"));
    }
}

