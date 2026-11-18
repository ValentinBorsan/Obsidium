package net.valentin.mythicrails;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Self-propelled modular rail cart with swappable Motor (Coal/Electric) and Machine Payload modules. */
public final class HermesFreightCart extends MinecartChest implements WorldlyContainer {
    public static final int CARGO_SIZE = 54;
    public static final int SLOT_MOTOR = 54;
    public static final int SLOT_MODULE = 55;
    public static final int SLOT_FUEL = 56;
    public static final int SLOT_AUX = 57;
    public static final int TOTAL_SLOTS = 58;

    public static final int MAX_FLUID = 32_000;
    public static final int BASE_MAX_ENERGY = 200_000;
    public static final int CELL_MAX_ENERGY = 2_000_000;

    private static final int[] CARGO_SLOTS = new int[CARGO_SIZE];
    static {
        for (int i = 0; i < CARGO_SIZE; i++) CARGO_SLOTS[i] = i;
    }

    private static final EntityDataAccessor<Integer> DISPLAY_PAYLOAD = SynchedEntityData.defineId(
            HermesFreightCart.class, EntityDataSerializers.INT);

    private final FreightDocking docking = new FreightDocking();
    private boolean running;
    private Direction heading = Direction.NORTH;
    private int fuelTicks;
    private int totalFuelTicks;
    private int energy;
    private int fluidAmount;
    private int fluidType; // 0 = empty, 1 = water, 2 = lava
    private int workProgress; // 0..100
    private int arboristCooldown;
    private int bumperCooldown;
    private boolean railBrake;

