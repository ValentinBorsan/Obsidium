package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.valentin.obsidium.block.AstrapeEnergyPylonBlock;
import net.valentin.obsidium.block.AstrapeEnergyPylonBlockEntity;
import net.valentin.obsidium.block.VolcanicTeleporterBlockEntity;
import net.valentin.obsidium.block.VolcaniteNetworkNodeEmitterBlock;
import net.valentin.obsidium.block.VolcaniteNetworkNodeEmitterBlockEntity;
import net.valentin.obsidium.block.VolcaniteNetworkNodeReceiverBlock;
import net.valentin.obsidium.block.VolcaniteNetworkNodeReceiverBlockEntity;
import net.valentin.obsidium.teleport.VolcaniteTeleportNetworkState;

import java.util.List;

public class SupremeLinkToolItem extends Item {
    public SupremeLinkToolItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        BlockEntity be = level.getBlockEntity(pos);

        // 1. Right-clicked on an Emitter: Bind coordinates
        if (be instanceof VolcaniteNetworkNodeEmitterBlockEntity || level.getBlockState(pos).getBlock() instanceof VolcaniteNetworkNodeEmitterBlock) {
            if (!level.isClientSide()) {
                String dim = level.dimension().location().toString();
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putInt("EmitX", pos.getX());
                    tag.putInt("EmitY", pos.getY());
                    tag.putInt("EmitZ", pos.getZ());
                    tag.putString("EmitDim", dim);
                    tag.putBoolean("HasEmitter", true);
                });

                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.8F);
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 16, 0.2, 0.2, 0.2, 0.05);
                }
                if (player != null) {
                    player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §aEmitter bound at §e" + pos.toShortString() + "§a! Now right-click a Receiver to connect."), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 2. Right-clicked on a Receiver: Link to bound Emitter
        if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity || level.getBlockState(pos).getBlock() instanceof VolcaniteNetworkNodeReceiverBlock) {
            CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = data.copyTag();

            if (!tag.getBoolean("HasEmitter")) {
                if (!level.isClientSide() && player != null) {
                    player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §cNo Emitter bound! Right-click a Network Node Emitter first."), true);
                    level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            String dim = tag.getString("EmitDim");
            if (!dim.equals(level.dimension().location().toString())) {
                if (!level.isClientSide() && player != null) {
                    player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §cCannot link across different dimensions!"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            BlockPos emitterPos = new BlockPos(tag.getInt("EmitX"), tag.getInt("EmitY"), tag.getInt("EmitZ"));
            double dist = Math.sqrt(pos.distSqr(emitterPos));

            if (dist > VolcaniteNetworkNodeReceiverBlockEntity.SUPREME_RANGE) {
                if (!level.isClientSide() && player != null) {
                    player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §cTarget out of range! Distance is " + (int) dist + "m (Max: " + VolcaniteNetworkNodeReceiverBlockEntity.SUPREME_RANGE + "m)."), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            if (!level.isClientSide()) {
                if (be instanceof VolcaniteNetworkNodeReceiverBlockEntity receiverBe) {
                    receiverBe.setLinkedEmitter(emitterPos);

                    if (level.isLoaded(emitterPos) && level.getBlockEntity(emitterPos) instanceof VolcaniteNetworkNodeEmitterBlockEntity emitterBe) {
                        emitterBe.registerReceiver(pos);
                    }

                    level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.6F);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.1);
                        sl.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 15, 0.2, 0.2, 0.2, 0.1);
                    }

                    if (player != null) {
                        player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §aSuccessfully linked Receiver at §e" + pos.toShortString() + " §ato Emitter at §e" + emitterPos.toShortString() + " §a(§b" + (int) dist + "m§a)!"), true);
                    }
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 3. Right-clicked on an Astrape Energy Pylon
        if (be instanceof AstrapeEnergyPylonBlockEntity || level.getBlockState(pos).getBlock() instanceof AstrapeEnergyPylonBlock) {
            if (player != null && player.isShiftKeyDown()) {
                // Sneak-click clears this pylon's links
                if (!level.isClientSide()) {
                    if (be instanceof AstrapeEnergyPylonBlockEntity pylonBe) {
                        pylonBe.clearAllLinks();
                        player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §cCleared all links for Astrape Pylon at §e" + pos.toShortString()), true);
                        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = data.copyTag();

            if (!tag.getBoolean("HasPylon")) {
                // Bind source pylon
                if (!level.isClientSide()) {
                    String dim = level.dimension().location().toString();
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
                        t.putInt("PylonX", pos.getX());
                        t.putInt("PylonY", pos.getY());
                        t.putInt("PylonZ", pos.getZ());
                        t.putString("PylonDim", dim);
                        t.putBoolean("HasPylon", true);
                    });

                    level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.9F);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.05);
                    }
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §aFirst Astrape Pylon bound at §e" + pos.toShortString() + "§a! Now click target Pylon to link/unlink."), true);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            } else {
                // Connect or disconnect with second pylon
                BlockPos startPos = new BlockPos(tag.getInt("PylonX"), tag.getInt("PylonY"), tag.getInt("PylonZ"));
                if (startPos.equals(pos)) {
                    // Clicked same pylon again: cancel selection
                    if (!level.isClientSide()) {
                        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove("HasPylon"));
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §ePylon selection cancelled."), true);
                        }
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }

                String dim = tag.getString("PylonDim");
                if (!dim.equals(level.dimension().location().toString())) {
                    if (!level.isClientSide() && player != null) {
                        player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §cCannot link Pylons across dimensions!"), true);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }

                double dist = Math.sqrt(pos.distSqr(startPos));
                if (dist > AstrapeEnergyPylonBlockEntity.MAX_RANGE) {
                    if (!level.isClientSide() && player != null) {
                        player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §cPylons out of range! Distance is " + (int) dist + "m (Max: " + (int) AstrapeEnergyPylonBlockEntity.MAX_RANGE + "m)."), true);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }

                if (!level.isClientSide()) {
                    BlockEntity startBe = level.getBlockEntity(startPos);
                    if (startBe instanceof AstrapeEnergyPylonBlockEntity pylonA && be instanceof AstrapeEnergyPylonBlockEntity pylonB) {
                        boolean linked = pylonA.toggleLink(pos);
                        pylonB.toggleLink(startPos);

                        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove("HasPylon"));

                        if (linked) {
                            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.8F);
                            if (level instanceof ServerLevel sl) {
                                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.1);
                                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.05);
                            }
                            if (player != null) {
                                player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §aSuccessfully linked Pylons! §b(" + (int) dist + "m) §7[Connections: " + pylonB.getLinkedPylons().size() + "/" + AstrapeEnergyPylonBlockEntity.MAX_LINKS + "]"), true);
                            }
                        } else {
                            level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
                            if (player != null) {
                                player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §cUnlinked Pylons at §e" + startPos.toShortString() + " §cand §e" + pos.toShortString()), true);
                            }
                        }
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        // 4. Right-clicked on a Volcanic Teleporter Pad: Bidirectional Quick-Warp Pairing
        if (be instanceof VolcanicTeleporterBlockEntity padB) {
            if (player != null && player.isShiftKeyDown()) {
                if (!level.isClientSide()) {
                    padB.setPinnedTargetId("");
                    player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §cCleared Quick-Warp pin for Teleporter at §e" + pos.toShortString()), true);
                    level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = data.copyTag();
            String currentDim = level.dimension().location().toString();

            if (!tag.getBoolean("HasTp")) {
                if (!level.isClientSide()) {
                    padB.registerInNetwork();
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
                        t.putInt("TpX", pos.getX());
                        t.putInt("TpY", pos.getY());
                        t.putInt("TpZ", pos.getZ());
                        t.putString("TpDim", currentDim);
                        t.putString("TpName", padB.getPadName());
                        t.putBoolean("HasTp", true);
                    });
                    level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.85F);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 16, 0.25, 0.25, 0.25, 0.1);
                    }
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §bTeleporter bound: §e" + padB.getPadName() + " §a! Click another Teleporter (even cross-dimension for Core pads)."), true);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            } else {
                BlockPos posA = new BlockPos(tag.getInt("TpX"), tag.getInt("TpY"), tag.getInt("TpZ"));
                String dimA = tag.getString("TpDim");
                if (dimA.equals(currentDim) && posA.equals(pos)) {
                    if (!level.isClientSide()) {
                        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove("HasTp"));
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §eTeleporter selection cancelled."), true);
                        }
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }

                if (!level.isClientSide() && level.getServer() != null) {
                    padB.registerInNetwork();
                    String idA = VolcaniteTeleportNetworkState.makePadId(dimA, posA);
                    String idB = VolcaniteTeleportNetworkState.makePadId(currentDim, pos);
                    ResourceLocation dimLocA = ResourceLocation.tryParse(dimA);
                    ServerLevel levelA = dimLocA != null ? level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimLocA)) : null;

                    boolean crossDim = !dimA.equals(currentDim);
                    boolean padASupreme = false;
                    if (levelA != null && levelA.isLoaded(posA) && levelA.getBlockEntity(posA) instanceof VolcanicTeleporterBlockEntity padA) {
                        padASupreme = padA.isSupreme();
                        if (!crossDim || padASupreme) {
                            padA.setPinnedTargetId(idB);
                        }
                    } else {
                        var nodeA = VolcaniteTeleportNetworkState.get(level.getServer()).getPad(idA);
                        padASupreme = nodeA != null && nodeA.supreme();
                    }

                    if (!crossDim || padB.isSupreme()) {
                        padB.setPinnedTargetId(idA);
                    }

                    CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove("HasTp"));

                    if (crossDim && !padB.isSupreme() && !padASupreme) {
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §cBasic Teleporters cannot link across dimensions!"), true);
                        }
                        level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
                    } else {
                        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.75F);
                        if (level instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 24, 0.3, 0.3, 0.3, 0.15);
                            sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 14, 0.25, 0.25, 0.25, 0.05);
                        }
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §aQuantum Teleporters linked §b↔ §e" + tag.getString("TpName") + " §a& §e" + padB.getPadName()), true);
                        }
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Sneak + Right click in air clears bound data
        if (player.isShiftKeyDown()) {
            CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = data.copyTag();

            if (tag.getBoolean("HasEmitter") || tag.getBoolean("HasPylon") || tag.getBoolean("HasTp")) {
                if (!level.isClientSide()) {
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
                        t.remove("HasEmitter");
                        t.remove("EmitX");
                        t.remove("EmitY");
                        t.remove("EmitZ");
                        t.remove("EmitDim");
                        t.remove("HasPylon");
                        t.remove("PylonX");
                        t.remove("PylonY");
                        t.remove("PylonZ");
                        t.remove("PylonDim");
                        t.remove("HasTp");
                        t.remove("TpX");
                        t.remove("TpY");
                        t.remove("TpZ");
                        t.remove("TpDim");
                        t.remove("TpName");
                    });
                    player.displayClientMessage(Component.literal("§6[Supreme Link Tool] §eBound targets cleared."), true);
                    level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.5F);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }

        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltip, isAdvanced);

        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();

        if (tag.getBoolean("HasEmitter")) {
            int x = tag.getInt("EmitX");
            int y = tag.getInt("EmitY");
            int z = tag.getInt("EmitZ");
            String dim = tag.getString("EmitDim");
            tooltip.add(Component.literal("§aBound Emitter: §e(" + x + ", " + y + ", " + z + ")").withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal("§7Dimension: §f" + dim).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§bRight-click a Network Node Receiver to link.").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("§8Sneak + Right-click air to clear target.").withStyle(ChatFormatting.DARK_GRAY));
        } else if (tag.getBoolean("HasPylon")) {
            int x = tag.getInt("PylonX");
            int y = tag.getInt("PylonY");
            int z = tag.getInt("PylonZ");
            String dim = tag.getString("PylonDim");
            tooltip.add(Component.literal("§6Bound Pylon: §e(" + x + ", " + y + ", " + z + ")").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("§7Dimension: §f" + dim).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§bRight-click another Astrape Pylon to link/unlink.").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("§8Sneak + Right-click air to clear target.").withStyle(ChatFormatting.DARK_GRAY));
        } else if (tag.getBoolean("HasTp")) {
            int x = tag.getInt("TpX");
            int y = tag.getInt("TpY");
            int z = tag.getInt("TpZ");
            String dim = tag.getString("TpDim");
            tooltip.add(Component.literal("§bBound Teleporter: §e" + tag.getString("TpName") + " (" + x + ", " + y + ", " + z + ")").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("§7Dimension: §f" + dim).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§dRight-click another Teleporter Pad to pair Quick-Warp.").withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.literal("§8Sneak + Right-click air to clear target.").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("§7Status: §cNo Target Bound").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("§7Right-click an Emitter, Astrape Pylon, or Teleporter Pad.").withStyle(ChatFormatting.GRAY));
        }

        tooltip.add(Component.literal("§6★ Supreme Wireless Quantum Synchronizer").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§dMax Linking Range: §f" + VolcaniteNetworkNodeReceiverBlockEntity.SUPREME_RANGE + " blocks").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}

