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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

public class SupremeVolcanicGeneratorBlockEntity extends BlockEntity implements MenuProvider, InventoryHolder, MachineInventory {
    public static final int INVENTORY_SIZE = 3;
    // Slot 0: Pyroclastic Distillate Input (Pyroclastic Distillate Bucket / Pyroclastic Flask)
    // Slot 1: Empty Container Output (Bucket / Glass Bottle)
    // Slot 2: Astral Volcanic Catalyst (Nether Star + Volcanic Ash Catalyst)

    public static final int MAX_DISTILLATE = 32000;
    public static final int MAX_ENERGY = 10000000; // 10,000,000 RF buffer for massive short-duration bursts
    public static final int MAX_OUTPUT = 100000;   // 100,000 RF/t output transfer rate
    public static final int MAX_PRESSURE = 100;    // 0..100% tectonic pressure
    public static final int COOLDOWN_DURATION = 200; // 10 seconds dormant crust cooling after eruption
    public static final int DISTILLATE_PER_ERUPTION_START = 1000; // 1,000 mB Pyroclastic Distillate required to trigger an eruption
    public static final int DISTILLATE_DRAIN_DURING_ERUPTION = 5; // 5 mB/t drained during active eruption

    // Volcanic Phases
    public static final int PHASE_PRESSURIZING = 0;
    public static final int PHASE_ERUPTING = 1;
    public static final int PHASE_COOLING = 2;

