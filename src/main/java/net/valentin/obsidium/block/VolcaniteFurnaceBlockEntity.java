package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.ItemTags;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcanitePipeEnergyUpgradeItem;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.Optional;

public class VolcaniteFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, MachineInventory {
    // 0=Input, 1=Fuel, 2=OutputAlloy, 3=OutputAsh, 4=Upgrade1, 5=Upgrade2
    private NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);

    private int progress = 0;
    private final int MAX_PROGRESS = 100;
    private int burnTime = 0;
    private int totalBurnTime = 0;

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(50000, 5000, 0) {
        @Override
        protected void onFinalCommit() { setChanged(); }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> burnTime;
                case 2 -> totalBurnTime;
                case 3 -> (int) (energyStorage.amount >> 16);
                case 4 -> (int) (energyStorage.amount & 0xFFFF);
                case 5 -> isThermalOverclocked(level, worldPosition) ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> burnTime = value;
                case 2 -> totalBurnTime = value;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public VolcaniteFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.VOLCANITE_FURNACE_BE, pos, state);
    }

    public static boolean isFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(ObsidiumMod.VOLCANIC_ASH)) return true;
        return AbstractFurnaceBlockEntity.isFuel(stack);
    }

    public static int getBurnDuration(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(ObsidiumMod.VOLCANIC_ASH)) return 400;
        if (stack.is(Items.LAVA_BUCKET)) return 20000;
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
    }

    public int getUpgradeSpeedMultiplier() {
        int bonus = 1;
        for (int i = 4; i <= 5; i++) {
            ItemStack up = items.get(i);
            if (up.getItem() instanceof VolcanitePipeEnergyUpgradeItem pipeUp) {
                bonus += pipeUp.getTier();
            } else if (net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem.isOverclockUpgrade(up)) {
                bonus += Math.max(1, (int) net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem.getSpeedBonus(up));
            }
        }
        return bonus;
    }

    public static final int BASE_RF_PER_TICK = 80;

    public static void tick(Level level, BlockPos pos, BlockState state, VolcaniteFurnaceBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean changed = false;

        if (entity.burnTime > 0) {
            entity.burnTime--;
            changed = true;
        }

        ItemStack input = entity.items.get(0);
        ItemStack fuel = entity.items.get(1);
        ItemStack outputAlloy = entity.items.get(2);
        ItemStack outputAsh = entity.items.get(3);

        boolean isThermal = isThermalOverclocked(level, pos);
        ItemStack result = getSmeltingResult(level, input);

        boolean canCraft = !result.isEmpty() &&
                           (outputAlloy.isEmpty() || (outputAlloy.is(result.getItem()) && outputAlloy.getCount() + result.getCount() <= outputAlloy.getMaxStackSize())) &&
                           (outputAsh.isEmpty() || (outputAsh.is(ObsidiumMod.VOLCANIC_ASH) && outputAsh.getCount() < outputAsh.getMaxStackSize()));

        boolean poweredByRf = false;
        if (canCraft && entity.burnTime <= 0 && !isThermal) {
            if (entity.energyStorage.amount >= BASE_RF_PER_TICK) {
                entity.energyStorage.amount -= BASE_RF_PER_TICK;
                poweredByRf = true;
                changed = true;
            } else if (isFuel(fuel)) {
                int duration = getBurnDuration(fuel);
                if (duration > 0) {
                    entity.burnTime = entity.totalBurnTime = duration;
                    if (fuel.is(Items.LAVA_BUCKET)) {
                        entity.items.set(1, new ItemStack(Items.BUCKET));
                    } else {
                        fuel.shrink(1);
                    }
                    changed = true;
                }
            }
        }

        boolean isBurning = entity.burnTime > 0 || isThermal || poweredByRf;

        if (isBurning && canCraft) {
            int speed = entity.getUpgradeSpeedMultiplier() * (isThermal ? 2 : 1);
            entity.progress += speed;
            changed = true;

            if (entity.progress % 30 == 0) {
                level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.FURNACE_ROAR, net.minecraft.sounds.SoundSource.BLOCKS, 0.55F, 1.0F);
            }

            if (entity.progress >= entity.MAX_PROGRESS) {
                entity.progress = 0;
                input.shrink(1);

                // Produce Smelted Product
                if (outputAlloy.isEmpty()) {
                    entity.items.set(2, result.copy());
                } else {
                    outputAlloy.grow(result.getCount());
                }

                // Produce Ash Byproduct
                if (outputAsh.isEmpty()) {
                    entity.items.set(3, new ItemStack(ObsidiumMod.VOLCANIC_ASH));
                } else {
                    outputAsh.grow(1);
                }

                level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.FURNACE_ROAR, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.1F);
                changed = true;
            }
        } else {
            if (entity.progress > 0) {
                entity.progress = Math.max(0, entity.progress - 2);
                changed = true;
            }
        }

        boolean isLitNow = isBurning && canCraft;
        if (state.hasProperty(VolcaniteFurnaceBlock.LIT) && state.getValue(VolcaniteFurnaceBlock.LIT) != isLitNow) {
            level.setBlock(pos, state.setValue(VolcaniteFurnaceBlock.LIT, isLitNow), 3);
        }

        if (changed) {
            entity.setChanged();
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    public static boolean isThermalOverclocked(Level level, BlockPos pos) {
        if (level == null) return false;
        for (Direction dir : Direction.values()) {
            BlockState adj = level.getBlockState(pos.relative(dir));
            if (adj.is(net.minecraft.world.level.block.Blocks.LAVA) || 
                adj.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) || 
                adj.getBlock() == ObsidiumMod.VOLCANITE_ENERGY_CORE_BLOCK || 
                adj.getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA)) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack getSmeltingResult(Level level, ItemStack input) {
        if (input.isEmpty()) return ItemStack.EMPTY;

        if (input.is(ObsidiumMod.VOLCANITE_DUST)) return new ItemStack(ObsidiumMod.VOLCANIC_ALLOY);
        if (input.is(ObsidiumMod.IRON_DUST)) return new ItemStack(Items.IRON_INGOT);
        if (input.is(ObsidiumMod.GOLD_DUST)) return new ItemStack(Items.GOLD_INGOT);
        if (input.is(ObsidiumMod.COPPER_DUST)) return new ItemStack(Items.COPPER_INGOT);
        if (input.is(ObsidiumMod.TIN_DUST) || input.is(ObsidiumMod.RAW_TIN) || input.is(ObsidiumMod.TIN_ORE_ITEM) || input.is(ObsidiumMod.DEEPSLATE_TIN_ORE_ITEM)) return new ItemStack(ObsidiumMod.TIN_INGOT);
        if (input.is(ObsidiumMod.LEAD_DUST) || input.is(ObsidiumMod.RAW_LEAD) || input.is(ObsidiumMod.LEAD_ORE_ITEM) || input.is(ObsidiumMod.DEEPSLATE_LEAD_ORE_ITEM)) return new ItemStack(ObsidiumMod.LEAD_INGOT);
        if (input.is(ObsidiumMod.SILVER_DUST) || input.is(ObsidiumMod.RAW_SILVER) || input.is(ObsidiumMod.SILVER_ORE_ITEM) || input.is(ObsidiumMod.DEEPSLATE_SILVER_ORE_ITEM)) return new ItemStack(ObsidiumMod.SILVER_INGOT);
        if (input.is(ObsidiumMod.PYRIC_BRONZE_DUST)) return new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT);
        if (input.is(ObsidiumMod.FEROPLUMB_DUST)) return new ItemStack(ObsidiumMod.FEROPLUMB_INGOT);

        if (input.is(ItemTags.LOGS) || input.is(ItemTags.LOGS_THAT_BURN)) {
            return new ItemStack(Items.CHARCOAL);
        }

        if (level != null && !level.isClientSide()) {
            SingleRecipeInput recipeInput = new SingleRecipeInput(input);
            Optional<RecipeHolder<SmeltingRecipe>> match = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level);
            if (match.isPresent()) {
                return match.get().value().assemble(recipeInput, level.registryAccess());
            }
        }

        return getSmeltingResult(input);
    }

    public static ItemStack getSmeltingResult(ItemStack input) {
        if (input.isEmpty()) return ItemStack.EMPTY;
        if (input.is(ObsidiumMod.VOLCANITE_DUST)) return new ItemStack(ObsidiumMod.VOLCANIC_ALLOY);
        if (input.is(ObsidiumMod.IRON_DUST) || input.is(Items.RAW_IRON) || input.is(Items.IRON_ORE) || input.is(Items.DEEPSLATE_IRON_ORE)) return new ItemStack(Items.IRON_INGOT);
        if (input.is(ObsidiumMod.GOLD_DUST) || input.is(Items.RAW_GOLD) || input.is(Items.GOLD_ORE) || input.is(Items.DEEPSLATE_GOLD_ORE)) return new ItemStack(Items.GOLD_INGOT);
        if (input.is(ObsidiumMod.COPPER_DUST) || input.is(Items.RAW_COPPER) || input.is(Items.COPPER_ORE) || input.is(Items.DEEPSLATE_COPPER_ORE)) return new ItemStack(Items.COPPER_INGOT);
        if (input.is(ObsidiumMod.TIN_DUST) || input.is(ObsidiumMod.RAW_TIN) || input.is(ObsidiumMod.TIN_ORE_ITEM) || input.is(ObsidiumMod.DEEPSLATE_TIN_ORE_ITEM)) return new ItemStack(ObsidiumMod.TIN_INGOT);
        if (input.is(ObsidiumMod.LEAD_DUST) || input.is(ObsidiumMod.RAW_LEAD) || input.is(ObsidiumMod.LEAD_ORE_ITEM) || input.is(ObsidiumMod.DEEPSLATE_LEAD_ORE_ITEM)) return new ItemStack(ObsidiumMod.LEAD_INGOT);
        if (input.is(ObsidiumMod.SILVER_DUST) || input.is(ObsidiumMod.RAW_SILVER) || input.is(ObsidiumMod.SILVER_ORE_ITEM) || input.is(ObsidiumMod.DEEPSLATE_SILVER_ORE_ITEM)) return new ItemStack(ObsidiumMod.SILVER_INGOT);
        if (input.is(ObsidiumMod.PYRIC_BRONZE_DUST)) return new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT);
        if (input.is(ObsidiumMod.FEROPLUMB_DUST)) return new ItemStack(ObsidiumMod.FEROPLUMB_INGOT);
        if (input.is(ItemTags.LOGS) || input.is(ItemTags.LOGS_THAT_BURN)) return new ItemStack(Items.CHARCOAL);
        if (input.is(Items.COBBLESTONE) || input.is(Items.COBBLED_DEEPSLATE)) return new ItemStack(Items.STONE);
        if (input.is(Items.SAND)) return new ItemStack(Items.GLASS);
        if (input.is(Items.ANCIENT_DEBRIS)) return new ItemStack(Items.NETHERITE_SCRAP);
        if (input.is(Items.CLAY_BALL)) return new ItemStack(Items.BRICK);
        if (input.is(Items.NETHERRACK)) return new ItemStack(Items.NETHER_BRICK);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) return true;
        if (slot == 1) return isFuel(stack);
        if (slot == 4 || slot == 5) return stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem
                || net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem.isOverclockUpgrade(stack);
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{ 2, 3, 1 };
        } else if (side == Direction.UP) {
            return new int[]{ 0 };
        } else {
            return new int[]{ 1, 2, 3, 0 };
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == 0) return side != Direction.DOWN;
        if (slot == 1) return side != Direction.UP && isFuel(stack);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == 2 || slot == 3) return true;
        if (slot == 1 && stack.is(Items.BUCKET)) return true;
        return false;
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 || slot == 1;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 2 || slot == 3;
    }

    @Override
    protected Component getDefaultName() { return Component.translatable("block.obsidium.volcanite_furnace"); }

    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new VolcaniteFurnaceMenu(syncId, playerInventory, this, this.data);
    }

    @Override
    public int getContainerSize() { return items.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack res = ContainerHelper.removeItem(items, slot, amount);
        if (!res.isEmpty()) setChanged();
        return res;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack res = ContainerHelper.takeItem(items, slot);
        if (!res.isEmpty()) setChanged();
        return res;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() { items.clear(); setChanged(); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putLong("Energy", this.energyStorage.amount);
        tag.putInt("Progress", this.progress);
        tag.putInt("BurnTime", this.burnTime);
        tag.putInt("TotalBurnTime", this.totalBurnTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.energyStorage.amount = tag.getLong("Energy");
        this.progress = tag.getInt("Progress");
        this.burnTime = tag.getInt("BurnTime");
        this.totalBurnTime = tag.getInt("TotalBurnTime");
    }

    @Override
    protected NonNullList<ItemStack> getItems() { return this.items; }

    @Override
    protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
}
