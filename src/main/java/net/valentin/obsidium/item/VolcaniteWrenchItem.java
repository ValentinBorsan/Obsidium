package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.*;

import java.util.List;

public class VolcaniteWrenchItem extends Item {
    public static final net.minecraft.tags.TagKey<Item> C_WRENCHES = net.minecraft.tags.TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath("c", "wrenches"));
    public static final net.minecraft.tags.TagKey<Item> C_TOOLS_WRENCH = net.minecraft.tags.TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    public static boolean isWrench(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof VolcaniteWrenchItem) return true;
        return stack.is(C_WRENCHES) || stack.is(C_TOOLS_WRENCH);
    }

    public VolcaniteWrenchItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // =========================================================================
        // 0. SNEAK + CLICK: DISMANTLE / BREAK ANY OBSIDIUM BLOCK SAFELY WITH DROPS
        // =========================================================================
        if (player != null && player.isShiftKeyDown()) {
            // 0a. Mega Volcanic Barrel dismantle controller
            if (block instanceof MegaVolcanicBarrelBlock || block instanceof MegaVolcanicBarrelPartBlock) {
                if (!level.isClientSide()) {
                    BlockPos controllerPos = (block instanceof MegaVolcanicBarrelPartBlock) ?
                            MegaVolcanicBarrelPartBlock.findControllerPos(level, pos) : pos;
                    if (controllerPos != null && level.getBlockState(controllerPos).getBlock() instanceof MegaVolcanicBarrelBlock barrelBlock) {
                        level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.MULTIBLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                        barrelBlock.dismantleFromPart(level, controllerPos, pos);
                        player.displayClientMessage(Component.literal("§6[Mega Barrel] §cDismantled Mega Volcanic Barrel!"), true);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            // 0b. Tartarus Piston Engine dismantle
            if (block instanceof TartarusPistonEngineBlock || block instanceof TartarusPistonEnginePartBlock) {
                if (!level.isClientSide()) {
                    BlockPos controllerPos = (block instanceof TartarusPistonEnginePartBlock) ?
                            TartarusPistonEnginePartBlock.findControllerPos(level, pos) : pos;
                    if (controllerPos != null && level.getBlockState(controllerPos).getBlock() instanceof TartarusPistonEngineBlock engineBlock) {
                        level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.MULTIBLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                        engineBlock.dismantleFromPart(level, controllerPos, pos);
                        player.displayClientMessage(Component.literal("§b❄ §6[Cryo-Thermic Engine] §cDismantled Cryo-Thermic Piston Engine!"), true);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            // 0c. Dionysian Bio-Caldera dismantle
            if (block instanceof DionysianBioCalderaBlock || block instanceof DionysianBioCalderaPartBlock) {
                if (!level.isClientSide()) {
                    BlockPos controllerPos = (block instanceof DionysianBioCalderaPartBlock) ?
                            DionysianBioCalderaPartBlock.findControllerPos(level, pos) : pos;
                    if (controllerPos != null && level.getBlockState(controllerPos).getBlock() instanceof DionysianBioCalderaBlock calderaBlock) {
                        level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.MULTIBLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                        calderaBlock.dismantleFromPart(level, controllerPos, pos);
                        player.displayClientMessage(Component.literal("§6[Bio-Caldera] §cDismantled Dionysian Bio-Caldera!"), true);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            // 0c2. Universal Conduit: Shift + Right-Click switches Channel Type (ITEMS -> FLUIDS -> ENERGY -> OMNI)
            if (block instanceof VolcaniteUniversalConduitBlock && level.getBlockEntity(pos) instanceof VolcaniteUniversalConduitBlockEntity conduit) {
                if (!level.isClientSide()) {
                    BlockHitResult bHit = new BlockHitResult(context.getClickLocation(), clickedFace, pos, false);
                    Direction targetFace = VolcaniteStoragePipeBlock.getTargetFaceFromHit(bHit, pos);
                    VolcaniteStoragePipeBlock.cycleConduitChannelType(conduit, level, pos, targetFace, player);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            // 0d. Universal Instant Dismantle: Any Obsidium machine, pipe, wire, conduit, or container!
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            if (blockId != null && blockId.getNamespace().equals("obsidium")) {
                if (!level.isClientSide()) {
                    level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.WRENCH_RATCHET, SoundSource.BLOCKS, 1.0F, 1.0F);

                    if (block instanceof MagmaForgeDummyBlock) {
                        BlockPos corePos = MagmaForgeDummyBlock.findCore(level, pos);
                        if (corePos != null && level.getBlockEntity(corePos) instanceof MagmaForgeCoreBlockEntity core) {
                            core.unform(level, corePos);
                            if (player == null || !player.getAbilities().instabuild) {
                                Block.popResource(level, pos, new ItemStack(ObsidiumMod.MAGMA_FORGE_CORE_ITEM));
                            }
                            level.destroyBlock(corePos, false, player);
                            player.displayClientMessage(Component.literal("§6[Wrench] §cDismantled Magma Forge"), true);
                            return InteractionResult.sidedSuccess(level.isClientSide());
                        }
                    }

                    List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos, level.getBlockEntity(pos), player, new ItemStack(Items.DIAMOND_PICKAXE));
                    if (drops.isEmpty()) {
                        Item item = block.asItem();
                        if (item != null && item != Items.AIR) {
                            drops = List.of(new ItemStack(item));
                        }
                    }

                    if (player == null || !player.getAbilities().instabuild) {
                        for (ItemStack drop : drops) {
                            Block.popResource(level, pos, drop);
                        }
                    }

                    level.destroyBlock(pos, false, player);
                    player.displayClientMessage(Component.literal("§6[Wrench] §cDismantled §f" + block.getName().getString()), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        // =========================================================================
        // 1. MULTIBLOCK ASSEMBLY CHECKS
        // =========================================================================
        if (MegaVolcanicBarrelBlock.isMultiblockComponent(state)) {
            if (player != null) {
                int coreSlot = -1;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack invStack = player.getInventory().getItem(i);
                    if (invStack.is(ObsidiumMod.DIONYSIAN_BARREL_CORE)) {
                        coreSlot = i;
                        break;
                    }
                }
                if (coreSlot != -1) {
                    if (MegaVolcanicBarrelBlock.tryAssembleMultiblock(level, pos, player, context.getHand())) {
                        if (!player.getAbilities().instabuild) {
                            player.getInventory().getItem(coreSlot).shrink(1);
                        }
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    }
                } else {
                    for (int ox = -1; ox <= 0; ox++) {
                        for (int oy = -1; oy <= 0; oy++) {
                            for (int oz = -1; oz <= 0; oz++) {
                                BlockPos anchor = pos.offset(ox, oy, oz);
                                if (MegaVolcanicBarrelBlock.isValid2x2x2Structure(level, anchor)) {
                                    if (!level.isClientSide()) {
                                        player.displayClientMessage(Component.literal("§6[Mega Barrel] §eStructure detected! Carry or hold 1 Dionysian Barrel Core to activate!"), true);
                                    }
                                    return InteractionResult.sidedSuccess(level.isClientSide());
                                }
                            }
                        }
                    }
                }
            }
        }

        if (TartarusPistonEngineBlock.isMultiblockComponent(state)) {
            if (TartarusPistonEngineBlock.tryAssembleMultiblock(level, pos, player, context.getHand())) {
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        if (DionysianBioCalderaBlock.isMultiblockComponent(state)) {
            if (DionysianBioCalderaBlock.tryAssembleMultiblock(level, pos, player, context.getHand())) {
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        // =========================================================================
        // 2. DIRECT CLICK ON PIPES / WIRES / CONDUITS: TOGGLE SIDE CONNECTION
        // =========================================================================
        // 2a. Storage Pipe, Hermes Item Pipe, Magmatic Universal Conduit
        if (block instanceof VolcaniteStoragePipeBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteStoragePipeBlockEntity pipeBe) {
                if (!level.isClientSide()) {
                    BlockHitResult bHit = new BlockHitResult(context.getClickLocation(), clickedFace, pos, false);
                    Direction targetFace = VolcaniteStoragePipeBlock.getTargetFaceFromHit(bHit, pos);

                    if (pipeBe instanceof VolcaniteUniversalConduitBlockEntity conduit) {
                        if (player != null && player.isShiftKeyDown()) {
                            VolcaniteStoragePipeBlock.cycleConduitChannelType(conduit, level, pos, targetFace, player);
                        } else {
                            VolcaniteStoragePipeBlock.cycleConduitFaceMode(conduit, level, pos, targetFace, player);
                        }
                    } else if (pipeBe.getModule(targetFace) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE) {
                        VolcaniteStoragePipeBlockEntity.ModuleType current = pipeBe.getModule(targetFace);
                        VolcaniteStoragePipeBlockEntity.ModuleType next = (current == VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT) ?
                                VolcaniteStoragePipeBlockEntity.ModuleType.INSERT : VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT;

                        pipeBe.setModule(targetFace, next);
                        String modeText = (next == VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT) ?
                                "§6[EXTRACT]" : "§b[INSERT]";
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Wrench] §eSide §f" + targetFace.getName().toUpperCase() + " §emode set to " + modeText), true);
                        }
                        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.6F);
                    } else {
                        boolean nowDisconnected = pipeBe.toggleSideDisconnected(targetFace);
                        BlockState neighborState = level.getBlockState(pos.relative(targetFace));
                        String targetDesc = neighborState.isAir() ? targetFace.getName().toUpperCase() : neighborState.getBlock().getName().getString();

                        if (nowDisconnected) {
                            level.playSound(null, pos, SoundEvents.SPYGLASS_STOP_USING, SoundSource.BLOCKS, 1.0F, 1.2F);
                            if (player != null) {
                                player.displayClientMessage(Component.literal("§6[Wrench] §cDisconnected §f" + targetFace.getName().toUpperCase() + " §cside from §e" + targetDesc), true);
                            }
                        } else {
                            level.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0F, 1.4F);
                            if (player != null) {
                                player.displayClientMessage(Component.literal("§6[Wrench] §aConnected §f" + targetFace.getName().toUpperCase() + " §aside to §e" + targetDesc), true);
                            }
                        }
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        // 2b. Naiad Fluid Pipe
        if (block instanceof NaiadFluidPipeBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof NaiadFluidPipeBlockEntity naiadBe) {
                if (!level.isClientSide()) {
                    BlockHitResult bHit = new BlockHitResult(context.getClickLocation(), clickedFace, pos, false);
                    Direction targetFace = NaiadFluidPipeBlock.getTargetFaceFromHit(bHit, pos);

                    if (naiadBe.getValveMode(targetFace) != NaiadFluidPipeBlockEntity.ValveMode.NONE) {
                        NaiadFluidPipeBlockEntity.ValveMode next = naiadBe.cycleValveMode(targetFace);
                        String modeDesc = next == NaiadFluidPipeBlockEntity.ValveMode.EXTRACT ? "§6EXTRACT (Pulling)" : "§bINSERT (Pushing)";
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Wrench] §b[Naiad Pipe] §7Face §f" + targetFace.getName().toUpperCase() + " §7valve mode: " + modeDesc), true);
                        }
                        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.6F);
                    } else {
                        boolean nowDisconnected = naiadBe.toggleSideDisconnected(targetFace);
                        BlockState neighborState = level.getBlockState(pos.relative(targetFace));
                        String targetDesc = neighborState.isAir() ? targetFace.getName().toUpperCase() : neighborState.getBlock().getName().getString();

                        if (nowDisconnected) {
                            level.playSound(null, pos, SoundEvents.SPYGLASS_STOP_USING, SoundSource.BLOCKS, 1.0F, 1.2F);
                            if (player != null) {
                                player.displayClientMessage(Component.literal("§6[Wrench] §cDisconnected §f" + targetFace.getName().toUpperCase() + " §cside from §e" + targetDesc), true);
                            }
                        } else {
                            level.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0F, 1.4F);
                            if (player != null) {
                                player.displayClientMessage(Component.literal("§6[Wrench] §aConnected §f" + targetFace.getName().toUpperCase() + " §aside to §e" + targetDesc), true);
                            }
                        }
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        // 2c. Astrape Energy Wire
        if (block instanceof VolcaniteEnergyWireBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VolcaniteEnergyWireBlockEntity wireBe) {
                if (!level.isClientSide()) {
                    BlockHitResult bHit = new BlockHitResult(context.getClickLocation(), clickedFace, pos, false);
                    Direction targetFace = VolcaniteEnergyWireBlock.getTargetFaceFromHit(bHit, pos);
                    boolean nowDisconnected = wireBe.toggleSideDisconnected(targetFace);
                    BlockState neighborState = level.getBlockState(pos.relative(targetFace));
                    String targetDesc = neighborState.isAir() ? targetFace.getName().toUpperCase() : neighborState.getBlock().getName().getString();

                    if (nowDisconnected) {
                        level.playSound(null, pos, SoundEvents.SPYGLASS_STOP_USING, SoundSource.BLOCKS, 1.0F, 1.2F);
                        if (player != null) {
                            player.displayClientMessage(Component.literal(String.format("§6[Wrench] §cDisconnected §f%s §cside from §e%s §8| §6⚡ §a%,d RF/t",
                                targetFace.getName().toUpperCase(), targetDesc, wireBe.getEffectiveMaxTransfer())), true);
                        }
                    } else {
                        level.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0F, 1.4F);
                        if (player != null) {
                            player.displayClientMessage(Component.literal(String.format("§6[Wrench] §aConnected §f%s §aside to §e%s §8| §6⚡ §a%,d RF/t",
                                targetFace.getName().toUpperCase(), targetDesc, wireBe.getEffectiveMaxTransfer())), true);
                        }
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        // 2d. Volcanite Energy Cell & Advanced Energy Unit: Cycle Face I/O (INPUT -> OUTPUT -> BOTH -> DISABLED)
        if (block instanceof VolcaniteEnergyCellBlock && level.getBlockEntity(pos) instanceof VolcaniteEnergyCellBlockEntity cell) {
            if (!level.isClientSide()) {
                if (cell.isPartOfFormedMultiblock()) {
                    if (player != null) {
                        player.displayClientMessage(Component.literal("§6[Wrench] §ePart of formed Energy Matrix §7(Shift+Right-Click to dismantle)"), true);
                    }
                } else {
                    int next = cell.cycleFaceMode(clickedFace);
                    String modeText = switch (next) {
                        case VolcaniteEnergyCellBlockEntity.FACE_INPUT -> "§9📥 INPUT (Receive)";
                        case VolcaniteEnergyCellBlockEntity.FACE_OUTPUT -> "§6📤 OUTPUT (Push)";
                        case VolcaniteEnergyCellBlockEntity.FACE_NONE -> "§8✖ DISABLED";
                        default -> "§d⇄ BOTH (Auto-Balance)";
                    };
                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.55F, 1.45F);
                    if (player != null) {
                        player.displayClientMessage(Component.literal(String.format(
                                "§6[Wrench] §eFace §f%s §7➔ %s §8| §6⚡ §e%,d §7/ §b%,d RF",
                                clickedFace.getName().toUpperCase(), modeText, cell.energyStorage.amount, cell.getEffectiveCapacity())), true);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // =========================================================================
        // 3. CLICK ON A CONTAINER / MACHINE: TOGGLE CONNECTION WITH ADJACENT PIPES
        // =========================================================================
        for (Direction d : Direction.values()) {
            BlockPos neighborPos = pos.relative(d);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            Direction pipeSideTowardsClickedBlock = d.getOpposite();

            // 3a. Adjacent Storage Pipe, Hermes Item Pipe, or Universal Conduit
            if (neighborBe instanceof VolcaniteStoragePipeBlockEntity pipeBe) {
                if (!level.isClientSide()) {
                    boolean nowDisconnected = pipeBe.toggleSideDisconnected(pipeSideTowardsClickedBlock);
                    String pipeName = neighborBe.getBlockState().getBlock().getName().getString();
                    if (nowDisconnected) {
                        level.playSound(null, neighborPos, SoundEvents.SPYGLASS_STOP_USING, SoundSource.BLOCKS, 1.0F, 1.2F);
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Wrench] §cDisconnected §f" + pipeName + " §cfrom §e" + block.getName().getString()), true);
                        }
                    } else {
                        level.playSound(null, neighborPos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0F, 1.4F);
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Wrench] §aConnected §f" + pipeName + " §ato §e" + block.getName().getString()), true);
                        }
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            // 3b. Adjacent Naiad Fluid Pipe
            if (neighborBe instanceof NaiadFluidPipeBlockEntity naiadBe) {
                if (!level.isClientSide()) {
                    boolean nowDisconnected = naiadBe.toggleSideDisconnected(pipeSideTowardsClickedBlock);
                    if (nowDisconnected) {
                        level.playSound(null, neighborPos, SoundEvents.SPYGLASS_STOP_USING, SoundSource.BLOCKS, 1.0F, 1.2F);
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Wrench] §cDisconnected §fNaiad Fluid Pipe §cfrom §e" + block.getName().getString()), true);
                        }
                    } else {
                        level.playSound(null, neighborPos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0F, 1.4F);
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Wrench] §aConnected §fNaiad Fluid Pipe §ato §e" + block.getName().getString()), true);
                        }
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            // 3c. Adjacent Astrape Energy Wire
            if (neighborBe instanceof VolcaniteEnergyWireBlockEntity wireBe) {
                if (!level.isClientSide()) {
                    boolean nowDisconnected = wireBe.toggleSideDisconnected(pipeSideTowardsClickedBlock);
                    if (nowDisconnected) {
                        level.playSound(null, neighborPos, SoundEvents.SPYGLASS_STOP_USING, SoundSource.BLOCKS, 1.0F, 1.2F);
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Wrench] §cDisconnected §fAstrape Energy Wire §cfrom §e" + block.getName().getString()), true);
                        }
                    } else {
                        level.playSound(null, neighborPos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0F, 1.4F);
                        if (player != null) {
                            player.displayClientMessage(Component.literal("§6[Wrench] §aConnected §fAstrape Energy Wire §ato §e" + block.getName().getString()), true);
                        }
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        // =========================================================================
        // 4. MACHINE ROTATION (if no pipes were toggled)
        // =========================================================================
        DirectionProperty facingProp = null;
        for (Property<?> prop : state.getProperties()) {
            if (prop instanceof DirectionProperty dp) {
                if (prop.getName().equals("facing")) {
                    facingProp = dp;
                    break;
                }
            }
        }

        if (facingProp != null) {
            if (!level.isClientSide()) {
                Direction currentFacing = state.getValue(facingProp);
                Direction nextFacing;

                if (facingProp.getPossibleValues().contains(Direction.UP) || facingProp.getPossibleValues().contains(Direction.DOWN)) {
                    int nextIdx = (currentFacing.get3DDataValue() + 1) % 6;
                    nextFacing = Direction.from3DDataValue(nextIdx);
                    if (!facingProp.getPossibleValues().contains(nextFacing)) {
                        nextFacing = currentFacing.getClockWise();
                    }
                } else {
                    nextFacing = currentFacing.getClockWise();
                }

                if (facingProp.getPossibleValues().contains(nextFacing)) {
                    level.setBlock(pos, state.setValue(facingProp, nextFacing), 3);
                    level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.MACHINE_TOGGLE_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (player != null) {
                        team.reborn.energy.api.EnergyStorage st = team.reborn.energy.api.EnergyStorage.SIDED.find(level, pos, clickedFace);
                        if (st == null) st = team.reborn.energy.api.EnergyStorage.SIDED.find(level, pos, null);
                        if (st != null && st.getCapacity() > 0) {
                            player.displayClientMessage(Component.literal(String.format("§6[Wrench] §eFacing rotated to §f%s §8| §6⚡ §a%,d §7/ §e%,d RF",
                                nextFacing.getName().toUpperCase(), st.getAmount(), st.getCapacity())), true);
                        } else {
                            player.displayClientMessage(Component.literal("§6[Wrench] §eFacing rotated to §f" + nextFacing.getName().toUpperCase()), true);
                        }
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // =========================================================================
        // 5. ENERGY MULTIMETER / INSPECTOR (for batteries, generators, machines without rotation)
        // =========================================================================
        team.reborn.energy.api.EnergyStorage inspectedStorage = team.reborn.energy.api.EnergyStorage.SIDED.find(level, pos, clickedFace);
        if (inspectedStorage == null) {
            inspectedStorage = team.reborn.energy.api.EnergyStorage.SIDED.find(level, pos, null);
        }

        if (inspectedStorage != null && inspectedStorage.getCapacity() > 0) {
            if (!level.isClientSide() && player != null) {
                long amt = inspectedStorage.getAmount();
                long cap = inspectedStorage.getCapacity();
                double pct = (amt * 100.0) / (double) cap;
                String io = "";
                if (inspectedStorage.supportsInsertion() && inspectedStorage.supportsExtraction()) {
                    io = " §8[§aIN§8/§cOUT§8]";
                } else if (inspectedStorage.supportsInsertion()) {
                    io = " §8[§aIN§8]";
                } else if (inspectedStorage.supportsExtraction()) {
                    io = " §8[§cOUT§8]";
                }
                BlockEntity be = level.getBlockEntity(pos);
                String linksInfo = "";
                if (be instanceof net.valentin.obsidium.block.AstrapeEnergyPylonBlockEntity pylonBe) {
                    linksInfo = " §8| §bLinks: " + pylonBe.getLinkedPylons().size() + "/" + net.valentin.obsidium.block.AstrapeEnergyPylonBlockEntity.MAX_LINKS;
                }

                player.displayClientMessage(Component.literal(
                    String.format("§6⚡ [Energy Inspector] §e%s§8: §a%,d §7/ §e%,d RF §7(%.1f%%)%s%s",
                        block.getName().getString(), amt, cap, pct, io, linksInfo)
                ), true);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 1.8F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag isAdvanced) {
        tooltip.add(Component.literal("§6⚡ Right-click energy machines & cables to inspect RF buffer").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§eRight-click on pipes/wires or containers to Connect / Disconnect").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("§eRight-click on mounted modules to toggle INSERT / EXTRACT").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("§7Right-click on machines to rotate facing").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§6Shift-right-click to instantly dismantle pipes, wires & machines").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("§8Universal Precision Logistical Tool").withStyle(ChatFormatting.DARK_GRAY));
    }
}
