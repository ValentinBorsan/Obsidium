package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem;
import net.valentin.obsidium.item.VolcanitePipeEnergyUpgradeItem;
import net.valentin.obsidium.sound.ModSounds;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class AdvancedVolcaniteFurnaceBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, WorldlyContainer, MachineInventory {
    public static final int INVENTORY_SIZE = 9;
    // 0, 1, 2 = Input 1..3
    // 3, 4, 5 = Output 1..3
    // 6       = Shared Volcanic Ash Output
    // 7, 8    = Upgrade Slots
    public static final int DATA_COUNT = 9;
    public static final int MAX_PROGRESS = 50; // 2x faster per slot than Basic (100 ticks)
    public static final int RF_PER_CHAMBER_TICK = 80; // 80 RF/t per chamber -> 240 RF/t for 3 chambers (3x Basic)

    private final int[] progress = new int[3];
    private int coilTemp = 300; // 300°C .. 2500°C (Thermal Momentum >= 1500°C gives +25% speed)
    private boolean autoSplit = true;
    private boolean autoEject = true;

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot >= 0 && slot <= 2) return true;
            if (slot == 7 || slot == 8) return isValidUpgrade(stack);
            return false;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            AdvancedVolcaniteFurnaceBlockEntity.this.setChanged();
        }
    };

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(250_000, 25_000, 0) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress[0];
                case 1 -> progress[1];
                case 2 -> progress[2];
                case 3 -> (int) (energyStorage.amount >> 16);
                case 4 -> (int) (energyStorage.amount & 0xFFFF);
                case 5 -> isThermalOverclocked(level, worldPosition) ? 1 : 0;
                case 6 -> coilTemp;
                case 7 -> autoSplit ? 1 : 0;
                case 8 -> autoEject ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress[0] = value;
                case 1 -> progress[1] = value;
                case 2 -> progress[2] = value;
                case 6 -> coilTemp = value;
                case 7 -> autoSplit = value != 0;
                case 8 -> autoEject = value != 0;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AdvancedVolcaniteFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.ADVANCED_VOLCANITE_FURNACE_BE, pos, state);
    }

    public static boolean isValidUpgrade(ItemStack stack) {
        return VolcaniteOverclockUpgradeItem.isOverclockUpgrade(stack)
                || stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem;
    }

    public void toggleAutoSplit() {
        this.autoSplit = !this.autoSplit;
        setChanged();
    }

    public void toggleAutoEject() {
        this.autoEject = !this.autoEject;
        setChanged();
    }

    public float getUpgradeSpeedMultiplier() {
        float bonus = 1.0f;
        for (int i = 7; i <= 8; i++) {
            ItemStack up = inventory.getItem(i);
            if (VolcaniteOverclockUpgradeItem.isOverclockUpgrade(up)) {
                bonus *= VolcaniteOverclockUpgradeItem.getSpeedBonus(up);
            } else if (up.getItem() instanceof VolcanitePipeEnergyUpgradeItem pipeUp) {
                bonus += 0.35f * pipeUp.getTier();
            }
        }
        return bonus;
    }

    public float getUpgradeEnergyMultiplier() {
        float mult = 1.0f;
        for (int i = 7; i <= 8; i++) {
            ItemStack up = inventory.getItem(i);
            if (VolcaniteOverclockUpgradeItem.isOverclockUpgrade(up)) {
                mult *= VolcaniteOverclockUpgradeItem.getEnergyCostMultiplier(up);
            } else if (up.getItem() instanceof VolcanitePipeEnergyUpgradeItem pipeUp) {
                mult *= Math.max(0.55f, 1.0f - 0.15f * pipeUp.getTier());
            }
        }
        return mult;
    }

    private static boolean isOreOrDustForBonus(ItemStack input) {
        if (input.isEmpty()) return false;
        return input.is(Items.RAW_IRON) || input.is(Items.RAW_GOLD) || input.is(Items.RAW_COPPER)
                || input.is(ObsidiumMod.RAW_TIN) || input.is(ObsidiumMod.RAW_LEAD) || input.is(ObsidiumMod.RAW_SILVER)
                || input.is(ObsidiumMod.IRON_DUST) || input.is(ObsidiumMod.GOLD_DUST) || input.is(ObsidiumMod.COPPER_DUST)
                || input.is(ObsidiumMod.TIN_DUST) || input.is(ObsidiumMod.LEAD_DUST) || input.is(ObsidiumMod.SILVER_DUST)
                || input.is(ObsidiumMod.VOLCANITE_DUST);
    }

    private void balanceInputSlots() {
        if (!autoSplit) return;
        for (int src = 0; src < 3; src++) {
            ItemStack sStack = inventory.getItem(src);
            if (sStack.getCount() <= 1) continue;
            for (int dst = 0; dst < 3; dst++) {
                if (src == dst) continue;
                ItemStack dStack = inventory.getItem(dst);
                if (dStack.isEmpty()) {
                    int move = sStack.getCount() / 2;
                    if (move > 0) {
                        ItemStack split = sStack.split(move);
                        inventory.setItem(dst, split);
                        setChanged();
                        return;
                    }
                } else if (ItemStack.isSameItemSameComponents(sStack, dStack) && sStack.getCount() - dStack.getCount() >= 2) {
                    int move = (sStack.getCount() - dStack.getCount()) / 2;
                    if (move > 0 && dStack.getCount() + move <= dStack.getMaxStackSize()) {
                        sStack.shrink(move);
                        dStack.grow(move);
                        setChanged();
                        return;
                    }
                }
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AdvancedVolcaniteFurnaceBlockEntity entity) {
        if (level.isClientSide()) return;

        entity.balanceInputSlots();

        boolean isThermal = isThermalOverclocked(level, pos);
        float speedMult = entity.getUpgradeSpeedMultiplier();
        float energyMult = entity.getUpgradeEnergyMultiplier();
        float momentumMult = entity.coilTemp >= 1500 ? 1.25f : 1.0f;
        int speedStep = Math.max(1, Math.round((isThermal ? 1.5f : 1.0f) * speedMult * momentumMult));
        int rfPerChamber = Math.max(10, Math.round(RF_PER_CHAMBER_TICK * energyMult));

        boolean anySmelting = false;
        boolean dirty = false;

        ItemStack ashSlot = entity.inventory.getItem(6);

        for (int c = 0; c < 3; c++) {
            ItemStack in = entity.inventory.getItem(c);
            ItemStack out = entity.inventory.getItem(3 + c);
            ItemStack result = VolcaniteFurnaceBlockEntity.getSmeltingResult(level, in);

            boolean canSmelt = !result.isEmpty()
                    && (out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result)
                    && out.getCount() + result.getCount() + 1 <= out.getMaxStackSize()))
                    && (ashSlot.isEmpty() || (ashSlot.is(ObsidiumMod.VOLCANIC_ASH) && ashSlot.getCount() < ashSlot.getMaxStackSize()));

            boolean hasPower = isThermal || entity.energyStorage.amount >= rfPerChamber;
            if (canSmelt && hasPower) {
                if (!isThermal) {
                    entity.energyStorage.amount -= rfPerChamber;
                }
                entity.progress[c] += speedStep;
                anySmelting = true;
                dirty = true;

                if (entity.progress[c] >= MAX_PROGRESS) {
                    entity.progress[c] = 0;
                    boolean bonusYield = isOreOrDustForBonus(in) && level.random.nextFloat() < 0.35f;
                    in.shrink(1);

                    ItemStack produced = result.copy();
                    if (bonusYield) {
                        produced.grow(1);
                    }
                    if (out.isEmpty()) {
                        entity.inventory.setItem(3 + c, produced);
                    } else {
                        out.grow(produced.getCount());
                    }

                    // 50% chance for Volcanic Ash in shared Slot 6
                    if (level.random.nextFloat() < 0.50f) {
                        ashSlot = entity.inventory.getItem(6);
                        if (ashSlot.isEmpty()) {
                            entity.inventory.setItem(6, new ItemStack(ObsidiumMod.VOLCANIC_ASH));
                        } else if (ashSlot.is(ObsidiumMod.VOLCANIC_ASH) && ashSlot.getCount() < ashSlot.getMaxStackSize()) {
                            ashSlot.grow(1);
                        }
                    }

                    level.playSound(null, pos, ModSounds.FURNACE_ROAR, SoundSource.BLOCKS, 0.55F, 1.15F);
                    if (level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.04);
                    }
                }
            } else if (entity.progress[c] > 0) {
                entity.progress[c] = Math.max(0, entity.progress[c] - 2);
                dirty = true;
            }
        }

        if (anySmelting) {
            if (entity.coilTemp < 2500) {
                entity.coilTemp = Math.min(2500, entity.coilTemp + 10);
                dirty = true;
            }
            if (level.getGameTime() % 25 == 0) {
                float pitch = 1.0f + (speedMult - 1.0f) * 0.15f;
                level.playSound(null, pos, ModSounds.FURNACE_ROAR, SoundSource.BLOCKS, 0.45F, pitch);
            }
        } else if (entity.coilTemp > 300) {
            entity.coilTemp = Math.max(300, entity.coilTemp - 6);
            dirty = true;
        }

        boolean currentLitState = state.hasProperty(AdvancedVolcaniteFurnaceBlock.LIT) && state.getValue(AdvancedVolcaniteFurnaceBlock.LIT);
        if (currentLitState != anySmelting && state.hasProperty(AdvancedVolcaniteFurnaceBlock.LIT)) {
            level.setBlock(pos, state.setValue(AdvancedVolcaniteFurnaceBlock.LIT, anySmelting), 3);
            dirty = true;
        }

        if (dirty) {
            entity.setChanged();
        }

        if (entity.autoEject) {
            VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
        }
    }

    @Override public int getContainerSize() { return this.inventory.getContainerSize(); }
    @Override public boolean isEmpty() { return this.inventory.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return this.inventory.getItem(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack res = this.inventory.removeItem(slot, amount);
        if (!res.isEmpty()) setChanged();
        return res;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack res = this.inventory.removeItemNoUpdate(slot);
        if (!res.isEmpty()) setChanged();
        return res;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        this.inventory.setItem(slot, stack);
        setChanged();
    }
    @Override public boolean stillValid(Player player) { return this.inventory.stillValid(player); }
    @Override public void clearContent() { this.inventory.clearContent(); setChanged(); }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot <= 2) return true;
        if (slot == 7 || slot == 8) return isValidUpgrade(stack);
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{3, 4, 5, 6};
        if (side == Direction.UP) return new int[]{0, 1, 2};
        return new int[]{0, 1, 2, 3, 4, 5, 6};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= 0 && slot <= 2 && side != Direction.DOWN;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 3 && slot <= 6;
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= 0 && slot <= 2;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= 3 && slot <= 6;
    }

    @Override
    public Container getInventory() {
        return this;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putIntArray("Progress", progress);
        tag.putLong("Energy", energyStorage.amount);
        tag.putInt("CoilTemp", coilTemp);
        tag.putBoolean("AutoSplit", autoSplit);
        tag.putBoolean("AutoEject", autoEject);
        MachineInventory.saveInventory(tag, inventory, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        int[] savedProg = tag.getIntArray("Progress");
        for (int i = 0; i < Math.min(3, savedProg.length); i++) {
            this.progress[i] = savedProg[i];
        }
        this.energyStorage.amount = tag.getLong("Energy");
        this.coilTemp = tag.contains("CoilTemp") ? tag.getInt("CoilTemp") : 300;
        this.autoSplit = !tag.contains("AutoSplit") || tag.getBoolean("AutoSplit");
        this.autoEject = !tag.contains("AutoEject") || tag.getBoolean("AutoEject");
        MachineInventory.loadInventory(tag, inventory, provider);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.advanced_volcanite_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new AdvancedVolcaniteFurnaceMenu(syncId, playerInventory, this, this.data, this);
    }

    public static boolean isThermalOverclocked(Level level, BlockPos pos) {
        if (level == null) return false;
        for (Direction dir : Direction.values()) {
            BlockState adj = level.getBlockState(pos.relative(dir));
            if (adj.is(Blocks.LAVA)
                    || adj.is(Blocks.MAGMA_BLOCK)
                    || adj.getBlock() == ObsidiumMod.VOLCANITE_ENERGY_CORE_BLOCK
                    || adj.getFluidState().is(Fluids.LAVA)) {
                return true;
            }
        }
        return false;
    }
}