    private int distillateAmount = 0;
    private int pressure = 0;         // 0..100
    private int phase = PHASE_PRESSURIZING;
    private int eruptionTicks = 0;
    private int maxEruptionTicks = 240;
    private int cooldownTicks = 0;
    private int currentBurstRate = 0; // Active RF/t during eruption burst

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            SupremeVolcanicGeneratorBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            if (slot == 0) {
                return isDistillateContainer(stack);
            }
            if (slot == 2) {
                return getCatalystBurstRate(stack) > 0;
            }
            return false;
        }
    };

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(MAX_ENERGY, 0, MAX_OUTPUT) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> distillateAmount;
                case 1 -> pressure;
                case 2 -> phase;
                case 3 -> eruptionTicks;
                case 4 -> maxEruptionTicks;
                case 5 -> cooldownTicks;
                case 6 -> currentBurstRate;
                case 7 -> (int) (energyStorage.amount & 0xFFFF);
                case 8 -> (int) ((energyStorage.amount >> 16) & 0xFFFF);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> distillateAmount = value;
                case 1 -> pressure = value;
                case 2 -> phase = value;
                case 3 -> eruptionTicks = value;
                case 4 -> maxEruptionTicks = value;
                case 5 -> cooldownTicks = value;
                case 6 -> currentBurstRate = value;
                case 7 -> energyStorage.amount = (energyStorage.amount & 0xFFFF0000L) | (value & 0xFFFFL);
                case 8 -> energyStorage.amount = (energyStorage.amount & 0xFFFFL) | ((long) (value & 0xFFFF) << 16);
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public SupremeVolcanicGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.SUPREME_VOLCANIC_GENERATOR_BE, pos, state);
    }

    public static boolean isDistillateContainer(ItemStack stack) {
        if (stack.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET) || stack.is(ObsidiumMod.PYROCLASTIC_FLASK)) return true;
        if (stack.getItem() instanceof net.valentin.obsidium.item.VolcaniteBottleItem) {
            return net.valentin.obsidium.item.VolcaniteBottleItem.getFluid(stack).isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID)
                    && net.valentin.obsidium.item.VolcaniteBottleItem.getFluidAmount(stack) > 0;
        }
        return false;
    }

    public static int getCatalystBurstRate(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST)) return 35000; // 35,000 RF/t Astral Cataclysmic Eruption!
        if (stack.is(ObsidiumMod.SUPREME_VOLCANIC_ALLOY)) return 20000;   // 20,000 RF/t Supreme Alloy Eruption!
        return 0;
    }

    public static int getCatalystDurationTicks(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(ObsidiumMod.ASTRAL_VOLCANIC_CATALYST)) return 240; // 12 seconds burst (8.4M RF total)
        if (stack.is(ObsidiumMod.SUPREME_VOLCANIC_ALLOY)) return 200;   // 10 seconds burst (4.0M RF total)
        return 200;
    }

    public int insertDistillate(int amount) {
        int toInsert = Math.min(amount, MAX_DISTILLATE - distillateAmount);
        if (toInsert > 0) {
            distillateAmount += toInsert;
            setChanged();
        }
        return toInsert;
    }

    public int getDistillateAmount() {
        return distillateAmount;
    }

    public int getMaxDistillate() {
        return MAX_DISTILLATE;
    }

    /**
     * Triggered manually via the [ERUPT!] GUI button (when pressure >= 50%) or automatically at 100% pressure.
     */
    public boolean triggerManualEruption() {
        if (this.phase != PHASE_PRESSURIZING || this.pressure < 50 || this.distillateAmount < DISTILLATE_PER_ERUPTION_START) {
            return false;
        }
        ItemStack catalyst = this.inventory.getItem(2);
        int baseRate = getCatalystBurstRate(catalyst);
        if (baseRate <= 0) {
            return false;
        }
        float pressureMult = this.pressure / 100.0f;
        startEruption(catalyst, Math.max(1000, Math.round(baseRate * pressureMult)), getCatalystDurationTicks(catalyst));
        return true;
    }

    private void startEruption(ItemStack catalyst, int burstRate, int durationTicks) {
        catalyst.shrink(1);
        this.distillateAmount = Math.max(0, this.distillateAmount - DISTILLATE_PER_ERUPTION_START);

        this.phase = PHASE_ERUPTING;
        this.pressure = 0;
        this.currentBurstRate = burstRate;
        this.maxEruptionTicks = durationTicks;
        this.eruptionTicks = durationTicks;
        setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.playSound(null, this.worldPosition, net.valentin.obsidium.sound.ModSounds.SUPREME_ERUPTION, SoundSource.BLOCKS, 0.7F, 1.0F);
            if (this.level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LAVA,
                        this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.1, this.worldPosition.getZ() + 0.5,
                        18, 0.35, 0.4, 0.35, 0.15);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.1, this.worldPosition.getZ() + 0.5,
                        24, 0.3, 0.45, 0.3, 0.08);
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SupremeVolcanicGeneratorBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean dirty = false;

        // 1. Drain Pyroclastic Distillate Bucket or Flask from Slot 0 into internal Distillate Chamber
        ItemStack input = entity.inventory.getItem(0);
        ItemStack output = entity.inventory.getItem(1);
        if (input.is(ObsidiumMod.PYROCLASTIC_DISTILLATE_BUCKET) && entity.distillateAmount + 1000 <= MAX_DISTILLATE) {
            if (output.isEmpty() || (output.is(Items.BUCKET) && output.getCount() < output.getMaxStackSize())) {
                entity.distillateAmount += 1000;
                input.shrink(1);
                if (output.isEmpty()) {
                    entity.inventory.setItem(1, new ItemStack(Items.BUCKET));
                } else {
                    output.grow(1);
                }
                dirty = true;
            }
        } else if (input.is(ObsidiumMod.PYROCLASTIC_FLASK) && entity.distillateAmount + 250 <= MAX_DISTILLATE) {
            if (output.isEmpty() || (output.is(Items.GLASS_BOTTLE) && output.getCount() < output.getMaxStackSize())) {
                entity.distillateAmount += 250;
                input.shrink(1);
                if (output.isEmpty()) {
                    entity.inventory.setItem(1, new ItemStack(Items.GLASS_BOTTLE));
                } else {
                    output.grow(1);
                }
                dirty = true;
            }
        } else if (input.getItem() instanceof net.valentin.obsidium.item.VolcaniteBottleItem) {
            var fv = net.valentin.obsidium.item.VolcaniteBottleItem.getFluid(input);
            int amt = net.valentin.obsidium.item.VolcaniteBottleItem.getFluidAmount(input);
            if (fv.isOf(ObsidiumMod.PYROCLASTIC_DISTILLATE_FLUID) && amt > 0 && entity.distillateAmount < MAX_DISTILLATE) {
                int toTransfer = Math.min(amt, Math.min(1000, MAX_DISTILLATE - entity.distillateAmount));
                if (toTransfer > 0) {
                    entity.distillateAmount += toTransfer;
                    net.valentin.obsidium.item.VolcaniteBottleItem.setFluid(input, fv, amt - toTransfer);
                    dirty = true;
                }
            }
        }

        // 2. Volcanic 3-Phase State Machine
        if (entity.phase == PHASE_PRESSURIZING) {
            ItemStack catalyst = entity.inventory.getItem(2);
            int burstRate = getCatalystBurstRate(catalyst);
            boolean canBuildPressure = entity.distillateAmount >= DISTILLATE_PER_ERUPTION_START
                    && burstRate > 0
                    && entity.energyStorage.amount < entity.energyStorage.capacity;

            if (canBuildPressure) {
                if (entity.pressure < MAX_PRESSURE) {
                    entity.pressure++;
                    dirty = true;
                }
                if (entity.pressure >= MAX_PRESSURE) {
                    entity.startEruption(catalyst, burstRate, getCatalystDurationTicks(catalyst));
                    dirty = true;
                }
            } else if (entity.pressure > 0 && entity.distillateAmount < DISTILLATE_PER_ERUPTION_START) {
                entity.pressure = Math.max(0, entity.pressure - 1);
                dirty = true;
            }
        } else if (entity.phase == PHASE_ERUPTING) {
            if (entity.eruptionTicks > 0) {
                entity.eruptionTicks--;

                if (entity.distillateAmount >= DISTILLATE_DRAIN_DURING_ERUPTION) {
                    entity.distillateAmount -= DISTILLATE_DRAIN_DURING_ERUPTION;
                }

                // Generate massive burst of volcanic energy!
                if (entity.energyStorage.amount < entity.energyStorage.capacity) {
                    entity.energyStorage.amount = Math.min(entity.energyStorage.capacity, entity.energyStorage.amount + entity.currentBurstRate);
                }

                // Server-side volcanic plume particles & rumble every few ticks
                if (level.getGameTime() % 5 == 0 && level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.LAVA,
                            pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                            3, 0.2, 0.25, 0.2, 0.05);
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                            pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                            2, 0.15, 0.2, 0.15, 0.02);
                }

                if (entity.eruptionTicks <= 0) {
                    entity.phase = PHASE_COOLING;
                    entity.cooldownTicks = COOLDOWN_DURATION;
                    entity.currentBurstRate = 0;
                    level.playSound(null, pos, net.valentin.obsidium.sound.ModSounds.SUPREME_VENT, SoundSource.BLOCKS, 0.8F, 1.0F);
                }
                dirty = true;
            }
        } else if (entity.phase == PHASE_COOLING) {
            if (entity.cooldownTicks > 0) {
                entity.cooldownTicks--;
                if (entity.cooldownTicks <= 0) {
                    entity.phase = PHASE_PRESSURIZING;
                    entity.pressure = 0;
                }
                dirty = true;
            } else {
                entity.phase = PHASE_PRESSURIZING;
                dirty = true;
            }
        }

        // 3. Sync WORKING blockstate
        boolean isErupting = (entity.phase == PHASE_ERUPTING && entity.eruptionTicks > 0);
        if (state.getValue(SupremeVolcanicGeneratorBlock.WORKING) != isErupting) {
            level.setBlock(pos, state.setValue(SupremeVolcanicGeneratorBlock.WORKING, isErupting), 3);
            dirty = true;
        }

        // 4. Push massive burst energy to adjacent cables, batteries & machines
        if (entity.energyStorage.amount > 0) {
            for (Direction dir : Direction.values()) {
                EnergyStorage target = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
                if (target != null) {
                    long moved = EnergyStorageUtil.move(entity.energyStorage, target, MAX_OUTPUT, null);
                    if (moved > 0) {
                        dirty = true;
                    }
                }
            }
        }

        if (dirty) {
            entity.setChanged();
        }

        VolcaniteStoragePipeBlockEntity.autoEjectMachineOutputs(level, pos, entity);
    }

    @Override
    public Container getInventory() {
        return this.inventory;
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == 0) return isDistillateContainer(stack);
        if (slot == 2) return getCatalystBurstRate(stack) > 0;
        return false;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Distillate", distillateAmount);
        tag.putInt("Pressure", pressure);
        tag.putInt("Phase", phase);
        tag.putInt("EruptionTicks", eruptionTicks);
        tag.putInt("MaxEruptionTicks", maxEruptionTicks);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putInt("CurrentBurstRate", currentBurstRate);
        tag.putLong("Energy", energyStorage.amount);
        MachineInventory.saveInventory(tag, inventory, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.distillateAmount = tag.contains("Distillate") ? tag.getInt("Distillate") : tag.getInt("Lava");
        this.pressure = tag.getInt("Pressure");
        this.phase = tag.getInt("Phase");
        this.eruptionTicks = tag.getInt("EruptionTicks");
        this.maxEruptionTicks = Math.max(1, tag.getInt("MaxEruptionTicks"));
        this.cooldownTicks = tag.getInt("CooldownTicks");
        this.currentBurstRate = tag.getInt("CurrentBurstRate");
        this.energyStorage.amount = tag.getLong("Energy");
        MachineInventory.loadInventory(tag, this.inventory, provider);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.obsidium.supreme_volcanic_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new SupremeVolcanicGeneratorMenu(syncId, playerInventory, this.inventory, this.data, this);
    }
}