    public HermesFreightCart(EntityType<? extends HermesFreightCart> type, Level level) {
        super(type, level);
        clearItemStacks();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISPLAY_PAYLOAD, 0);
    }

    public CartModuleItem.MotorType motorType() {
        ItemStack stack = getItem(SLOT_MOTOR);
        return stack.getItem() instanceof CartModuleItem mod && mod.isMotor()
                ? mod.motorType() : CartModuleItem.MotorType.NONE;
    }

    public CartModuleItem.PayloadType payloadType() {
        ItemStack stack = getItem(SLOT_MODULE);
        return stack.getItem() instanceof CartModuleItem mod && mod.isPayload()
                ? mod.payloadType() : CartModuleItem.PayloadType.NONE;
    }

    public int maxEnergy() {
        return payloadType() == CartModuleItem.PayloadType.ENERGY_CELL ? CELL_MAX_ENERGY : BASE_MAX_ENERGY;
    }

    public boolean running() { return running; }
    public Direction heading() { return heading; }
    public int fuelTicks() { return fuelTicks; }
    public int totalFuelTicks() { return totalFuelTicks; }
    public int energy() { return energy; }
    public int fluidAmount() { return fluidAmount; }
    public int fluidType() { return fluidType; }
    public int workProgress() { return workProgress; }

    public int addEnergy(int amount) {
        if (amount <= 0 || energy >= maxEnergy()) return 0;
        int added = Math.min(amount, maxEnergy() - energy);
        energy += added;
        return added;
    }

    public int extractEnergy(int amount) {
        if (amount <= 0 || energy <= 0) return 0;
        int taken = Math.min(amount, energy);
        energy -= taken;
        return taken;
    }

    public int addFluid(int type, int mb) {
        if (type <= 0 || mb <= 0 || (fluidAmount > 0 && fluidType != type)) return 0;
        int added = Math.min(mb, MAX_FLUID - fluidAmount);
        if (added > 0) {
            fluidType = type;
            fluidAmount += added;
        }
        return added;
    }

    public int extractFluid(int mb) {
        if (mb <= 0 || fluidAmount <= 0) return 0;
        int taken = Math.min(mb, fluidAmount);
        fluidAmount -= taken;
        if (fluidAmount == 0) fluidType = 0;
        return taken;
    }
    public int getDockStatus() { return docking.status(); }
    public boolean isHeldByStation() { return docking.holds(this); }
    public boolean releaseFromStation(BlockPos pos) { return docking.release(this, pos); }

    public void setRunning(boolean value) {
        running = value;
        if (!value) setDeltaMovement(Vec3.ZERO);
    }

    public void setHeading(Direction direction) {
        if (direction.getAxis().isHorizontal()) {
            heading = direction;
            setDeltaMovement(Vec3.ZERO);
        }
    }

    public static boolean acceptsFuel(ItemStack stack) {
        return !stack.isEmpty() && !stack.getItem().hasCraftingRemainingItem()
                && AbstractFurnaceBlockEntity.isFuel(stack);
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            super.tick();
            return;
        }

        entityData.set(DISPLAY_PAYLOAD, payloadType().id);

        if (energy > maxEnergy()) energy = maxEnergy();

        // Charge internal RF from Aux slot (Redstone / Redstone Block / Coal)
        chargeFromAux();

        // Module work ticks (Liquid Barrel, Arborist, Smelter, Grinder)
        tickPayloadModule();

        // Station docking takes priority
        if (docking.tick(this)) return;

        BlockPos railPos = railAt(level(), position());

        // Rail Bumper & Semaphore control
        if (railPos != null) {
            if (handleRailBumper(railPos)) return;
            if (handleRailSemaphore(railPos)) return;
        }

        // Self-propelled drive
        var motor = motorType();
        if (running && motor != CartModuleItem.MotorType.NONE && railPos != null) {
            boolean powered = false;
            double maxSpeed = 0.28;
            double accel = 0.035;
            if (motor == CartModuleItem.MotorType.COAL) {
                ItemStack fuelStack = getItem(SLOT_FUEL);
                if (fuelTicks <= 0 && acceptsFuel(fuelStack)) {
                    totalFuelTicks = Math.clamp(AbstractFurnaceBlockEntity.getFuel().getOrDefault(fuelStack.getItem(), 0), 0, 72000);
                    fuelTicks = totalFuelTicks;
                    if (fuelTicks > 0) fuelStack.shrink(1);
                }
                if (fuelTicks > 0) {
                    fuelTicks--;
                    powered = true;
                }
            } else if (motor == CartModuleItem.MotorType.ELECTRIC) {
                if (energy >= 20) {
                    energy -= 20;
                    powered = true;
                    maxSpeed = 0.34;
                }
            }

            if (powered) {
                BlockState railState = level().getBlockState(railPos);
                if (railState.getBlock() instanceof VolcaniteSpeedRailBlock speedRail) {
                    var shape = railState.getValue(speedRail.getShapeProperty());
                    boolean curved = shape == net.minecraft.world.level.block.state.properties.RailShape.NORTH_EAST
                            || shape == net.minecraft.world.level.block.state.properties.RailShape.NORTH_WEST
                            || shape == net.minecraft.world.level.block.state.properties.RailShape.SOUTH_EAST
                            || shape == net.minecraft.world.level.block.state.properties.RailShape.SOUTH_WEST;
                    maxSpeed = curved ? 0.40 : 0.72;
                    accel = 0.06;
                }
                var motion = getDeltaMovement();
                var direction = motion.horizontalDistanceSqr() > 0.0001
                        ? new Vec3(motion.x, 0, motion.z).normalize()
                        : new Vec3(heading.getStepX(), 0, heading.getStepZ());
                double speed = Math.min(maxSpeed, motion.horizontalDistance() + accel);
                setDeltaMovement(direction.x * speed, motion.y, direction.z * speed);
            } else {
                running = false;
                setDeltaMovement(Vec3.ZERO);
            }
        } else if (motor == CartModuleItem.MotorType.NONE && running) {
            running = false;
        }

        super.tick();

        var motion = getDeltaMovement();
        if (motion.horizontalDistanceSqr() > 0.00001) {
            heading = Math.abs(motion.x) >= Math.abs(motion.z)
                    ? (motion.x >= 0 ? Direction.EAST : Direction.WEST)
                    : (motion.z >= 0 ? Direction.SOUTH : Direction.NORTH);
        }
    }

    private void chargeFromAux() {
        var motor = motorType();
        var payload = payloadType();
        if (motor != CartModuleItem.MotorType.ELECTRIC && payload != CartModuleItem.PayloadType.ENERGY_CELL) return;
        if (energy >= maxEnergy()) return;
        ItemStack aux = getItem(SLOT_AUX);
        if (aux.isEmpty()) return;
        int gain = 0;
        if (aux.is(Items.REDSTONE)) gain = 8_000;
        else if (aux.is(Items.REDSTONE_BLOCK)) gain = 72_000;
        else if (aux.is(Items.COAL) || aux.is(Items.CHARCOAL)) gain = 4_000;
        if (gain > 0) {
            energy = Math.min(maxEnergy(), energy + gain);
            aux.shrink(1);
        }
    }

    private boolean consumeWorkPower(int rfCost, int burnCost) {
        if (energy >= rfCost) {
            energy -= rfCost;
            return true;
        }
        if (fuelTicks >= burnCost) {
            fuelTicks -= burnCost;
            return true;
        }
        ItemStack fuelStack = getItem(SLOT_FUEL);
        if (acceptsFuel(fuelStack)) {
            totalFuelTicks = Math.clamp(AbstractFurnaceBlockEntity.getFuel().getOrDefault(fuelStack.getItem(), 0), 0, 72000);
            fuelTicks = totalFuelTicks;
            if (fuelTicks > 0) {
                fuelStack.shrink(1);
                if (fuelTicks >= burnCost) {
                    fuelTicks -= burnCost;
                    return true;
                }
            }
        }
        return false;
    }

    private void tickPayloadModule() {
        switch (payloadType()) {
            case LIQUID_BARREL -> tickLiquidBarrel();
            case ARBORIST -> tickArborist();
            case SMELTER -> tickSmelter();
            case GRINDER -> tickGrinder();
            default -> workProgress = 0;
        }
    }

    private void tickLiquidBarrel() {
        workProgress = 0;
        ItemStack aux = getItem(SLOT_AUX);
        if (aux.isEmpty()) return;
        if (aux.is(Items.WATER_BUCKET) && (fluidAmount == 0 || fluidType == 1) && fluidAmount + 1000 <= MAX_FLUID) {
            fluidType = 1;
            fluidAmount += 1000;
            setItem(SLOT_AUX, new ItemStack(Items.BUCKET));
        } else if (aux.is(Items.LAVA_BUCKET) && (fluidAmount == 0 || fluidType == 2) && fluidAmount + 1000 <= MAX_FLUID) {
            fluidType = 2;
            fluidAmount += 1000;
            setItem(SLOT_AUX, new ItemStack(Items.BUCKET));
        } else if (aux.is(Items.BUCKET) && aux.getCount() == 1 && fluidAmount >= 1000) {
            Item filled = fluidType == 2 ? Items.LAVA_BUCKET : Items.WATER_BUCKET;
            fluidAmount -= 1000;
            if (fluidAmount == 0) fluidType = 0;
            setItem(SLOT_AUX, new ItemStack(filled));
        }
    }

    private void tickArborist() {
        if (arboristCooldown > 0) {
            arboristCooldown--;
            workProgress = (20 - arboristCooldown) * 5;
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) return;
        BlockPos center = blockPosition();
        BlockPos targetLog = null;
        for (int dy = 0; dy <= 4 && targetLog == null; dy++) {
            for (int dx = -2; dx <= 2 && targetLog == null; dx++) {
                for (int dz = -2; dz <= 2 && targetLog == null; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState st = serverLevel.getBlockState(p);
                    if (st.is(BlockTags.LOGS) || st.is(BlockTags.LEAVES)) {
                        targetLog = p;
                    }
                }
            }
        }
        if (targetLog == null) {
            workProgress = 0;
            arboristCooldown = 10;
            return;
        }
        if (!consumeWorkPower(120, 15)) return;

        BlockState state = serverLevel.getBlockState(targetLog);
        boolean wasLog = state.is(BlockTags.LOGS);
        var drops = Block.getDrops(state, serverLevel, targetLog, null);
        serverLevel.destroyBlock(targetLog, false);
        for (ItemStack drop : drops) {
            insertIntoCargo(drop);
        }
        if (wasLog) {
            BlockPos below = targetLog.below();
            BlockState soil = serverLevel.getBlockState(below);
            if (soil.is(BlockTags.DIRT) && serverLevel.getBlockState(targetLog).isAir()) {
                if (consumeSaplingFromCargo(serverLevel, targetLog)) {
                    // Replanted from cargo
                }
            }
        }
        arboristCooldown = 20;
        workProgress = 100;
    }

    private boolean consumeSaplingFromCargo(ServerLevel serverLevel, BlockPos pos) {
        for (int i = 0; i < CARGO_SIZE; i++) {
            ItemStack st = getItem(i);
            if (!st.isEmpty() && st.is(net.minecraft.tags.ItemTags.SAPLINGS)) {
                Block block = Block.byItem(st.getItem());
                if (block != Blocks.AIR) {
                    st.shrink(1);
                    serverLevel.setBlock(pos, block.defaultBlockState(), 3);
                    return true;
                }
            }
        }
        return false;
    }

    private void tickSmelter() {
        if (level().getGameTime() % 2 != 0) return;
        int inputSlot = -1;
        ItemStack output = ItemStack.EMPTY;
        for (int i = 0; i < CARGO_SIZE; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            Optional<RecipeHolder<SmeltingRecipe>> recipe = level().getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level());
            if (recipe.isPresent()) {
                ItemStack result = recipe.get().value().getResultItem(level().registryAccess()).copy();
                if (!result.isEmpty() && canInsertIntoCargo(result)) {
                    inputSlot = i;
                    output = result;
                    break;
                }
            }
        }
        if (inputSlot < 0) {
            workProgress = 0;
            return;
        }
        if (!consumeWorkPower(40, 4)) return;
        workProgress += 10;
        if (workProgress >= 100) {
            workProgress = 0;
            getItem(inputSlot).shrink(1);
            insertIntoCargo(output);
        }
    }

    private void tickGrinder() {
        if (level().getGameTime() % 2 != 0) return;
        int inputSlot = -1;
        ItemStack output = ItemStack.EMPTY;
        for (int i = 0; i < CARGO_SIZE; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            ItemStack ground = grindResult(stack);
            if (!ground.isEmpty() && canInsertIntoCargo(ground)) {
                inputSlot = i;
                output = ground;
                break;
            }
        }
        if (inputSlot < 0) {
            workProgress = 0;
            return;
        }
        if (!consumeWorkPower(40, 4)) return;
        workProgress += 10;
        if (workProgress >= 100) {
            workProgress = 0;
            getItem(inputSlot).shrink(1);
            insertIntoCargo(output);
        }
    }

    private static ItemStack grindResult(ItemStack input) {
        Item item = input.getItem();
        if (item == Items.IRON_ORE || item == Items.DEEPSLATE_IRON_ORE) return new ItemStack(Items.RAW_IRON, 2);
        if (item == Items.GOLD_ORE || item == Items.DEEPSLATE_GOLD_ORE) return new ItemStack(Items.RAW_GOLD, 2);
        if (item == Items.COPPER_ORE || item == Items.DEEPSLATE_COPPER_ORE) return new ItemStack(Items.RAW_COPPER, 4);
        if (item == Items.COAL_ORE || item == Items.DEEPSLATE_COAL_ORE) return new ItemStack(Items.COAL, 2);
        if (item == Items.DIAMOND_ORE || item == Items.DEEPSLATE_DIAMOND_ORE) return new ItemStack(Items.DIAMOND, 2);
        if (item == Items.EMERALD_ORE || item == Items.DEEPSLATE_EMERALD_ORE) return new ItemStack(Items.EMERALD, 2);
        if (item == Items.REDSTONE_ORE || item == Items.DEEPSLATE_REDSTONE_ORE) return new ItemStack(Items.REDSTONE, 6);
        if (item == Items.LAPIS_ORE || item == Items.DEEPSLATE_LAPIS_ORE) return new ItemStack(Items.LAPIS_LAZULI, 6);
        if (item == Items.COBBLESTONE) return new ItemStack(Items.GRAVEL);
        if (item == Items.GRAVEL) return new ItemStack(Items.SAND);
        if (item == Items.SANDSTONE) return new ItemStack(Items.SAND, 4);
        if (item == Items.BONE) return new ItemStack(Items.BONE_MEAL, 4);
        if (item == Items.BLAZE_ROD) return new ItemStack(Items.BLAZE_POWDER, 3);
        return ItemStack.EMPTY;
    }

    private boolean canInsertIntoCargo(ItemStack stack) {
        for (int i = 0; i < CARGO_SIZE; i++) {
            ItemStack cur = getItem(i);
            if (cur.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(cur, stack) && cur.getCount() + stack.getCount() <= cur.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private void insertIntoCargo(ItemStack stack) {
        if (stack.isEmpty()) return;
        for (int i = 0; i < CARGO_SIZE && !stack.isEmpty(); i++) {
            ItemStack cur = getItem(i);
            if (!cur.isEmpty() && ItemStack.isSameItemSameComponents(cur, stack) && cur.getCount() < cur.getMaxStackSize()) {
                int add = Math.min(stack.getCount(), cur.getMaxStackSize() - cur.getCount());
                cur.grow(add);
                stack.shrink(add);
            }
        }
        for (int i = 0; i < CARGO_SIZE && !stack.isEmpty(); i++) {
            if (getItem(i).isEmpty()) {
                setItem(i, stack.copy());
                stack.setCount(0);
                break;
            }
        }
    }

    static BlockPos railAt(Level level, Vec3 point) {
        var pos = BlockPos.containing(point.x, point.y + 0.1, point.z);
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return null;
        if (level.getBlockState(pos).getBlock() instanceof BaseRailBlock) return pos;
        pos = pos.below();
        return level.getBlockState(pos).getBlock() instanceof BaseRailBlock ? pos : null;
    }

    private boolean handleRailBumper(BlockPos railPos) {
        if (bumperCooldown > 0) {
            bumperCooldown--;
            return false;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState adjacent = level().getBlockState(railPos.relative(dir));
            if (adjacent.getBlock() instanceof RailBumperBlock
                    && adjacent.getValue(RailBumperBlock.FACING) == dir.getOpposite()) {
                boolean powered = adjacent.getValue(RailBumperBlock.POWERED);
                if (powered) {
                    Direction launchDir = dir.getOpposite();
                    heading = launchDir;
                    bumperCooldown = 20;
                    if (motorType() != CartModuleItem.MotorType.NONE) running = true;
                    setPos(railPos.getX() + 0.5 + launchDir.getStepX() * 0.25, getY(),
                            railPos.getZ() + 0.5 + launchDir.getStepZ() * 0.25);
                    setDeltaMovement(launchDir.getStepX() * 0.35, 0, launchDir.getStepZ() * 0.35);
                    return false;
                }
                Vec3 motion = getDeltaMovement();
                double toward = motion.x * dir.getStepX() + motion.z * dir.getStepZ();
                if (toward >= -0.02) {
                    setDeltaMovement(Vec3.ZERO);
                    setPos(railPos.getX() + 0.5, getY(), railPos.getZ() + 0.5);
                    tickVanillaStopped();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleRailSemaphore(BlockPos railPos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState adjacent = level().getBlockState(railPos.relative(dir));
            if (adjacent.getBlock() instanceof RailSemaphoreBlock
                    && adjacent.getValue(RailSemaphoreBlock.RED)
                    && adjacent.getValue(RailSemaphoreBlock.FACING) == heading) {
                setDeltaMovement(Vec3.ZERO);
                tickVanillaStopped();
                return true;
            }
        }
        return false;
    }

    @Override
    protected double getMaxSpeed() {
        BlockPos railPos = railAt(level(), position());
        if (railPos != null) {
            BlockState state = level().getBlockState(railPos);
            if (state.getBlock() instanceof VolcaniteSpeedRailBlock speedRail) {
                var shape = state.getValue(speedRail.getShapeProperty());
                boolean curved = shape == net.minecraft.world.level.block.state.properties.RailShape.NORTH_EAST
                        || shape == net.minecraft.world.level.block.state.properties.RailShape.NORTH_WEST
                        || shape == net.minecraft.world.level.block.state.properties.RailShape.SOUTH_EAST
                        || shape == net.minecraft.world.level.block.state.properties.RailShape.SOUTH_WEST;
                return curved ? 0.42D : 0.78D;
            } else if (state.getBlock() instanceof ObsidianRailBlock) {
                return 0.52D;
            }
        }
        return super.getMaxSpeed();
    }

    void tickVanillaStopped() {
        railBrake = true;
        try {
            super.tick();
        } finally {
            railBrake = false;
        }
    }

    @Override
    protected void moveAlongTrack(BlockPos pos, BlockState state) {
        if (railBrake) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.moveAlongTrack(pos, state);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        docking.save(tag);
        tag.putBoolean("Running", running);
        tag.putInt("Heading", heading.get2DDataValue());
        tag.putInt("FuelTicks", fuelTicks);
        tag.putInt("TotalFuelTicks", totalFuelTicks);
        tag.putInt("Energy", energy);
        tag.putInt("FluidAmount", fluidAmount);
        tag.putInt("FluidType", fluidType);
        tag.putInt("WorkProgress", workProgress);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        docking.load(tag);
        running = tag.getBoolean("Running");
        heading = Direction.from2DDataValue(tag.getInt("Heading"));
        fuelTicks = Math.max(0, tag.getInt("FuelTicks"));
        totalFuelTicks = Math.max(0, tag.getInt("TotalFuelTicks"));
        energy = Math.clamp(tag.getInt("Energy"), 0, CELL_MAX_ENERGY);
        fluidAmount = Math.clamp(tag.getInt("FluidAmount"), 0, MAX_FLUID);
        fluidType = Math.clamp(tag.getInt("FluidType"), 0, 2);
        workProgress = Math.clamp(tag.getInt("WorkProgress"), 0, 100);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        int id = entityData.get(DISPLAY_PAYLOAD);
        return switch (CartModuleItem.PayloadType.byId(id)) {
            case LIQUID_BARREL -> Blocks.BARREL.defaultBlockState();
            case ENERGY_CELL -> MythicRails.ENERGY_CELL_DISPLAY.defaultBlockState();
            case ARBORIST -> Blocks.COMPOSTER.defaultBlockState();
            case SMELTER -> Blocks.BLAST_FURNACE.defaultBlockState();
            case GRINDER -> Blocks.STONECUTTER.defaultBlockState();
            default -> MythicRails.CARGO_DISPLAY.defaultBlockState();
        };
    }

    @Override
    public int getContainerSize() { return TOTAL_SLOTS; }

    @Override
    public int[] getSlotsForFace(Direction side) { return CARGO_SLOTS; }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index >= 0 && index < CARGO_SIZE;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index >= 0 && index < CARGO_SIZE;
    }

    @Override
    protected Item getDropItem() { return MythicRails.FREIGHT_CART_ITEM; }

    @Override
    public ItemStack getPickResult() { return new ItemStack(MythicRails.FREIGHT_CART_ITEM); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new FreightCartMenu(id, inventory, this);
    }
}
