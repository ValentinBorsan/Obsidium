package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.block.VolcaniteStoragePipeBlock;
import net.valentin.obsidium.block.VolcaniteStoragePipeBlockEntity;
import net.valentin.obsidium.block.VolcaniteStoragePipeBlockEntity.ModuleType;

import java.util.List;

public class VolcaniteCableConnectorItem extends Item {
    private final boolean isExport;

    public VolcaniteCableConnectorItem(Properties properties, boolean isExport) {
        super(properties);
        this.isExport = isExport;
    }

    public boolean isExport() {
        return isExport;
    }

    public ModuleType getConnectorModuleType() {
        // Import Connector pulls FROM chest/machine INTO network (ModuleType.EXTRACT)
        // Export Connector pushes FROM network INTO chest/machine (ModuleType.INSERT)
        return isExport ? ModuleType.INSERT : ModuleType.EXTRACT;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);

        // Case 0: Clicked on a Wireless Node (Emitter or Receiver)
        if (state.getBlock() instanceof net.valentin.obsidium.block.VolcaniteNetworkNodeEmitterBlock ||
            state.getBlock() instanceof net.valentin.obsidium.block.VolcaniteNetworkNodeReceiverBlock) {
            if (!level.isClientSide() && player != null) {
                player.displayClientMessage(Component.literal("§6[Wireless Node] §eWireless Nodes are network relays, not item storage! Optic Fiber connects directly to them without connectors. Place connectors on chests/machines."), true);
            }
            return InteractionResult.SUCCESS;
        }

        // Case 1: Clicked directly on a Volcanite Optic Fiber / Pipe
        if (be instanceof VolcaniteStoragePipeBlockEntity pipeBe) {
            Direction targetFace = findBestContainerFace(level, pos, context.getClickedFace(), pipeBe);
            if (targetFace == null) {
                if (!level.isClientSide() && player != null) {
                    player.displayClientMessage(Component.literal("§c[Fiber Connector] No adjacent chest or machine found to connect!"), true);
                }
                return InteractionResult.FAIL;
            }
            return attachConnector(level, pos, pipeBe, targetFace, player, stack);
        }

        // Case 2: Clicked on a Chest or Machine adjacent to a Volcanite Optic Fiber
        if (VolcaniteStoragePipeBlockEntity.isExternalContainer(level, pos, be)) {
            // First check the clicked face for an adjacent fiber cable
            Direction clickedFace = context.getClickedFace();
            BlockPos adjPos = pos.relative(clickedFace);
            if (level.getBlockEntity(adjPos) instanceof VolcaniteStoragePipeBlockEntity pipeBe) {
                return attachConnector(level, adjPos, pipeBe, clickedFace.getOpposite(), player, stack);
            }
            // Otherwise check all 6 sides of the chest/machine for an adjacent fiber cable
            for (Direction d : Direction.values()) {
                BlockPos neighborPos = pos.relative(d);
                if (level.getBlockEntity(neighborPos) instanceof VolcaniteStoragePipeBlockEntity neighborPipe) {
                    return attachConnector(level, neighborPos, neighborPipe, d.getOpposite(), player, stack);
                }
            }
        }

        return InteractionResult.PASS;
    }

    public static Direction findBestContainerFace(Level level, BlockPos pipePos, Direction preferredFace, VolcaniteStoragePipeBlockEntity pipeBe) {
        // 1. Check if preferredFace points to an external container/machine
        if (preferredFace != null) {
            BlockPos targetPos = pipePos.relative(preferredFace);
            if (VolcaniteStoragePipeBlockEntity.isExternalContainer(level, targetPos, level.getBlockEntity(targetPos))) {
                return preferredFace;
            }
        }
        // 2. Check all 6 directions for an external container/machine without a connector yet
        Direction fallback = null;
        for (Direction d : Direction.values()) {
            BlockPos targetPos = pipePos.relative(d);
            if (VolcaniteStoragePipeBlockEntity.isExternalContainer(level, targetPos, level.getBlockEntity(targetPos))) {
                if (pipeBe.getModule(d) == ModuleType.NONE) {
                    return d;
                }
                if (fallback == null) fallback = d;
            }
        }
        return fallback;
    }

    public InteractionResult attachConnector(Level level, BlockPos pipePos, VolcaniteStoragePipeBlockEntity pipeBe, Direction face, Player player, ItemStack stack) {
        if (!level.isClientSide()) {
            ModuleType desired = getConnectorModuleType();
            ModuleType current = pipeBe.getModule(face);

            if (current == desired) {
                // Already has this exact connector -> open its GUI directly!
                pipeBe.setSelectedFaceForMenu(face);
                if (player != null) {
                    player.openMenu(pipeBe);
                }
                return InteractionResult.SUCCESS;
            }

            // If replacing the opposite connector type, refund the old one
            if (current != ModuleType.NONE && player != null) {
                ItemStack refund = VolcaniteStoragePipeBlock.getConnectorDropForModule(pipeBe, current);
                if (!player.getInventory().add(refund)) {
                    player.drop(refund, false);
                }
            }

            // Ensure side is not manually disconnected
            if (pipeBe.isSideDisconnected(face)) {
                pipeBe.toggleSideDisconnected(face);
            }

            pipeBe.setModule(face, desired);

            // Refresh fiber blockstate connections so the cable arm snaps to the chest/machine
            BlockState pipeState = level.getBlockState(pipePos);
            if (pipeState.getBlock() instanceof VolcaniteStoragePipeBlock pipeBlock) {
                BlockState updated = pipeBlock.makeConnections(level, pipePos)
                        .setValue(VolcaniteStoragePipeBlock.WATERLOGGED, pipeState.getValue(VolcaniteStoragePipeBlock.WATERLOGGED));
                level.setBlock(pipePos, updated, 3);
            }

            if (player != null && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            level.playSound(null, pipePos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.BLOCKS, 1.0F, isExport ? 1.1F : 1.35F);
            if (player != null) {
                String label = isExport ? "§6📤 Export Connector (Network -> Container)" : "§b📥 Import Connector (Container -> Network)";
                player.displayClientMessage(Component.literal("§6[Optic Fiber] §aAttached " + label + " §aon §f" + face.getName().toUpperCase() + " §aside!"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (isExport) {
            tooltip.add(Component.literal("§6📤 Network Export Connector").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("§7Directie: §eRețea -> Chest/Mașinărie §7(Scoate din sistem)"));
            tooltip.add(Component.literal("§7Pushes items or fluids §efrom Storage Network §7into a chest or machine."));
        } else {
            tooltip.add(Component.literal("§b📥 Network Import Connector").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("§7Directie: §bChest/Mașinărie -> Rețea §7(Introduce în sistem)"));
            tooltip.add(Component.literal("§7Pulls items or fluids §efrom a chest or machine §7into Storage Network."));
        }
        tooltip.add(Component.literal("§8• Data-Only: Does not transmit RF energy."));
        tooltip.add(Component.literal("§eRight-Click §7on a Volcanite Optic Fiber or adjacent chest/machine to attach."));
    }
}
