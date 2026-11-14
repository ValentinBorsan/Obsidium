package net.valentin.obsidium.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.block.GraftedSaplingBlock;
import net.valentin.obsidium.block.GraftedSaplingBlockEntity;
import net.valentin.obsidium.block.MineralTreeLeavesBlock;
import net.valentin.obsidium.block.OreTreeSaplingBlock;

import java.util.List;

public class DemetrianBotanistLensItem extends Item {

    public DemetrianBotanistLensItem(Properties properties) {
        super(properties.durability(500).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player == null) return InteractionResult.PASS;

        // 1. Inspecting a Grafted Sapling
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof GraftedSaplingBlockEntity grafted) {
            String rootSpecies = DemetrianScionItem.formatSpeciesName(grafted.getRootstockSpecies());
            GraftedSaplingBlock.Stage stage = grafted.getStage();

            player.sendSystemMessage(Component.literal("§6━━━ 🔬 Demetrian Botanical Analysis ━━━"));
            player.sendSystemMessage(Component.literal("§7Rootstock (Root): §e" + rootSpecies));

            if (stage == GraftedSaplingBlock.Stage.INCISED) {
                player.sendSystemMessage(Component.literal("§7Status: §cOpen Incision §7(Apply a Scion cutting)"));
                player.sendSystemMessage(Component.literal("§8Possible Hybrids: Coal -> Redstone | Copper -> Lapis | Silver -> Quartz/Electrum"));
            } else if (stage == GraftedSaplingBlock.Stage.GRAFTED) {
                String scionSpecies = DemetrianScionItem.formatSpeciesName(grafted.getScionSpecies());
                player.sendSystemMessage(Component.literal("§7Inserted Scion: §a" + scionSpecies));
                player.sendSystemMessage(Component.literal("§7Status: §eReady for Metamorphosis §7(Bind with Grafting Tape!)"));
            }

            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
            return InteractionResult.SUCCESS;
        }

        // 2. Inspecting Mineral Leaves or Vanilla Leaves
        if (block instanceof MineralTreeLeavesBlock minLeaves) {
            String sp = DemetrianScionItem.formatSpeciesName(minLeaves.species());
            player.sendSystemMessage(Component.literal("§6━━━ 🔬 Noble Canopy Analysis ━━━"));
            player.sendSystemMessage(Component.literal("§7Donor Species: §e" + sp));
            player.sendSystemMessage(Component.literal("§7Harvesting: §aRight-click with Grafting Knife to harvest a Scion."));
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.6F);
            return InteractionResult.SUCCESS;
        } else if (block instanceof LeavesBlock) {
            player.sendSystemMessage(Component.literal("§6━━━ 🔬 Wild Canopy Analysis ━━━"));
            player.sendSystemMessage(Component.literal("§7Donor Species: §e" + block.getName().getString()));
            player.sendSystemMessage(Component.literal("§7Harvesting: §aRight-click with Grafting Knife to harvest a Scion."));
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.6F);
            return InteractionResult.SUCCESS;
        }

        // 3. Inspecting a Sapling
        if (block instanceof OreTreeSaplingBlock || block instanceof SaplingBlock) {
            player.sendSystemMessage(Component.literal("§6━━━ 🔬 Young Sapling Analysis ━━━"));
            player.sendSystemMessage(Component.literal("§7Plant: §e" + block.getName().getString()));
            player.sendSystemMessage(Component.literal("§7Status: §aIntact"));
            player.sendSystemMessage(Component.literal("§7Grafting: §eMake an incision with the Grafting Knife to prepare as rootstock."));
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.6F);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6✦ Demetrian Botanical Monocle"));
        tooltip.add(Component.literal("§7Right-click on any sapling, leaves, or graft"));
        tooltip.add(Component.literal("§7to inspect species, grafting stages, and hybrid potential."));
    }
}
