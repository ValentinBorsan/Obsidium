package net.valentin.obsidium.block;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.base.SimpleEnergyItem;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;

public class ThanatosExecutionerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, MachineInventory {

    public static final int INVENTORY_SIZE = 10;
    // Slots 0..8: Output Loot Buffer (9 slots)
    // Slot 9: Weapon / Sword Upgrade Slot (applies sword damage + Looting enchantments)
    public static final int WEAPON_SLOT = 9;

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            ThanatosExecutionerBlockEntity.this.setChanged();
        }
    };

    public final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(200_000L, 15_000L, 0L) {
        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    private int attackCooldown = 0;
    private int storedXp = 0;
    private int targetMode = 0; // 0 = Hostiles Only, 1 = All Mobs (except pets)
    private int killCount = 0;
    private int spawnerEnabled = 1; // 1 = Spawner Active (Lure mode), 0 = Disabled
    private int spawnTimer = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) (energyStorage.amount & 0xFFFF);
                case 1 -> (int) ((energyStorage.amount >> 16) & 0xFFFF);
                case 2 -> storedXp;
                case 3 -> targetMode;
                case 4 -> killCount;
                case 5 -> spawnerEnabled;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 3) targetMode = value;
            else if (index == 2) storedXp = value;
            else if (index == 5) spawnerEnabled = value;
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public ThanatosExecutionerBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.THANATOS_EXECUTIONER_BE, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ThanatosExecutionerBlockEntity entity) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        boolean wasWorking = state.getValue(ThanatosExecutionerBlock.WORKING);
        boolean isWorking = false;

        Direction facing = state.getValue(ThanatosExecutionerBlock.FACING);
        Direction right = facing.getClockWise();

        // 5x5 Chamber bounding box facing forward (5 wide, 5 deep, 4 high)
        BlockPos corner1 = pos.relative(facing, 1).relative(right, -2);
        BlockPos corner2 = pos.relative(facing, 5).relative(right, 2).above(3);
        AABB chamberBox = AABB.encapsulatingFullBlocks(corner1, corner2);

        // 1. Vacuum Loot & XP Orbs from Chamber (Optimized: runs every 6 ticks or immediately after attack)
        if (serverLevel.getGameTime() % 6 == 0) {
            entity.vacuumDrops(serverLevel, chamberBox);
            entity.autoOutputLoot(serverLevel, pos);
        }

        // 3. Automated Mob Lure / Spawner (Normal rate: every 100 ticks = 5 seconds)
        if (entity.spawnerEnabled == 1 && entity.energyStorage.amount >= 4000) {
            if (++entity.spawnTimer >= 100) {
                entity.spawnTimer = 0;
                entity.trySpawnMobInChamber(serverLevel, pos, facing, right, chamberBox);
            }
        }

        // 4. Mob Attack Cycle (every 12 ticks)
        if (++entity.attackCooldown >= 12) {
            entity.attackCooldown = 0;

            if (entity.energyStorage.amount >= 1500) {
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, chamberBox, e -> {
                    if (!e.isAlive() || e instanceof Player) return false;
                    if (entity.targetMode == 0) {
                        return e instanceof Enemy || e instanceof Mob mob && mob.isAggressive();
                    } else {
                        return !(e instanceof TamableAnimal tamed && tamed.isTame());
                    }
                });

                if (!targets.isEmpty()) {
                    isWorking = true;
                    // Execute up to 4 targets per sweep
                    int attacks = 0;
                    for (LivingEntity target : targets) {
                        if (attacks >= 4 || entity.energyStorage.amount < 1500) break;

                        // Deal player-type strike
                        entity.executeMob(serverLevel, target);
                        entity.energyStorage.amount = Math.max(0, entity.energyStorage.amount - 1500);
                        attacks++;
                        entity.killCount++;
                    }

                    // Sweep particles across the chamber entrance
                    double px = pos.getX() + 0.5 + facing.getStepX() * 1.2;
                    double py = pos.getY() + 1.2;
                    double pz = pos.getZ() + 0.5 + facing.getStepZ() * 1.2;
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
                    serverLevel.playSound(null, pos, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.BLOCKS, 0.4F, 1.2F);
                }
            }
        }

        if (wasWorking != isWorking) {
            level.setBlock(pos, state.setValue(ThanatosExecutionerBlock.WORKING, isWorking), 3);
            entity.setChanged();
        }
    }

    private void executeMob(ServerLevel level, LivingEntity target) {
        float damage = 35.0F; // Base instakill damage
        ItemStack weapon = inventory.getItem(WEAPON_SLOT);
        if (!weapon.isEmpty() && weapon.getItem() instanceof SwordItem sword) {
            damage = Math.max(damage, 30.0F + sword.getTier().getAttackDamageBonus());
        }

        Player player = level.getRandomPlayer();
        DamageSource source = (player != null) ? level.damageSources().playerAttack(player) : level.damageSources().generic();
        target.hurt(source, damage);

        if (!weapon.isEmpty()) {
            applyWeaponExecutionCost(level, weapon);
        }
    }

    private void applyWeaponExecutionCost(ServerLevel level, ItemStack weapon) {
        // 1. RF Energy-based weapon (e.g. Hephaestean Sword)
        if (weapon.getItem() instanceof SimpleEnergyItem energyItem) {
            long stored = energyItem.getStoredEnergy(weapon);
            if (stored >= 150) {
                energyItem.tryUseEnergy(weapon, 150);
            }
            setChanged();
            return;
        }

        // 2. XP-based / Mending: if weapon has Mending and machine has soul XP
        boolean hasMending = false;
        ItemEnchantments enchants = weapon.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : enchants.keySet()) {
            if (holder.getRegisteredName().toLowerCase().contains("mending") ||
                holder.unwrapKey().map(k -> k.location().getPath().toLowerCase().contains("mending")).orElse(false)) {
                hasMending = true;
                break;
            }
        }

        if (hasMending && this.storedXp > 0) {
            this.storedXp = Math.max(0, this.storedXp - 1);
            if (weapon.isDamageableItem() && weapon.getDamageValue() > 0 && this.storedXp > 0) {
                this.storedXp = Math.max(0, this.storedXp - 1);
                weapon.setDamageValue(Math.max(0, weapon.getDamageValue() - 1));
            }
            setChanged();
            return;
        }

        // 3. Durability wear
        if (weapon.isDamageableItem()) {
            int currentDmg = weapon.getDamageValue();
            int maxDmg = weapon.getMaxDamage();
            if (currentDmg + 1 >= maxDmg) {
                inventory.setItem(WEAPON_SLOT, ItemStack.EMPTY);
                level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.9F, 0.9F);
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, weapon),
                        worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                        15, 0.15, 0.15, 0.15, 0.05);
            } else {
                weapon.setDamageValue(currentDmg + 1);
            }
            setChanged();
        }
    }

    private void vacuumDrops(ServerLevel level, AABB chamberBox) {
        // Collect Item Drops
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, chamberBox);
        for (ItemEntity itemEntity : items) {
            if (!itemEntity.isAlive()) continue;
            ItemStack stack = itemEntity.getItem();
            ItemStack remainder = insertLoot(stack);
            if (remainder.isEmpty()) {
                itemEntity.discard();
            } else if (remainder.getCount() < stack.getCount()) {
                itemEntity.setItem(remainder);
            }
        }

        // Collect Experience Orbs (Batched sound & setChanged)
        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, chamberBox);
        if (!orbs.isEmpty()) {
            boolean collected = false;
            for (ExperienceOrb orb : orbs) {
                if (!orb.isAlive()) continue;
                this.storedXp += orb.getValue();
                orb.discard();
                collected = true;
            }
            if (collected) {
                level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.15F, 1.6F);
                setChanged();
            }
        }
    }

    private ItemStack insertLoot(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack rem = stack.copy();

        // Pass 1: Existing matching stacks in slots 0..8
        for (int i = 0; i < 9; i++) {
            ItemStack inSlot = inventory.getItem(i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, rem)) {
                int space = inSlot.getMaxStackSize() - inSlot.getCount();
                int toAdd = Math.min(space, rem.getCount());
                if (toAdd > 0) {
                    inSlot.grow(toAdd);
                    rem.shrink(toAdd);
                    if (rem.isEmpty()) {
                        inventory.setChanged();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // Pass 2: Empty slots in slots 0..8
        for (int i = 0; i < 9; i++) {
            ItemStack inSlot = inventory.getItem(i);
            if (inSlot.isEmpty()) {
                inventory.setItem(i, rem.copy());
                inventory.setChanged();
                return ItemStack.EMPTY;
            }
        }

        inventory.setChanged();
        return rem;
    }

    /**
     * Automatic output: sweeps all 6 adjacent sides and pushes loot directly into chests,
     * barrels, modular chests, volcanite units, and storage pipes!
     */
    private void autoOutputLoot(ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe == null || neighborBe instanceof ThanatosExecutionerBlockEntity) continue;

            // 1. Direct Fabric ItemStorage Transfer
            Storage<ItemVariant> targetStorage = ItemStorage.SIDED.find(level, neighborPos, dir.getOpposite());
            if (targetStorage != null) {
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.isEmpty()) continue;

                    try (Transaction tx = Transaction.openOuter()) {
                        long inserted = targetStorage.insert(ItemVariant.of(stack), stack.getCount(), tx);
                        if (inserted > 0) {
                            tx.commit();
                            stack.shrink((int) inserted);
                            if (stack.isEmpty()) inventory.setItem(i, ItemStack.EMPTY);
                            inventory.setChanged();
                        }
                    }
                }
                continue;
            }

            // 2. Direct Vanilla / Mod Container Transfer
            if (neighborBe instanceof Container container) {
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.isEmpty()) continue;

                    int accepted = VolcaniteStoragePipeBlockEntity.insertIntoContainer(container, dir.getOpposite(), stack, false);
                    if (accepted > 0) {
                        stack.shrink(accepted);
                        if (stack.isEmpty()) inventory.setItem(i, ItemStack.EMPTY);
                        inventory.setChanged();
                    }
                }
            }
        }
    }

    public void cycleTargetMode() {
        this.targetMode = (this.targetMode == 0) ? 1 : 0;
        setChanged();
    }

    public void cycleSpawner() {
        this.spawnerEnabled = (this.spawnerEnabled == 1) ? 0 : 1;
        setChanged();
    }

    public int getSpawnerEnabled() {
        return spawnerEnabled;
    }

    private void trySpawnMobInChamber(ServerLevel level, BlockPos pos, Direction facing, Direction right, AABB chamberBox) {
        // Limit max mobs in chamber to prevent entity overflow
        List<Mob> currentMobs = level.getEntitiesOfClass(Mob.class, chamberBox);
        if (currentMobs.size() >= 6) return;

        // Try up to 10 random positions inside chamber
        for (int attempts = 0; attempts < 10; attempts++) {
            int dx = level.random.nextInt(5) - 2;
            int dz = level.random.nextInt(5) + 1;
            BlockPos colPos = pos.relative(facing, dz).relative(right, dx);

            for (int dy = -1; dy <= 2; dy++) {
                BlockPos floor = colPos.above(dy);
                BlockPos spawnPos = floor.above();
                BlockPos headPos = spawnPos.above();

                if (!level.getBlockState(floor).isAir() &&
                    level.getBlockState(spawnPos).isAir() &&
                    level.getBlockState(headPos).isAir()) {

                    // Check darkness (block light <= 9, or fire/Nether mobs which bypass lighting)
                    int light = level.getBrightness(LightLayer.BLOCK, spawnPos);
                    EntityType<? extends Mob> mobType = selectMobType(level);
                    boolean isNetherMob = (mobType == ObsidiumMod.CINDER_STALKER || mobType == ObsidiumMod.TARTAREAN_HOUND || mobType == EntityType.BLAZE || mobType == EntityType.MAGMA_CUBE || mobType == EntityType.WITHER_SKELETON);

                    if (light <= 9 || isNetherMob) {
                        Mob mob = mobType.create(level);
                        if (mob != null) {
                            mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
                            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.SPAWNER, null);
                            level.addFreshEntity(mob);

                            this.energyStorage.amount = Math.max(0, this.energyStorage.amount - 4000);
                            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, 8, 0.25, 0.25, 0.25, 0.02);
                            level.playSound(null, spawnPos, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 0.4F, 1.2F);
                            setChanged();
                            return;
                        }
                    }
                }
            }
        }
    }

    private EntityType<? extends Mob> selectMobType(ServerLevel level) {
        if (level.dimension() == Level.NETHER) {
            int r = level.random.nextInt(100);
            if (r < 18) return ObsidiumMod.CINDER_STALKER; // Cinder Stalker custom mob
            if (r < 32) return ObsidiumMod.TARTAREAN_HOUND; // Tartarean Hound custom mob
            if (r < 50) return EntityType.WITHER_SKELETON;
            if (r < 68) return EntityType.BLAZE;
            if (r < 82) return EntityType.PIGLIN;
            if (r < 92) return EntityType.MAGMA_CUBE;
            return EntityType.ZOMBIFIED_PIGLIN;
        } else if (level.dimension() == Level.END) {
            int r = level.random.nextInt(100);
            return (r < 15) ? EntityType.ENDERMITE : EntityType.ENDERMAN;
        } else {
            // Overworld defaults (Includes custom Cinder Stalker, Tartarean Hound, Zombie, etc.)
            int r = level.random.nextInt(100);
            if (r < 14) return ObsidiumMod.CINDER_STALKER;
            if (r < 24) return ObsidiumMod.TARTAREAN_HOUND;
            if (r < 42) return EntityType.ZOMBIE;
            if (r < 56) return EntityType.SKELETON;
            if (r < 68) return EntityType.SPIDER;
            if (r < 78) return EntityType.CAVE_SPIDER;
            if (r < 88) return EntityType.CREEPER;
            if (r < 95) return EntityType.WITCH;
            return EntityType.ENDERMAN;
        }
    }

    public int getStoredXp() {
        return storedXp;
    }

    public void extractXp(Player player) {
        if (storedXp > 0) {
            player.giveExperiencePoints(storedXp);
            if (level != null) {
                level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.0F);
            }
            storedXp = 0;
            setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Thanatos Executioner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new ThanatosExecutionerMenu(syncId, playerInventory, this, this.data);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return inventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return inventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    @Override
    public boolean isInputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == WEAPON_SLOT && stack.getItem() instanceof SwordItem;
    }

    @Override
    public boolean isOutputSlot(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= 0 && slot < 9;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        // Slots 0..8 accessible for output from any side
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == WEAPON_SLOT && stack.getItem() instanceof SwordItem;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 0 && slot < 9;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, inventory.getItems(), registries);
        tag.putLong("Energy", energyStorage.amount);
        tag.putInt("StoredXp", storedXp);
        tag.putInt("TargetMode", targetMode);
        tag.putInt("KillCount", killCount);
        tag.putInt("SpawnerEnabled", spawnerEnabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.clearContent();
        ContainerHelper.loadAllItems(tag, inventory.getItems(), registries);
        energyStorage.amount = tag.getLong("Energy");
        storedXp = tag.getInt("StoredXp");
        targetMode = tag.getInt("TargetMode");
        killCount = tag.getInt("KillCount");
        if (tag.contains("SpawnerEnabled")) {
            spawnerEnabled = tag.getInt("SpawnerEnabled");
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
}
