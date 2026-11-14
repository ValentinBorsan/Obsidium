package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcaniteBottleItem;
import net.valentin.obsidium.menu.MagmaForgeMenu;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayList;
import java.util.List;

public class MagmaForgeCoreBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, MachineInventory {
    public static final int MAX_PROGRESS = 200; // 10 seconds passive, or ~2-3 hammer strikes!
    public static final long MAX_ENERGY = 10_000_000L;
    public static final long MAX_FLUID_MB = 64_000L; // 64 Buckets

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 250_000, 0) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    public final SingleFluidStorage fluidStorage = SingleFluidStorage.withFixedCapacity(FluidConstants.BUCKET * 64, this::setChanged);

    private boolean isFormed = false;
    private int craftTimer = 0;
    private int perfectStrikes = 0;
    private int heatLevel = 0;

    public boolean isFormed() {
        return isFormed;
    }

    // 6 Slots:
    // 0, 1, 2 = Inputs 1, 2, 3
    // 3 = Lava Bucket / Volcanite Bottle Fuel Slot
    // 4 = Main Output Slot
    // 5 = Bonus Output Slot (Perfect Forge Reward)
    public final SimpleContainer container = new SimpleContainer(6) {
        @Override
        public void setChanged() {
            super.setChanged();
            MagmaForgeCoreBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == 3) {
                return stack.is(Items.LAVA_BUCKET) || stack.getItem() instanceof VolcaniteBottleItem;
            }
            return slot == 0 || slot == 1 || slot == 2;
        }
    };

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == 3) {
            return stack.is(Items.LAVA_BUCKET) || stack.getItem() instanceof VolcaniteBottleItem;
        }
        return slot == 0 || slot == 1 || slot == 2;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 4 || slot == 5;
    }

    @Override
    public Container getInventory() {
        return this.container;
    }

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            long fluidMb = fluidStorage.amount / 81L;
            return switch (index) {
                case 0 -> (int) (energyStorage.amount & 0xFFFF);
                case 1 -> (int) ((energyStorage.amount >> 16) & 0xFFFF);
                case 2 -> (int) (fluidMb & 0xFFFF);
                case 3 -> (int) ((fluidMb >> 16) & 0xFFFF);
                case 4 -> craftTimer;
                case 5 -> MAX_PROGRESS;
                case 6 -> perfectStrikes;
                case 7 -> heatLevel;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public static final List<MagmaForgeRecipe> RECIPES = new ArrayList<>();

    static {
        // 0. Volcanic Liquid Barrels
        RECIPES.add(MagmaForgeRecipe.of(
            "Barrels",
            new ItemStack(ObsidiumMod.HEPHAESTUS_VOLCANITE_LOG, 4),
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 4),
            new ItemStack(Items.BARREL, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_WOOD_LIQUID_BARREL_ITEM, 1),
            ItemStack.EMPTY,
            5_000, 100
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Barrels",
            new ItemStack(ObsidiumMod.VOLCANIC_WOOD_LIQUID_BARREL_ITEM, 1),
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 4),
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 4),
            new ItemStack(ObsidiumMod.REINFORCED_VOLCANIC_LIQUID_BARREL_ITEM, 1),
            ItemStack.EMPTY,
            12_000, 250
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Barrels",
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 4),
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 4),
            new ItemStack(ObsidiumMod.VOLCANITE_FLUX_MECHANISM, 1),
            new ItemStack(ObsidiumMod.DIONYSIAN_BARREL_CORE, 1),
            ItemStack.EMPTY,
            18_000, 350
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Barrels",
            new ItemStack(ObsidiumMod.REINFORCED_VOLCANIC_LIQUID_BARREL_ITEM, 1),
            new ItemStack(ObsidiumMod.DIONYSIAN_BARREL_CORE, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_CASING_ITEM, 7),
            new ItemStack(ObsidiumMod.MEGA_VOLCANIC_BARREL_ITEM, 1),
            ItemStack.EMPTY,
            35_000, 700
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Biomass",
            new ItemStack(ObsidiumMod.VOLCANITE_GENERATOR_BLOCK, 1),
            new ItemStack(ObsidiumMod.HERMES_COPPER_FRUIT, 2),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 2),
            new ItemStack(ObsidiumMod.DEMETRIAN_BIOMASS_GENERATOR_ITEM, 1),
            ItemStack.EMPTY,
            40_000, 1_000
        ));

        // 1. Metallurgic Alloys (Early, Mid, Late)
        RECIPES.add(MagmaForgeRecipe.of(
            "Alloys",
            new ItemStack(Items.COPPER_INGOT, 2),
            new ItemStack(ObsidiumMod.TIN_INGOT, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_DUST, 1),
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 3),
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 1),
            5_000, 200
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Alloys",
            new ItemStack(Items.IRON_INGOT, 2),
            new ItemStack(ObsidiumMod.LEAD_INGOT, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_ASH, 1),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 2),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 1),
            5_000, 200
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Alloys",
            new ItemStack(Items.IRON_INGOT, 2),
            new ItemStack(ObsidiumMod.LEAD_INGOT, 1),
            new ItemStack(Items.COAL, 1),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 2),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 1),
            5_000, 200
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Alloys",
            new ItemStack(Items.IRON_INGOT, 2),
            new ItemStack(ObsidiumMod.LEAD_INGOT, 1),
            new ItemStack(Items.CHARCOAL, 1),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 2),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 1),
            5_000, 200
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Alloys",
            new ItemStack(ObsidiumMod.VOLCANITE_INGOT, 2),
            new ItemStack(Items.GOLD_INGOT, 2),
            new ItemStack(Items.OBSIDIAN, 2),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 4),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 2),
            15_000, 500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Alloys",
            new ItemStack(ObsidiumMod.VOLCANITE_INGOT, 2),
            new ItemStack(ObsidiumMod.SILVER_INGOT, 2),
            new ItemStack(Items.OBSIDIAN, 2),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 4),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 2),
            15_000, 500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Alloys",
            new ItemStack(ObsidiumMod.VOLCANITE_INGOT, 2),
            new ItemStack(Items.GOLD_INGOT, 1),
            new ItemStack(ObsidiumMod.SILVER_INGOT, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 5),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 2),
            15_000, 500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Alloys",
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 3),
            new ItemStack(Items.NETHER_STAR, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_THERMO_CORE, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 2),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            80_000, 2000
        ));

        // 2. Tier 1: Promethean Tools (Pyric Bronze)
        RECIPES.add(MagmaForgeRecipe.of(
            "Early Tools",
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 2),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.PROMETHEAN_SWORD, 1),
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 1),
            8_000, 250
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Early Tools",
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 3),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.PROMETHEAN_PICKAXE, 1),
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 1),
            8_000, 250
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Early Tools",
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 3),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.PROMETHEAN_AXE, 1),
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 1),
            8_000, 250
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Early Tools",
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.PROMETHEAN_SHOVEL, 1),
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 1),
            8_000, 250
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Early Tools",
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 2),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.PROMETHEAN_HOE, 1),
            new ItemStack(ObsidiumMod.PYRIC_BRONZE_INGOT, 1),
            8_000, 250
        ));

        // 3. Tier 1: Tartarean Tools & Armor (Feroplumb)
        RECIPES.add(MagmaForgeRecipe.of(
            "Early Tools",
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 2),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.TARTAREAN_SWORD, 1),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 1),
            8_000, 250
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Early Tools",
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 3),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.TARTAREAN_PICKAXE, 1),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 1),
            8_000, 250
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Early Tools",
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 3),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.TARTAREAN_AXE, 1),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 1),
            8_000, 250
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Early Tools",
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.TARTAREAN_SHOVEL, 1),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 1),
            8_000, 250
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Early Tools",
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 2),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.TARTAREAN_HOE, 1),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 1),
            8_000, 250
        ));

        RECIPES.add(MagmaForgeRecipe.of(
            "Early Armor",
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 4),
            new ItemStack(Items.IRON_HELMET, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.TARTAREAN_HELMET, 1),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 1),
            12_000, 300
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Early Armor",
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 6),
            new ItemStack(Items.IRON_CHESTPLATE, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.TARTAREAN_CHESTPLATE, 1),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 1),
            12_000, 300
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Early Armor",
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 5),
            new ItemStack(Items.IRON_LEGGINGS, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.TARTAREAN_LEGGINGS, 1),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 1),
            12_000, 300
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Early Armor",
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 4),
            new ItemStack(Items.IRON_BOOTS, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.TARTAREAN_BOOTS, 1),
            new ItemStack(ObsidiumMod.FEROPLUMB_INGOT, 1),
            12_000, 300
        ));

        // 4. Tier 2: Typhonic Tools & Armor (Volcanic Alloy)
        RECIPES.add(MagmaForgeRecipe.of(
            "Mid Tools",
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 2),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 1),
            new ItemStack(ObsidiumMod.TYPHONIC_SWORD, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 1),
            20_000, 500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Mid Tools",
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 3),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 1),
            new ItemStack(ObsidiumMod.TYPHONIC_PICKAXE, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 1),
            20_000, 500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Mid Tools",
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 3),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 1),
            new ItemStack(ObsidiumMod.TYPHONIC_AXE, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 1),
            20_000, 500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Mid Tools",
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 1),
            new ItemStack(ObsidiumMod.TYPHONIC_SHOVEL, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 1),
            20_000, 500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Mid Tools",
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 2),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 1),
            new ItemStack(ObsidiumMod.TYPHONIC_HOE, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 1),
            20_000, 500
        ));

        RECIPES.add(MagmaForgeRecipe.of(
            "Mid Armor",
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 4),
            new ItemStack(ObsidiumMod.TARTAREAN_HELMET, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.TYPHONIC_HELMET, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 1),
            25_000, 600
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Mid Armor",
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 6),
            new ItemStack(ObsidiumMod.TARTAREAN_CHESTPLATE, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.TYPHONIC_CHESTPLATE, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 1),
            25_000, 600
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Mid Armor",
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 5),
            new ItemStack(ObsidiumMod.TARTAREAN_LEGGINGS, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.TYPHONIC_LEGGINGS, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 1),
            25_000, 600
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Mid Armor",
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 4),
            new ItemStack(ObsidiumMod.TARTAREAN_BOOTS, 1),
            ItemStack.EMPTY,
            new ItemStack(ObsidiumMod.TYPHONIC_BOOTS, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 1),
            25_000, 600
        ));

        // 5. Tier 3: Hephaestean Tools & Armor (Supreme RF Powered)
        RECIPES.add(MagmaForgeRecipe.of(
            "Supreme Tools",
            new ItemStack(ObsidiumMod.TYPHONIC_SWORD, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_THERMO_CORE, 1),
            new ItemStack(ObsidiumMod.HEPHAESTEAN_SWORD, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            60_000, 1000
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Supreme Tools",
            new ItemStack(ObsidiumMod.TYPHONIC_PICKAXE, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_FLUX_MECHANISM, 1),
            new ItemStack(ObsidiumMod.HEPHAESTEAN_PICKAXE, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            60_000, 1000
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Supreme Tools",
            new ItemStack(ObsidiumMod.TYPHONIC_AXE, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_FLUX_MECHANISM, 1),
            new ItemStack(ObsidiumMod.HEPHAESTEAN_AXE, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            60_000, 1000
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Supreme Tools",
            new ItemStack(ObsidiumMod.TYPHONIC_SHOVEL, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_FLUX_MECHANISM, 1),
            new ItemStack(ObsidiumMod.HEPHAESTEAN_SHOVEL, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            60_000, 1000
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Supreme Tools",
            new ItemStack(ObsidiumMod.TYPHONIC_HOE, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_FLUX_MECHANISM, 1),
            new ItemStack(ObsidiumMod.HEPHAESTEAN_HOE, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            60_000, 1000
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Supreme Tools",
            new ItemStack(ObsidiumMod.HEPHAESTEAN_PICKAXE, 1),
            new ItemStack(ObsidiumMod.HEPHAESTEAN_AXE, 1),
            new ItemStack(ObsidiumMod.HEPHAESTEAN_SHOVEL, 1),
            new ItemStack(ObsidiumMod.HEPHAESTEAN_PAXEL, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            150_000, 3000
        ));

        RECIPES.add(MagmaForgeRecipe.of(
            "Supreme Armor",
            new ItemStack(ObsidiumMod.TYPHONIC_HELMET, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_THERMO_CORE, 1),
            new ItemStack(ObsidiumMod.HEPHAESTEAN_HELMET, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            75_000, 1500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Supreme Armor",
            new ItemStack(ObsidiumMod.TYPHONIC_CHESTPLATE, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 2),
            new ItemStack(ObsidiumMod.VOLCANITE_THERMO_CORE, 1),
            new ItemStack(ObsidiumMod.HEPHAESTEAN_CHESTPLATE, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            75_000, 1500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Supreme Armor",
            new ItemStack(ObsidiumMod.TYPHONIC_LEGGINGS, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 2),
            new ItemStack(ObsidiumMod.VOLCANITE_THERMO_CORE, 1),
            new ItemStack(ObsidiumMod.HEPHAESTEAN_LEGGINGS, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            75_000, 1500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Supreme Armor",
            new ItemStack(ObsidiumMod.TYPHONIC_BOOTS, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_THERMO_CORE, 1),
            new ItemStack(ObsidiumMod.HEPHAESTEAN_BOOTS, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            75_000, 1500
        ));

        // 6. Tech Components & High-Tier Gear
        RECIPES.add(MagmaForgeRecipe.of(
            "Tech",
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 2),
            new ItemStack(Items.BLAZE_ROD, 1),
            new ItemStack(Items.MAGMA_CREAM, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_THERMO_CORE, 3),
            new ItemStack(ObsidiumMod.VOLCANITE_THERMO_CORE, 1),
            20_000, 500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Tech",
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 2),
            new ItemStack(Items.REDSTONE, 2),
            new ItemStack(Items.GOLD_INGOT, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_FLUX_MECHANISM, 3),
            new ItemStack(ObsidiumMod.VOLCANITE_FLUX_MECHANISM, 1),
            20_000, 500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Tech",
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 2),
            new ItemStack(Items.REDSTONE, 2),
            new ItemStack(ObsidiumMod.SILVER_INGOT, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_FLUX_MECHANISM, 4),
            new ItemStack(ObsidiumMod.VOLCANITE_FLUX_MECHANISM, 1),
            20_000, 500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Biomass",
            new ItemStack(ObsidiumMod.MIDAS_GOLD_SAPLING_ITEM, 1),
            new ItemStack(ObsidiumMod.ARTEMIS_SILVER_SAPLING_ITEM, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_ASH, 2),
            new ItemStack(ObsidiumMod.ELECTRUM_SAPLING_ITEM, 1),
            ItemStack.EMPTY,
            25_000, 500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Tech",
            new ItemStack(Items.DIAMOND, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_INGOT, 1),
            new ItemStack(Items.REDSTONE, 2),
            new ItemStack(ObsidiumMod.VOLCANITE_STORAGE_PROCESSOR, 3),
            new ItemStack(ObsidiumMod.VOLCANITE_STORAGE_PROCESSOR, 1),
            25_000, 500
        ));

        // 5. High-Tier Gear & Upgrades
        RECIPES.add(MagmaForgeRecipe.of(
            "Gear",
            new ItemStack(ObsidiumMod.VOLCANITE_CHEST_ITEM, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 4),
            new ItemStack(ObsidiumMod.VOLCANITE_BINDING, 2),
            new ItemStack(ObsidiumMod.HEPHAESTEAN_ARMORY_ITEM, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_PLATE, 1),
            20_000, 500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Gear",
            new ItemStack(ObsidiumMod.BASIC_BACKPACK, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_PORTABLE_BATTERY, 1),
            new ItemStack(ObsidiumMod.ADVANCED_BACKPACK, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_THERMO_CORE, 1),
            75_000, 1500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Gear",
            new ItemStack(ObsidiumMod.VOLCANITE_PORTABLE_BATTERY, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_FLUX_MECHANISM, 1),
            new ItemStack(ObsidiumMod.ADVANCED_VOLCANITE_PORTABLE_BATTERY, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_FLUX_MECHANISM, 1),
            75_000, 1500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Gear",
            new ItemStack(ObsidiumMod.VOLCANITE_ADVANCED_PIPE_ENERGY_UPGRADE, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_FLUX_MECHANISM, 1),
            new ItemStack(ObsidiumMod.VOLCANITE_SUPREME_PIPE_ENERGY_UPGRADE, 2),
            new ItemStack(ObsidiumMod.VOLCANITE_SUPREME_PIPE_ENERGY_UPGRADE, 1),
            60_000, 1000
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Tech",
            new ItemStack(ObsidiumMod.VOLCANITE_GEOTHERMAL_GENERATOR_ITEM, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 2),
            new ItemStack(ObsidiumMod.VOLCANITE_ENERGY_CORE_ITEM, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_GENERATOR_ITEM, 1),
            new ItemStack(ObsidiumMod.SUPREME_VOLCANIC_ALLOY, 1),
            100_000, 2000
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Tech",
            new ItemStack(Items.NETHER_STAR, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_ASH, 2),
            new ItemStack(ObsidiumMod.VOLCANITE, 1),
            new ItemStack(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST, 2),
            new ItemStack(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST, 1),
            60_000, 1500
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Tech",
            new ItemStack(ObsidiumMod.VOLCANITE_GENERATOR_ITEM, 1),
            new ItemStack(ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK, 2),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 4),
            new ItemStack(ObsidiumMod.TARTARUS_PISTON_ENGINE_ITEM, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_ALLOY, 1),
            50_000, 1000
        ));
        RECIPES.add(MagmaForgeRecipe.of(
            "Tech",
            new ItemStack(ObsidiumMod.DEMETRIAN_BIOMASS_GENERATOR_ITEM, 1),
            new ItemStack(ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK, 2),
            new ItemStack(ObsidiumMod.VOLCANIC_ASH, 4),
            new ItemStack(ObsidiumMod.DIONYSIAN_BIO_CALDERA_ITEM, 1),
            new ItemStack(ObsidiumMod.VOLCANIC_ASH, 2),
            50_000, 1000
        ));
    }

    public MagmaForgeCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.MAGMA_FORGE_CORE_BE, pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.magma_forge_core");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MagmaForgeMenu(containerId, playerInventory, this.container, this.data, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsFormed", this.isFormed);
        tag.putInt("CraftTimer", this.craftTimer);
        tag.putInt("PerfectStrikes", this.perfectStrikes);
        tag.putInt("HeatLevel", this.heatLevel);

        NonNullList<ItemStack> items = NonNullList.withSize(this.container.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < this.container.getContainerSize(); i++) {
            items.set(i, this.container.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putLong("Energy", this.energyStorage.amount);

        CompoundTag fluidTag = new CompoundTag();
        this.fluidStorage.writeNbt(fluidTag, registries);
        tag.put("Fluid", fluidTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isFormed = tag.getBoolean("IsFormed");
        this.craftTimer = tag.getInt("CraftTimer");
        this.perfectStrikes = tag.getInt("PerfectStrikes");
        this.heatLevel = tag.getInt("HeatLevel");

        NonNullList<ItemStack> items = NonNullList.withSize(this.container.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        for (int i = 0; i < items.size(); i++) {
            this.container.setItem(i, items.get(i));
        }
        this.energyStorage.amount = tag.getLong("Energy");

        if (tag.contains("Fluid")) {
            this.fluidStorage.readNbt(tag.getCompound("Fluid"), registries);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MagmaForgeCoreBlockEntity entity) {
        if (level.getGameTime() % 10 == 0) {
            entity.checkMultiblock(level, pos);
        }

        if (entity.isFormed) {
            if (level.isClientSide) {
                if (level.random.nextFloat() < 0.35f) {
                    double ox = (level.random.nextDouble() - 0.5) * 1.8;
                    double oz = (level.random.nextDouble() - 0.5) * 1.8;
                    level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5 + ox * 0.4, pos.getY() + 1.05, pos.getZ() + 0.5 + oz * 0.4, 0, 0.05, 0);
                    level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5 + ox, pos.getY() + 1.02, pos.getZ() + 0.5 + oz, 0, 0.02, 0);
                }
            } else {
                entity.processLavaSlot();
                entity.processCrafting(level, pos);
                if (entity.heatLevel > 0 && level.getGameTime() % 15 == 0) {
                    entity.heatLevel = Math.max(0, entity.heatLevel - 1);
                }
                VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
            }
        }
    }

    private void processLavaSlot() {
        ItemStack fuelStack = this.container.getItem(3);
        if (fuelStack.isEmpty()) return;

        long spaceDroplets = this.fluidStorage.getCapacity() - this.fluidStorage.amount;
        if (spaceDroplets < FluidConstants.BUCKET) return;

        if (fuelStack.is(Items.LAVA_BUCKET)) {
            if (this.fluidStorage.isResourceBlank() || this.fluidStorage.variant.isOf(Fluids.LAVA)) {
                this.fluidStorage.variant = FluidVariant.of(Fluids.LAVA);
                this.fluidStorage.amount += FluidConstants.BUCKET;
                this.container.setItem(3, new ItemStack(Items.BUCKET));
                setChanged();
                if (level != null) {
                    level.playSound(null, worldPosition, net.valentin.obsidium.sound.ModSounds.GEOTHERMAL_BUBBLE, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
        } else if (fuelStack.getItem() instanceof VolcaniteBottleItem) {
            FluidVariant bottleFluid = VolcaniteBottleItem.getFluid(fuelStack);
            int bottleAmtMb = VolcaniteBottleItem.getFluidAmount(fuelStack);
            if (bottleFluid.isOf(Fluids.LAVA) && bottleAmtMb > 0) {
                long spaceMb = spaceDroplets / 81L;
                int toTransferMb = (int) Math.min(bottleAmtMb, Math.min(spaceMb, 1000));
                if (toTransferMb > 0) {
                    this.fluidStorage.variant = FluidVariant.of(Fluids.LAVA);
                    this.fluidStorage.amount += toTransferMb * 81L;
                    VolcaniteBottleItem.setFluid(fuelStack, bottleFluid, bottleAmtMb - toTransferMb);
                    setChanged();
                }
            }
        }
    }

    private static boolean isValidCornerBlock(Block b) {
        return b == ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK ||
               b == ObsidiumMod.VOLCANITE_ENERGY_CORE_BLOCK ||
               b == ObsidiumMod.VOLCANITE_ENERGY_CELL_BLOCK ||
               b == ObsidiumMod.MAGMA_FORGE_DUMMY_BLOCK;
    }

    private static boolean isValidEdgeBlock(Block b) {
        return b == ObsidiumMod.VOLCANITE_CASING_BLOCK ||
               b == ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK ||
               b == ObsidiumMod.MAGMA_FORGE_DUMMY_BLOCK;
    }

    public void checkMultiblock(Level level, BlockPos pos) {
        if (this.isFormed) return;

        boolean valid = true;
        // 3x3 Platform around the Magma Forge Core at (x in -1..1, y = 0, z in -1..1)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos p = pos.offset(x, 0, z);
                Block block = level.getBlockState(p).getBlock();
                if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    if (!isValidCornerBlock(block)) valid = false;
                } else {
                    if (!isValidEdgeBlock(block)) valid = false;
                }
            }
        }

        if (valid && !level.isClientSide) {
            form(level, pos);
        }
    }

    public void form(Level level, BlockPos pos) {
        this.isFormed = true;
        level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.MULTIBLOCK_FORM, SoundSource.BLOCKS, 1.0f, 1.0f);

        BlockState dummyState = ObsidiumMod.MAGMA_FORGE_DUMMY_BLOCK.defaultBlockState();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                level.setBlock(pos.offset(x, 0, z), dummyState, Block.UPDATE_ALL);
            }
        }

        BlockState newState = getBlockState().setValue(MagmaForgeCoreBlock.FORMED, true);
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 25, 1.0, 0.2, 1.0, 0.1);
            sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 40, 1.2, 0.3, 1.2, 0.05);
        }
        setChanged();
        level.sendBlockUpdated(pos, newState, newState, Block.UPDATE_ALL);
    }

    public void unform(Level level, BlockPos pos) {
        if (!this.isFormed) return;
        this.isFormed = false;
        level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.MULTIBLOCK_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);

        // Restore 3x3 layer at y = 0 (only replace MAGMA_FORGE_DUMMY_BLOCK so broken block isn't duplicated)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos p = pos.offset(x, 0, z);
                if (level.getBlockState(p).getBlock() == ObsidiumMod.MAGMA_FORGE_DUMMY_BLOCK) {
                    if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                        level.setBlock(p, ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    } else {
                        level.setBlock(p, ObsidiumMod.VOLCANITE_CASING_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }

        // Backward compatibility: if an older 3x3x2 save had dummy blocks at y = -1, restore them too
        BlockPos bottomCenter = pos.below();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos p = bottomCenter.offset(x, 0, z);
                if (level.getBlockState(p).getBlock() == ObsidiumMod.MAGMA_FORGE_DUMMY_BLOCK) {
                    if (x == 0 && z == 0) {
                        level.setBlock(p, ObsidiumMod.VOLCANITE_ENERGY_CORE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    } else if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                        level.setBlock(p, ObsidiumMod.VOLCANITE_ENERGY_UNIT_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    } else {
                        level.setBlock(p, ObsidiumMod.VOLCANITE_CASING_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }

        if (level.getBlockState(pos).getBlock() == ObsidiumMod.MAGMA_FORGE_CORE_BLOCK) {
            BlockState newState = getBlockState().setValue(MagmaForgeCoreBlock.FORMED, false);
            level.setBlock(pos, newState, Block.UPDATE_ALL);
            level.sendBlockUpdated(pos, newState, newState, Block.UPDATE_ALL);
        }
        setChanged();
    }

    @Nullable
    public MagmaForgeRecipe findMatchingRecipe() {
        ItemStack s0 = this.container.getItem(0);
        ItemStack s1 = this.container.getItem(1);
        ItemStack s2 = this.container.getItem(2);
        if (s0.isEmpty() && s1.isEmpty() && s2.isEmpty()) return null;

        for (MagmaForgeRecipe recipe : RECIPES) {
            if (recipe.matches(s0, s1, s2)) {
                return recipe;
            }
        }
        return null;
    }

    public boolean canCraft(MagmaForgeRecipe recipe) {
        if (recipe == null) return false;

        ItemStack currentOutput = this.container.getItem(4);
        if (!currentOutput.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(currentOutput, recipe.result)) return false;
            if (currentOutput.getCount() + recipe.result.getCount() > currentOutput.getMaxStackSize()) return false;
        }

        long reqEnergy = perfectStrikes >= 2 ? recipe.energyCost / 2 : recipe.energyCost;
        long reqLava = perfectStrikes >= 2 ? recipe.lavaCost / 2 : recipe.lavaCost;

        if (this.energyStorage.amount < reqEnergy) return false;
        if (this.fluidStorage.amount < reqLava) return false;
        if (this.fluidStorage.amount > 0 && !this.fluidStorage.getResource().isOf(Fluids.LAVA)) return false;

        return true;
    }

    /**
     * Interactive Hammer Strike triggered by the player in the Magma Forge UI!
     */
    public void handleHammerStrike(Player player, boolean perfect) {
        if (level == null || level.isClientSide()) return;

        MagmaForgeRecipe recipe = findMatchingRecipe();
        if (recipe == null || !canCraft(recipe)) {
            level.playSound(null, worldPosition, net.valentin.obsidium.sound.ModSounds.FORGE_FAIL, SoundSource.BLOCKS, 0.5f, 1.0f);
            return;
        }

        if (perfect) {
            this.craftTimer += 70; // +35% instant progress!
            this.perfectStrikes = Math.min(3, this.perfectStrikes + 1);
            this.heatLevel = Math.min(100, this.heatLevel + 30);

            level.playSound(null, worldPosition, net.valentin.obsidium.sound.ModSounds.FORGE_PERFECT_STRIKE, SoundSource.BLOCKS, 0.8f, 1.0f + (perfectStrikes * 0.08f));

            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.LAVA, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 12, 0.5, 0.2, 0.5, 0.1);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 1.3, worldPosition.getZ() + 0.5, 15, 0.4, 0.3, 0.4, 0.05);
            }
        } else {
            this.craftTimer += 25; // +12.5% progress
            this.heatLevel = Math.min(100, this.heatLevel + 10);
            level.playSound(null, worldPosition, net.valentin.obsidium.sound.ModSounds.FORGE_STRIKE, SoundSource.BLOCKS, 0.5f, 0.95f + level.random.nextFloat() * 0.1f);
        }

        if (this.craftTimer >= MAX_PROGRESS) {
            completeCraft(level, worldPosition, recipe);
        }
        setChanged();
    }

    /**
     * Pulls ingredients for the selected recipe from the player's inventory into the 3 input slots.
     */
    public void autoFillRecipe(Player player, int recipeIndex) {
        if (level == null || level.isClientSide() || recipeIndex < 0 || recipeIndex >= RECIPES.size()) return;
        MagmaForgeRecipe recipe = RECIPES.get(recipeIndex);

        Inventory inv = player.getInventory();

        // 1. Return any mismatched items from slots 0, 1, 2 to the player's inventory
        for (int i = 0; i < 3; i++) {
            ItemStack inSlot = this.container.getItem(i);
            if (!inSlot.isEmpty()) {
                ItemStack req = recipe.getIngredientStack(i);
                if (req.isEmpty() || !inSlot.is(req.getItem())) {
                    if (!inv.add(inSlot)) {
                        player.drop(inSlot, false);
                    }
                    this.container.setItem(i, ItemStack.EMPTY);
                }
            }
        }

        // 2. Pull matching items from player inventory into slots 0, 1, 2
        for (int i = 0; i < recipe.ingredients.size() && i < 3; i++) {
            ItemStack req = recipe.ingredients.get(i);
            ItemStack current = this.container.getItem(i);
            int needed = req.getCount();

            if (!current.isEmpty() && current.is(req.getItem())) {
                needed = Math.max(0, req.getCount() - current.getCount());
            }

            if (needed > 0) {
                for (int slot = 0; slot < inv.getContainerSize() && needed > 0; slot++) {
                    ItemStack pStack = inv.getItem(slot);
                    if (!pStack.isEmpty() && pStack.is(req.getItem())) {
                        int take = Math.min(needed, pStack.getCount());
                        if (current.isEmpty()) {
                            current = pStack.copyWithCount(take);
                            this.container.setItem(i, current);
                        } else {
                            current.grow(take);
                        }
                        pStack.shrink(take);
                        needed -= take;
                    }
                }
            }
        }

        this.craftTimer = 0;
        this.perfectStrikes = 0;
        level.playSound(null, worldPosition, net.valentin.obsidium.sound.ModSounds.MACHINE_CLICK, SoundSource.PLAYERS, 0.7f, 1.0f);
        setChanged();
    }

    private void processCrafting(Level level, BlockPos pos) {
        MagmaForgeRecipe currentRecipe = findMatchingRecipe();
        if (currentRecipe == null) {
            if (craftTimer > 0 || perfectStrikes > 0) {
                craftTimer = 0;
                perfectStrikes = 0;
                setChanged();
            }
            return;
        }

        if (!canCraft(currentRecipe)) {
            if (craftTimer > 0) {
                craftTimer = Math.max(0, craftTimer - 1);
                setChanged();
            }
            return;
        }

        craftTimer++;
        if (craftTimer >= MAX_PROGRESS) {
            completeCraft(level, pos, currentRecipe);
        } else if (level.getGameTime() % 5 == 0 && level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5, 3, 0.35, 0.15, 0.35, 0.03);
        }
    }

    private void completeCraft(Level level, BlockPos pos, MagmaForgeRecipe recipe) {
        boolean isMasterwork = this.perfectStrikes >= 2;
        long finalEnergyCost = isMasterwork ? recipe.energyCost / 2 : recipe.energyCost;
        long finalLavaCost = isMasterwork ? recipe.lavaCost / 2 : recipe.lavaCost;

        recipe.consumeInputs(this.container);

        try (Transaction t = Transaction.openOuter()) {
            this.energyStorage.extract(finalEnergyCost, t);
            this.fluidStorage.extract(FluidVariant.of(Fluids.LAVA), finalLavaCost, t);
            t.commit();
        }

        // Main Output (Slot 4)
        ItemStack currentOutput = this.container.getItem(4);
        if (currentOutput.isEmpty()) {
            this.container.setItem(4, recipe.result.copy());
        } else {
            currentOutput.grow(recipe.result.getCount());
        }

        // Masterwork Bonus Output (Slot 5) if player hit >= 2 Perfect Strikes!
        if (isMasterwork && !recipe.bonusResult.isEmpty()) {
            ItemStack currentBonus = this.container.getItem(5);
            if (currentBonus.isEmpty()) {
                this.container.setItem(5, recipe.bonusResult.copy());
            } else if (ItemStack.isSameItemSameComponents(currentBonus, recipe.bonusResult)
                       && currentBonus.getCount() + recipe.bonusResult.getCount() <= currentBonus.getMaxStackSize()) {
                currentBonus.grow(recipe.bonusResult.getCount());
            }
        }

        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.05);
            sl.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.5, 0.3, 0.5, 0.1);
            level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.FORGE_COMPLETE, SoundSource.BLOCKS, 1.0f, isMasterwork ? 1.1f : 1.0f);
            if (isMasterwork) {
                level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.FORGE_PERFECT_STRIKE, SoundSource.BLOCKS, 0.8f, 1.25f);
            }
        }

        this.craftTimer = 0;
        this.perfectStrikes = 0;
        setChanged();
    }

    public void debugStructure(Player player) {
        if (level == null || level.isClientSide()) return;

        // First try to form if valid!
        checkMultiblock(level, worldPosition);
        if (this.isFormed) {
            player.displayClientMessage(Component.literal("§6[Magma Forge] §a3x3 Multiblock Formed! Right-click again to open."), true);
            return;
        }

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos p = worldPosition.offset(x, 0, z);
                Block found = level.getBlockState(p).getBlock();
                if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                    if (!isValidCornerBlock(found)) {
                        player.displayClientMessage(Component.literal(
                            "§6[Magma Forge 3x3] §cMissing §eVolcanite Energy Unit §cat corner (" + p.toShortString() + "). Found: §f" + found.getName().getString()), true);
                        return;
                    }
                } else {
                    if (!isValidEdgeBlock(found)) {
                        player.displayClientMessage(Component.literal(
                            "§6[Magma Forge 3x3] §cMissing §eVolcanite Casing §cat side (" + p.toShortString() + "). Found: §f" + found.getName().getString()), true);
                        return;
                    }
                }
            }
        }
    }
}
