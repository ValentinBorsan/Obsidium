package net.valentin.mythicrails;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Menu for Fluid & Energy rail stations (1 Aux slot + telemetry + player inventory). */
public final class RailUtilityStationMenu extends AbstractContainerMenu {
    private final Level level;
    private final BlockPos pos;
    private final Player owner;
    private final ContainerData data;
    private final Container aux;

    public RailUtilityStationMenu(int id, Inventory inventory) {
        this(id, inventory, null, BlockPos.ZERO);
    }

    public RailUtilityStationMenu(int id, Inventory inventory, Level level, BlockPos pos) {
        super(MythicRails.UTILITY_STATION_MENU, id);
        this.level = level;
        this.pos = pos.immutable();
        this.owner = inventory.player;
        this.aux = level == null ? new SimpleContainer(1)
                : (level.getBlockEntity(pos) instanceof RailUtilityStationBlockEntity be ? be : new SimpleContainer(1));

        this.data = level == null ? new SimpleContainerData(12) : new ContainerData() {
            @Override
            public int get(int index) {
                if (!available()) return 0;
                var state = level.getBlockState(pos);
                var station = (RailUtilityStationBlock) state.getBlock();
                var be = (RailUtilityStationBlockEntity) level.getBlockEntity(pos);
                var cart = findCart();
                return switch (index) {
                    case 0 -> state.getValue(RailUtilityStationBlock.FACING).get2DDataValue();
                    case 1 -> state.getValue(RailUtilityStationBlock.ENABLED) ? 1 : 0;
                    case 2 -> station.kind().isFluid ? 1 : 0;
                    case 3 -> station.kind().isLoader ? 1 : 0;
                    case 4 -> cart != null ? cart.getDockStatus() : 0;
                    case 5 -> be.fluidMb() / 10;
                    case 6 -> be.fluidTypeCode();
                    case 7 -> be.energyAmount() / 100;
                    case 8 -> FreightStationBlock.hasDepartureSignal(level, pos) ? 1 : 0;
                    case 9 -> cart == null ? 0 : 1;
                    case 10 -> cart == null ? 0 : (station.kind().isFluid ? cart.fluidAmount() / 10 : cart.energy() / 100);
                    case 11 -> cart == null ? 2000 : cart.maxEnergy() / 100;
                    default -> 0;
                };
            }

            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 12; }
        };

        addSlot(new Slot(aux, 0, 14, 62));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, 23 + col * 18, 152 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 23 + col * 18, 210));
        }
        addDataSlots(data);
    }

    private boolean available() {
        return level != null && level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                && level.getBlockState(pos).getBlock() instanceof RailUtilityStationBlock
                && level.getBlockEntity(pos) instanceof RailUtilityStationBlockEntity be && be == aux;
    }

    private HermesFreightCart findCart() {
        return level.getEntitiesOfClass(HermesFreightCart.class, new AABB(pos.above()).inflate(0.1, 0.2, 0.1)).stream()
                .filter(c -> !c.isRemoved())
                .min(java.util.Comparator.comparingDouble(c -> c.distanceToSqr(Vec3.atCenterOf(pos.above()))))
                .orElse(null);
    }

    public int value(int index) { return data.get(index); }

    @Override
    public boolean stillValid(Player player) {
        return level == null || (player == owner && player.level() == level && available()
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var result = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level == null || level.isClientSide() || !stillValid(player)) return false;
        var state = level.getBlockState(pos);
        if (id == 0) {
            level.setBlock(pos, state.cycle(RailUtilityStationBlock.ENABLED), 3);
            return true;
        }
        if (id == 1 || id == 2) {
            if (findCart() != null && state.getValue(RailUtilityStationBlock.ENABLED)) return false;
            var dir = state.getValue(RailUtilityStationBlock.FACING);
            level.setBlock(pos, state.setValue(RailUtilityStationBlock.FACING,
                    id == 1 ? dir.getCounterClockWise() : dir.getClockWise()), 3);
            return true;
        }
        if (id == 3) {
            var cart = findCart();
            return cart != null && cart.releaseFromStation(pos);
        }
        return false;
    }
}
