package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcanitePipeEnergyUpgradeItem;
import team.reborn.energy.api.EnergyStorage;

import java.util.concurrent.atomic.AtomicReference;

public class VolcanitePipeModuleMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 20;

    private final VolcaniteStoragePipeBlockEntity pipeBe;
    private final AtomicReference<Direction> sideRef;
    private final ContainerData data;
    private final SimpleContainer filterContainer = new SimpleContainer(3);
    private final SimpleContainer upgradeContainer = new SimpleContainer(1);
    private boolean updatingSlots = false;

    public VolcanitePipeModuleMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null, new AtomicReference<>(Direction.NORTH), new SimpleContainerData(DATA_COUNT));
    }

    public VolcanitePipeModuleMenu(int syncId, Inventory playerInventory, VolcaniteStoragePipeBlockEntity pipeBe, Direction side) {
        this(syncId, playerInventory, pipeBe, new AtomicReference<>(side));
    }

    private VolcanitePipeModuleMenu(int syncId, Inventory playerInventory, VolcaniteStoragePipeBlockEntity pipeBe, AtomicReference<Direction> sideRef) {
        this(syncId, playerInventory, pipeBe, sideRef, createContainerData(pipeBe, sideRef));
    }

    private VolcanitePipeModuleMenu(int syncId, Inventory playerInventory, VolcaniteStoragePipeBlockEntity pipeBe, AtomicReference<Direction> sideRef, ContainerData data) {
        super(ObsidiumMod.VOLCANITE_PIPE_MODULE_MENU, syncId);
        this.pipeBe = pipeBe;
        this.sideRef = sideRef;
        this.data = data;
        checkContainerDataCount(data, DATA_COUNT);

        refreshSlotsForCurrentSide();

        // Slots 0, 1, 2: Built-in 3 Ghost Filter Slots at (12 + i * 19, 97)
        for (int i = 0; i < 3; i++) {
            final int slotIdx = i;
            this.addSlot(new Slot(filterContainer, slotIdx, 12 + i * 19, 97) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return true;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false; // Ghost slot - handled in clicked()
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    if (!updatingSlots && pipeBe != null) {
                        pipeBe.setFilterItem(sideRef.get(), slotIdx, filterContainer.getItem(slotIdx));
                    }
                }
            });
        }

        // Slot 3: Upgrade Slot (Energy, Overclock, or Volcanite Stack Upgrade) at (146, 97)
        this.addSlot(new Slot(upgradeContainer, 0, 146, 97) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (stack.getItem() instanceof net.valentin.obsidium.item.VolcaniteStackUpgradeItem) return true;
                if (stack.getItem() instanceof net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem) return true;
                if (stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem) {
                    return pipeBe == null || !pipeBe.isOpticFiber();
                }
                return false;
            }

            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                if (!updatingSlots && pipeBe != null) {
                    Direction curSide = sideRef.get();
                    ItemStack inSlot = upgradeContainer.getItem(0);
                    if (inSlot.getItem() instanceof net.valentin.obsidium.item.VolcaniteStackUpgradeItem) {
                        pipeBe.setEnergyUpgrade(curSide, 3, inSlot);
                    } else if (inSlot.getItem() instanceof VolcanitePipeEnergyUpgradeItem up) {
                        pipeBe.setEnergyUpgrade(curSide, up.getTier(), inSlot);
                    } else if (inSlot.getItem() instanceof net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem oc) {
                        pipeBe.setEnergyUpgrade(curSide, oc.getTier(), inSlot);
                    } else {
                        pipeBe.setEnergyUpgrade(curSide, 0, ItemStack.EMPTY);
                    }
                }
            }
        });

        // Player Inventory (Slots 4..30) at y = 138
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 138 + row * 18));
            }
        }

        // Hotbar (Slots 31..39) at y = 196
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 196));
        }

        this.addDataSlots(data);
    }

    private void refreshSlotsForCurrentSide() {
        if (pipeBe == null) return;
        updatingSlots = true;
        try {
            Direction curSide = sideRef.get();
            for (int i = 0; i < 3; i++) {
                filterContainer.setItem(i, pipeBe.getFilterItem(curSide, i));
            }
            upgradeContainer.setItem(0, pipeBe.getEnergyUpgradeItem(curSide));
        } finally {
            updatingSlots = false;
        }
    }

    private static int getFluidTypeId(FluidVariant variant) {
        if (variant == null || variant.isBlank()) return 0;
        Fluid fluid = variant.getFluid();
        if (fluid == Fluids.LAVA) return 1;
        if (fluid == Fluids.WATER) return 2;
        if (fluid == ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID) return 3;
        if (fluid == ObsidiumMod.STEAM_FLUID) return 4;
        if (fluid == ObsidiumMod.BIO_OIL_FLUID) return 5;
        if (fluid == ObsidiumMod.BIO_ETHANOL_FLUID) return 6;
        if (fluid == ObsidiumMod.BIODIESEL_FLUID) return 7;
        if (fluid == ObsidiumMod.PYRO_DIESEL_FLUID) return 8;
        return 0;
    }

    private static ContainerData createContainerData(VolcaniteStoragePipeBlockEntity pipeBe, AtomicReference<Direction> sideRef) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (pipeBe == null) return 0;
                Direction curSide = sideRef.get();
                switch (index) {
                    case 0:
                        return pipeBe.getModule(curSide).getId();
                    case 1:
                        return curSide.get3DDataValue();
                    case 2:
                        return pipeBe.getTransferType(curSide);
                    case 3:
                        return pipeBe.getEnergyUpgradeTier(curSide);
                    case 4:
                        if (pipeBe.getLevel() != null) {
                            BlockPos targetPos = pipeBe.getBlockPos().relative(curSide);
                            BlockState s = pipeBe.getLevel().getBlockState(targetPos);
                            return s.isAir() ? 0 : BuiltInRegistries.BLOCK.getId(s.getBlock());
                        }
                        return 0;
                    case 5:
                        if (pipeBe.getLevel() != null) {
                            BlockPos targetPos = pipeBe.getBlockPos().relative(curSide);
                            return EnergyStorage.SIDED.find(pipeBe.getLevel(), targetPos, curSide.getOpposite()) != null ? 1 : 0;
                        }
                        return 0;
                    case 6:
                        if (pipeBe instanceof VolcaniteUniversalConduitBlockEntity conduit && conduit.getStoredEnergy() > 0) {
                            return 1;
                        }
                        VolcaniteStorageControllerBlockEntity ctrl = pipeBe.findConnectedController();
                        return (ctrl != null && ctrl.isPowered()) ? 1 : 0;
                    case 7:
                        if (pipeBe instanceof VolcaniteUniversalConduitBlockEntity conduit && conduit.getStoredEnergy() > 0 && conduit.getMaxEnergy() > 0) {
                            return (int) ((conduit.getStoredEnergy() * 100L) / conduit.getMaxEnergy());
                        }
                        VolcaniteStorageControllerBlockEntity ctrlPct = pipeBe.findConnectedController();
                        if (ctrlPct != null && ctrlPct.getEnergyStorage().getCapacity() > 0) {
                            return (int) ((ctrlPct.getEnergyStorage().getAmount() * 100L) / ctrlPct.getEnergyStorage().getCapacity());
                        }
                        return 0;
                    case 8:
                        return pipeBe.isFilterBlacklist(curSide) ? 1 : 0;
                    case 9:
                        return pipeBe.supportsEnergyInGui() ? 1 : 0;
                    case 10:
                        if (pipeBe instanceof VolcaniteUniversalConduitBlockEntity) return 2;
                        return pipeBe.canSwitchTransferType() ? 1 : 0;
                    case 11:
                        return pipeBe.isOpticFiber() ? 1 : 0;
                    case 12:
                        return pipeBe.getPriority(curSide);
                    case 13:
                        return pipeBe.isRoundRobin(curSide) ? 1 : 0;
                    case 14:
                        return pipeBe.getRedstoneMode(curSide);
                    case 15:
                        if (pipeBe instanceof VolcaniteUniversalConduitBlockEntity conduit) {
                            return (int) Math.min(32000L, conduit.getStoredEnergy() / 100L);
                        }
                        return 0;
                    case 16:
                        if (pipeBe instanceof VolcaniteUniversalConduitBlockEntity conduit) {
                            return Math.min(32000, conduit.getStoredFluidMb());
                        }
                        return 0;
                    case 17:
                        if (pipeBe instanceof VolcaniteUniversalConduitBlockEntity conduit) {
                            return getFluidTypeId(conduit.getStoredFluidVariant());
                        }
                        return 0;
                    case 18: {
                        int mask = 0;
                        if (pipeBe.getLevel() != null) {
                            for (Direction d : Direction.values()) {
                                if (VolcaniteStoragePipeBlock.connectsTo(pipeBe.getLevel(), pipeBe.getBlockPos(), d, pipeBe)) {
                                    mask |= (1 << d.get3DDataValue());
                                }
                            }
                        }
                        return mask;
                    }
                    case 19: {
                        int mask = 0;
                        for (Direction d : Direction.values()) {
                            if (pipeBe.getModule(d) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE && !pipeBe.isSideDisconnected(d)) {
                                mask |= (1 << d.get3DDataValue());
                            }
                        }
                        return mask;
                    }
                    default:
                        return 0;
                }
            }

            @Override
            public void set(int index, int value) {
                if (pipeBe == null) return;
                Direction curSide = sideRef.get();
                if (index == 0) {
                    pipeBe.setModule(curSide, VolcaniteStoragePipeBlockEntity.ModuleType.byId(value));
                } else if (index == 1) {
                    sideRef.set(Direction.from3DDataValue(value));
                } else if (index == 2) {
                    pipeBe.setTransferType(curSide, value);
                } else if (index == 3) {
                    pipeBe.setEnergyUpgrade(curSide, value, pipeBe.getEnergyUpgradeItem(curSide));
                } else if (index == 8) {
                    pipeBe.setFilterBlacklist(curSide, value == 1);
                } else if (index == 12) {
                    pipeBe.setPriority(curSide, value);
                } else if (index == 13) {
                    pipeBe.setRoundRobin(curSide, value == 1);
                } else if (index == 14) {
                    pipeBe.setRedstoneMode(curSide, value);
                }
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 3) {
            ItemStack carried = getCarried();
            Direction curSide = sideRef.get();
            if (carried.isEmpty()) {
                filterContainer.setItem(slotId, ItemStack.EMPTY);
                if (pipeBe != null) pipeBe.setFilterItem(curSide, slotId, ItemStack.EMPTY);
            } else {
                ItemStack ghost = carried.copyWithCount(1);
                filterContainer.setItem(slotId, ghost);
                if (pipeBe != null) pipeBe.setFilterItem(curSide, slotId, ghost);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    public int getModuleMode() {
        return this.data.get(0);
    }

    public Direction getSide() {
        return Direction.from3DDataValue(this.data.get(1));
    }

    public int getTransferType() {
        return this.data.get(2);
    }

    public int getEnergyUpgradeTier() {
        return this.data.get(3);
    }

    public int getConnectedMachineBlockId() {
        return this.data.get(4);
    }

    public boolean getTargetHasEnergy() {
        return this.data.get(5) == 1;
    }

    public boolean isNetworkPowered() {
        return this.data.get(6) == 1;
    }

    public int getNetworkEnergyPercent() {
        return this.data.get(7);
    }

    public boolean isFilterBlacklist() {
        return this.data.get(8) == 1;
    }

    public boolean supportsEnergy() {
        return this.data.get(9) == 1;
    }

    public boolean canSwitchType() {
        return this.data.get(10) >= 1;
    }

    public boolean isUniversalConduit() {
        return this.data.get(10) == 2;
    }

    public boolean isOpticFiber() {
        return this.data.get(11) == 1;
    }

    public int getPriority() {
        return this.data.get(12);
    }

    public boolean isRoundRobin() {
        return this.data.get(13) == 1;
    }

    public int getRedstoneMode() {
        return this.data.get(14);
    }

    public int getStoredEnergyHundreds() {
        return this.data.get(15);
    }

    public int getStoredFluidMb() {
        return this.data.get(16);
    }

    public int getStoredFluidTypeId() {
        return this.data.get(17);
    }

    public int getConnectedFacesMask() {
        return this.data.get(18);
    }

    public int getActiveFacesMask() {
        return this.data.get(19);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        Direction curSide = sideRef.get();
        if (id == 0) {
            if (isOpticFiber()) {
                return false;
            }
            int current = getModuleMode();
            int next;
            if (isUniversalConduit()) {
                next = (current + 1) % 3; // 0 (AUTO) -> 1 (IMPORT/EXTRACT) -> 2 (EXPORT/INSERT)
            } else {
                next = (current == 1) ? 2 : 1;
            }
            this.data.set(0, next);
            if (pipeBe != null) {
                pipeBe.setModule(curSide, VolcaniteStoragePipeBlockEntity.ModuleType.byId(next));
            }
            broadcastChanges();
            return true;
        } else if (id == 1) {
            if (!canSwitchType()) return false;
            int current = getTransferType();
            int next = isUniversalConduit() ? ((current + 1) % 4) : ((current == 0) ? 1 : 0);
            this.data.set(2, next);
            if (pipeBe != null) {
                pipeBe.setTransferType(curSide, next);
            }
            broadcastChanges();
            return true;
        } else if (id == 2) {
            boolean next = !isFilterBlacklist();
            this.data.set(8, next ? 1 : 0);
            if (pipeBe != null) {
                pipeBe.setFilterBlacklist(curSide, next);
            }
            broadcastChanges();
            return true;
        } else if (id == 3) {
            for (int i = 0; i < 3; i++) {
                filterContainer.setItem(i, ItemStack.EMPTY);
            }
            if (pipeBe != null) {
                pipeBe.clearFilters(curSide);
            }
            broadcastChanges();
            return true;
        } else if (id == 4) {
            if (pipeBe != null) {
                int next = pipeBe.adjustPriority(curSide, -1);
                this.data.set(12, next);
            }
            broadcastChanges();
            return true;
        } else if (id == 5) {
            if (pipeBe != null) {
                int next = pipeBe.adjustPriority(curSide, 1);
                this.data.set(12, next);
            }
            broadcastChanges();
            return true;
        } else if (id == 6) {
            if (pipeBe != null) {
                boolean next = pipeBe.toggleRoundRobin(curSide);
                this.data.set(13, next ? 1 : 0);
            }
            broadcastChanges();
            return true;
        } else if (id == 7) {
            if (pipeBe != null) {
                int next = pipeBe.cycleRedstoneMode(curSide);
                this.data.set(14, next);
            }
            broadcastChanges();
            return true;
        } else if (id >= 10 && id < 16) {
            Direction nextSide = Direction.from3DDataValue(id - 10);
            sideRef.set(nextSide);
            if (pipeBe != null) {
                pipeBe.setSelectedFaceForMenu(nextSide);
            }
            refreshSlotsForCurrentSide();
            broadcastChanges();
            return true;
        } else if (id == 16) {
            if (pipeBe != null && canSwitchType()) {
                pipeBe.setAllFacesTransferType(pipeBe.getTransferType(curSide));
            }
            broadcastChanges();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            Direction curSide = sideRef.get();
            if (index >= 0 && index < 3) {
                slot.set(ItemStack.EMPTY);
                if (pipeBe != null) pipeBe.setFilterItem(curSide, index, ItemStack.EMPTY);
                return ItemStack.EMPTY;
            } else if (index == 3) {
                if (!this.moveItemStackTo(stack, 4, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.setChanged();
                return ItemStack.EMPTY;
            } else {
                if (stack.getItem() instanceof net.valentin.obsidium.item.VolcaniteStackUpgradeItem
                        || (stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem && !isOpticFiber())
                        || stack.getItem() instanceof net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem) {
                    if (this.slots.get(3).getItem().isEmpty()) {
                        this.slots.get(3).set(stack.split(1));
                        this.slots.get(3).setChanged();
                        return ItemStack.EMPTY;
                    }
                }
                for (int f = 0; f < 3; f++) {
                    if (this.slots.get(f).getItem().isEmpty()) {
                        this.slots.get(f).set(stack.copyWithCount(1));
                        if (pipeBe != null) pipeBe.setFilterItem(curSide, f, stack.copyWithCount(1));
                        return ItemStack.EMPTY;
                    }
                }
                this.slots.get(0).set(stack.copyWithCount(1));
                if (pipeBe != null) pipeBe.setFilterItem(curSide, 0, stack.copyWithCount(1));
                return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (pipeBe == null || pipeBe.isRemoved() || pipeBe.getLevel() == null) {
            return false;
        }
        return player.distanceToSqr(
                pipeBe.getBlockPos().getX() + 0.5,
                pipeBe.getBlockPos().getY() + 0.5,
                pipeBe.getBlockPos().getZ() + 0.5
        ) <= 64.0;
    }
}
