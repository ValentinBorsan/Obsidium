package net.valentin.obsidium.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.valentin.obsidium.block.NaiadFluidPipeBlockEntity;
import net.valentin.obsidium.block.VolcaniteEnergyWireBlockEntity;
import net.valentin.obsidium.block.VolcaniteStoragePipeBlock;
import net.valentin.obsidium.block.VolcaniteStoragePipeBlockEntity;

import java.util.List;

public class VolcaniteLogisticsModuleItem extends Item {
    public VolcaniteLogisticsModuleItem(Properties properties) {
        super(properties.stacksTo(16).fireResistant());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        Player player = context.getPlayer();

        BlockEntity clickedBe = level.getBlockEntity(clickedPos);

        // Case 1: Clicked on a Volcanite Storage Pipe, Universal Conduit, or Hermes Item Pipe
        if (clickedBe instanceof VolcaniteStoragePipeBlockEntity pipeBe) {
            if (!level.isClientSide()) {
                BlockHitResult hit = new BlockHitResult(context.getClickLocation(), clickedFace, clickedPos, false);
                Direction targetFace = VolcaniteStoragePipeBlock.getClickedPipeFace(hit, clickedPos, level);
                boolean connected = level.getBlockState(clickedPos).getValue(VolcaniteStoragePipeBlock.getProperty(targetFace))
                        && !pipeBe.isSideDisconnected(targetFace);
                if (!connected) {
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§c[Pipe] Connect this side to a chest, barrel, machine, or logistics network first."), true);
                    }
                    return InteractionResult.CONSUME;
                }

                if (pipeBe.getModule(targetFace) == VolcaniteStoragePipeBlockEntity.ModuleType.NONE) {
                    pipeBe.setModule(targetFace, VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT);
                    pipeBe.setValveModule(targetFace, false);
                    if (player != null && !player.getAbilities().instabuild) {
                        context.getItemInHand().shrink(1);
                    }
                    level.playSound(null, clickedPos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§6[Pipe] §aMounted §6Logistics Module §aon §f" + targetFace.getName().toUpperCase() + " §aside!"), true);
                    }
                } else if (player != null) {
                    // Open module UI
                    pipeBe.setSelectedFaceForMenu(targetFace);
                    player.openMenu(pipeBe);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // Case 2: Clicked on a Naiad Fluid Pipe
        if (clickedBe instanceof NaiadFluidPipeBlockEntity naiadBe) {
            if (!level.isClientSide()) {
                net.minecraft.world.phys.BlockHitResult bHit = new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(), clickedFace, clickedPos, false);
                Direction targetFace = net.valentin.obsidium.block.NaiadFluidPipeBlock.getTargetFaceFromHit(bHit, clickedPos);

                // If the hit face points to another pipe or non-fluid neighbor, pick the closest external FluidStorage face
                BlockPos checkPos = clickedPos.relative(targetFace);
                boolean validExternal = !(level.getBlockEntity(checkPos) instanceof NaiadFluidPipeBlockEntity)
                        && net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(level, checkPos, targetFace.getOpposite()) != null;
                if (!validExternal) {
                    net.minecraft.world.phys.Vec3 hitOffset = context.getClickLocation().subtract(clickedPos.getX() + 0.5, clickedPos.getY() + 0.5, clickedPos.getZ() + 0.5);
                    Direction bestFace = null;
                    double bestScore = -Double.MAX_VALUE;
                    for (Direction d : Direction.values()) {
                        BlockPos np = clickedPos.relative(d);
                        if (level.getBlockEntity(np) instanceof NaiadFluidPipeBlockEntity) continue;
                        if (net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(level, np, d.getOpposite()) != null) {
                            double score = hitOffset.x * d.getStepX() + hitOffset.y * d.getStepY() + hitOffset.z * d.getStepZ();
                            if (score > bestScore) {
                                bestScore = score;
                                bestFace = d;
                            }
                        }
                    }
                    if (bestFace != null) {
                        targetFace = bestFace;
                    } else {
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§b[Naiad Pipe] §eValves can only be placed on tank/machine connections!"), true);
                        }
                        return InteractionResult.sidedSuccess(false);
                    }
                }
                if (naiadBe.getValveMode(targetFace) == NaiadFluidPipeBlockEntity.ValveMode.NONE) {
                    naiadBe.setValveMode(targetFace, NaiadFluidPipeBlockEntity.ValveMode.EXTRACT);
                    if (player != null && !player.getAbilities().instabuild) {
                        context.getItemInHand().shrink(1);
                    }
                    level.playSound(null, clickedPos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§b[Naiad Pipe] §aMounted §bFluid Logistics Valve §aon §f" + targetFace.getName().toUpperCase() + " §aside!"), true);
                    }
                } else {
                    NaiadFluidPipeBlockEntity.ValveMode next = naiadBe.cycleValveMode(targetFace);
                    level.playSound(null, clickedPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 1.2F);
                    if (player != null) {
                        String modeDesc = next == NaiadFluidPipeBlockEntity.ValveMode.EXTRACT ? "§6EXTRACT (Pulling)" : "§bINSERT (Pushing)";
                        player.displayClientMessage(Component.literal("§b[Naiad Pipe] §7Face §f" + targetFace.getName().toUpperCase() + " §7valve mode: " + modeDesc), true);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // Case 3: Clicked on a Volcanite Energy Wire
        if (clickedBe instanceof VolcaniteEnergyWireBlockEntity wireBe) {
            if (!level.isClientSide()) {
                Direction targetFace = clickedFace;
                VolcaniteEnergyWireBlockEntity.WireMode next = wireBe.cycleWireMode(targetFace);
                level.playSound(null, clickedPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 1.2F);
                if (player != null) {
                    String modeDesc = switch (next) {
                        case EXTRACT -> "§6EXTRACT / EXPORT (Pull Energy)";
                        case INSERT -> "§bINSERT / IMPORT (Push Energy)";
                        case NORMAL -> "§eNORMAL (Bidirectional)";
                    };
                    player.displayClientMessage(Component.literal("§6[Energy Wire] §eFace §f" + targetFace.getName().toUpperCase() + " §emode: " + modeDesc), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // Case 4: Clicked on a container/machine adjacent to a pipe
        if (VolcaniteStoragePipeBlockEntity.isExternalContainer(level, clickedPos, clickedBe)) {
            for (Direction d : Direction.values()) {
                BlockPos neighborPos = clickedPos.relative(d);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (neighborBe instanceof VolcaniteStoragePipeBlockEntity pipeBe) {
                    if (!level.isClientSide()) {
                        Direction pipeSideTowardsChest = d.getOpposite();
                        if (pipeBe.getModule(pipeSideTowardsChest) == VolcaniteStoragePipeBlockEntity.ModuleType.NONE) {
                            pipeBe.setModule(pipeSideTowardsChest, VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT);
                            if (player != null && !player.getAbilities().instabuild) {
                                context.getItemInHand().shrink(1);
                            }
                            level.playSound(null, neighborPos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
                            if (player != null) {
                                player.displayClientMessage(Component.literal("§6[Pipe] §aMounted §6Logistics Module §apulling from this container!"), true);
                            }
                        } else if (player != null) {
                            pipeBe.setSelectedFaceForMenu(pipeSideTowardsChest);
                            player.openMenu(pipeBe);
                        }
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6✦ Universal Pipe Logistics Module"));
        tooltip.add(Component.literal("§eUnified Export / Import Controller for All Pipes"));
        tooltip.add(Component.literal("§7• Right-click any pipe, wire, or adjacent container to mount"));
        tooltip.add(Component.literal("§7• Switch between §6EXTRACT §7and §bINSERT §7inside GUI"));
        tooltip.add(Component.literal("§7• Built-in 3-slot §aWhitelist §7/ §cBlacklist §7filter"));
        tooltip.add(Component.literal("§8• Shift + Right-click with empty hand/wrench to dismantle"));
    }
}

