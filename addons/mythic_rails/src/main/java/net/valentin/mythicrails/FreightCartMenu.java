package net.valentin.mythicrails;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Modular cart menu: 54 cargo slots + 4 modular slots (Motor, Payload, Fuel, Aux) + telemetry. */
public final class FreightCartMenu extends AbstractContainerMenu {
    private final Container container;
    private final HermesFreightCart cart;
    private final ContainerData data;

    public FreightCartMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(HermesFreightCart.TOTAL_SLOTS));
    }

    public FreightCartMenu(int id, Inventory inventory, Container container) {
        super(MythicRails.FREIGHT_MENU, id);
        checkContainerSize(container, HermesFreightCart.TOTAL_SLOTS);
        this.container = container;
        this.cart = container instanceof HermesFreightCart fc ? fc : null;
        container.startOpen(inventory.player);

        this.data = this.cart == null ? new SimpleContainerData(11) : new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> cart.running() ? 1 : 0;
                    case 1 -> cart.heading().get2DDataValue();
                    case 2 -> cart.motorType().id;
                    case 3 -> cart.payloadType().id;
                    case 4 -> (cart.fuelTicks() + 19) / 20;
                    case 5 -> (cart.totalFuelTicks() + 19) / 20;
                    case 6 -> cart.energy() / 100;
                    case 7 -> cart.maxEnergy() / 100;
                    case 8 -> cart.fluidAmount() / 10;
                    case 9 -> cart.fluidType();
                    case 10 -> cart.workProgress();
                    default -> 0;
                };
            }

            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 11; }
        };

        // 0..53: Cargo slots (6 rows x 9 cols)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(container, row * 9 + col, 23 + col * 18, 84 + row * 18));
            }
        }

        // 54: Motor slot
        addSlot(new Slot(container, HermesFreightCart.SLOT_MOTOR, 10, 26) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof CartModuleItem mod && mod.isMotor();
            }
            @Override public int getMaxStackSize() { return 1; }
        });

        // 55: Payload Module slot
        addSlot(new Slot(container, HermesFreightCart.SLOT_MODULE, 10, 48) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof CartModuleItem mod && mod.isPayload();
            }
            @Override public int getMaxStackSize() { return 1; }
        });

        // 56: Fuel slot
        addSlot(new Slot(container, HermesFreightCart.SLOT_FUEL, 32, 26) {
            @Override public boolean mayPlace(ItemStack stack) {
                return HermesFreightCart.acceptsFuel(stack);
            }
        });

        // 57: Aux slot (buckets / redstone / batteries)
        addSlot(new Slot(container, HermesFreightCart.SLOT_AUX, 32, 48));

        // Player inventory (27) + hotbar (9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, 23 + col * 18, 198 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 23 + col * 18, 256));
        }

        addDataSlots(data);
    }

    public int value(int index) { return data.get(index); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (cart == null || player.level().isClientSide() || !stillValid(player)) return false;
        if (id == 0) {
            if (cart.motorType() == CartModuleItem.MotorType.NONE && !cart.running()) return false;
            cart.setRunning(!cart.running());
            return true;
        }
        if ((id == 1 || id == 2) && !cart.running()) {
            var dir = cart.heading();
            cart.setHeading(id == 1 ? dir.getCounterClockWise() : dir.getClockWise());
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index < HermesFreightCart.TOTAL_SLOTS) {
            if (!moveItemStackTo(stack, HermesFreightCart.TOTAL_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (stack.getItem() instanceof CartModuleItem mod) {
                if (mod.isMotor() && moveItemStackTo(stack, HermesFreightCart.SLOT_MOTOR, HermesFreightCart.SLOT_MOTOR + 1, false)) {
                    // moved to motor slot
                } else if (mod.isPayload() && moveItemStackTo(stack, HermesFreightCart.SLOT_MODULE, HermesFreightCart.SLOT_MODULE + 1, false)) {
                    // moved to module slot
                } else if (!moveItemStackTo(stack, 0, HermesFreightCart.CARGO_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, HermesFreightCart.CARGO_SIZE, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
