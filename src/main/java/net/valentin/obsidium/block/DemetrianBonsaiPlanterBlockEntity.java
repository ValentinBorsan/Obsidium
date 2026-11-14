package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.VolcanicHoneyBottleItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DemetrianBonsaiPlanterBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int SLOT_SOIL = 0;
    public static final int SLOT_SAPLING = 1;
    public static final int OUTPUT_START = 2;
    public static final int OUTPUT_COUNT = 5;
    public static final int TOTAL_SLOTS = 7;

    public static final int BASE_GROWTH_TICKS = 1200; // 60 seconds per harvest (30s with Volcanic Ash)

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    private int growthTicks = 0;
    private int maxGrowthTicks = BASE_GROWTH_TICKS;

    public DemetrianBonsaiPlanterBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.DEMETRIAN_BONSAI_PLANTER_BE, pos, state);
    }

    public ItemStack getSoil() {
        return items.get(SLOT_SOIL);
    }

    public ItemStack getSapling() {
        return items.get(SLOT_SAPLING);
    }

    public float getGrowthRatio() {
        if (maxGrowthTicks <= 0 || items.get(SLOT_SAPLING).isEmpty())
            return 0.0F;
        return Math.min(1.0F, (float) growthTicks / (float) maxGrowthTicks);
    }

    public void clientTick() {
        if (!items.get(SLOT_SAPLING).isEmpty()) {
            ItemStack soil = items.get(SLOT_SOIL);
            int speedMultiplier = (!soil.isEmpty() && soil.is(ObsidiumMod.VOLCANIC_ASH)) ? 2 : 1;
            if (growthTicks < maxGrowthTicks) {
                growthTicks += speedMultiplier;
            } else {
                growthTicks = maxGrowthTicks;
            }
            if (growthTicks >= maxGrowthTicks && this.level != null) {
                if (this.level.random.nextFloat() < 0.20F) {
                    double px = this.worldPosition.getX() + 0.25 + this.level.random.nextDouble() * 0.5;
                    double py = this.worldPosition.getY() + 0.7 + this.level.random.nextDouble() * 0.4;
                    double pz = this.worldPosition.getZ() + 0.25 + this.level.random.nextDouble() * 0.5;
                    this.level.addParticle(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 0.0, 0.02, 0.0);
                }
            }
        } else {
            growthTicks = 0;
        }
    }

    public void serverTick() {
        ItemStack sapling = items.get(SLOT_SAPLING);
        if (sapling.isEmpty()) {
            growthTicks = 0;
            return;
        }

        // Soil bonus calculation
        ItemStack soil = items.get(SLOT_SOIL);
        int speedMultiplier = 1;
        if (!soil.isEmpty() && soil.is(ObsidiumMod.VOLCANIC_ASH)) {
            speedMultiplier = 2; // Volcanic Ash grows 2x faster!
        } else if (!soil.isEmpty() && soil.is(Items.PODZOL)) {
            speedMultiplier = 1; // Podzol baseline
        }

        if (growthTicks < maxGrowthTicks) {
            growthTicks += speedMultiplier;
            if (growthTicks > maxGrowthTicks) {
                growthTicks = maxGrowthTicks;
            }

            // Sync periodically to keep client animation in lockstep
            if (growthTicks % 40 == 0 || growthTicks >= maxGrowthTicks) {
                setChanged();
                syncClient();
            }
        }
    }

    public boolean isMature() {
        return !items.get(SLOT_SAPLING).isEmpty() && maxGrowthTicks > 0 && growthTicks >= maxGrowthTicks;
    }

    public void resetGrowth() {
        this.growthTicks = 0;
        setChanged();
        syncClient();
    }

    public List<ItemStack> generateBonsaiDrops(Item saplingItem) {
        List<ItemStack> drops = new ArrayList<>();
        if (this.level == null)
            return drops;

        // 1. Twigs / Sticks from pruning (1 to 2 sticks) + small guaranteed log yield (1, rarely 2)
        drops.add(new ItemStack(Items.STICK, 1 + this.level.random.nextInt(2)));
        drops.add(new ItemStack(getLogForSapling(saplingItem), this.level.random.nextFloat() < 0.25F ? 2 : 1));

        // 2. Sapling chance ("puieti unde e cazul"):
        // Normal trees have 20% chance to drop 1 sapling.
        // Sterile Apex Trees (Tartarean Netherite & Adamantine Diamond) NEVER drop
        // saplings!
        boolean isSterile = (saplingItem == ObsidiumMod.TARTAREAN_NETHERITE_SAPLING.asItem()
                || saplingItem == ObsidiumMod.ADAMANTINE_DIAMOND_SAPLING.asItem());
        if (!isSterile && this.level.random.nextFloat() < 0.20F) {
            drops.add(new ItemStack(saplingItem, 1));
        }

        // 3. Foliage & Metal Fruit Drops ("frunzele si fructe - nu dau direct
        // nuggets"):
        if (saplingItem == Items.OAK_SAPLING) {
            drops.add(new ItemStack(Items.OAK_LEAVES, 1 + this.level.random.nextInt(2)));
            if (this.level.random.nextFloat() < 0.35F)
                drops.add(new ItemStack(Items.APPLE, 1));
        } else if (saplingItem == Items.BIRCH_SAPLING) {
            drops.add(new ItemStack(Items.BIRCH_LEAVES, 1 + this.level.random.nextInt(2)));
        } else if (saplingItem == Items.SPRUCE_SAPLING) {
            drops.add(new ItemStack(Items.SPRUCE_LEAVES, 1 + this.level.random.nextInt(2)));
        } else if (saplingItem == Items.DARK_OAK_SAPLING) {
            drops.add(new ItemStack(Items.DARK_OAK_LEAVES, 1 + this.level.random.nextInt(2)));
            if (this.level.random.nextFloat() < 0.35F)
                drops.add(new ItemStack(Items.APPLE, 1));
        } else if (saplingItem == Items.ACACIA_SAPLING) {
            drops.add(new ItemStack(Items.ACACIA_LEAVES, 1 + this.level.random.nextInt(2)));
        } else if (saplingItem == Items.JUNGLE_SAPLING) {
            drops.add(new ItemStack(Items.JUNGLE_LEAVES, 1 + this.level.random.nextInt(2)));
            if (this.level.random.nextFloat() < 0.35F)
                drops.add(new ItemStack(Items.COCOA_BEANS, 1));
        } else if (saplingItem == Items.CHERRY_SAPLING) {
            drops.add(new ItemStack(Items.CHERRY_LEAVES, 1 + this.level.random.nextInt(2)));
        } else if (saplingItem == Items.MANGROVE_PROPAGULE) {
            drops.add(new ItemStack(Items.MANGROVE_LEAVES, 1 + this.level.random.nextInt(2)));

            // Obsidium Ore Trees - Yields authentic Metal Fruits!
        } else if (saplingItem == ObsidiumMod.ARES_IRON_SAPLING.asItem()) {
            drops.add(new ItemStack(ObsidiumMod.ARES_IRON_LEAVES_ITEM, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(ObsidiumMod.ARES_IRON_FRUIT, 1));
        } else if (saplingItem == ObsidiumMod.MIDAS_GOLD_SAPLING.asItem()) {
            drops.add(new ItemStack(ObsidiumMod.MIDAS_GOLD_LEAVES_ITEM, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(ObsidiumMod.MIDAS_GOLD_FRUIT, 1));
        } else if (saplingItem == ObsidiumMod.HERMES_COPPER_SAPLING.asItem()) {
            drops.add(new ItemStack(ObsidiumMod.HERMES_COPPER_LEAVES_ITEM, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(ObsidiumMod.HERMES_COPPER_FRUIT, 1));
        } else if (saplingItem == ObsidiumMod.CRONUS_LEAD_SAPLING.asItem()) {
            drops.add(new ItemStack(ObsidiumMod.CRONUS_LEAD_LEAVES_ITEM, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(ObsidiumMod.CRONUS_LEAD_FRUIT, 1));
        } else if (saplingItem == ObsidiumMod.ZEUS_TIN_SAPLING.asItem()) {
            drops.add(new ItemStack(ObsidiumMod.ZEUS_TIN_LEAVES_ITEM, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(ObsidiumMod.ZEUS_TIN_FRUIT, 1));
        } else if (saplingItem == ObsidiumMod.ARTEMIS_SILVER_SAPLING.asItem()) {
            drops.add(new ItemStack(ObsidiumMod.ARTEMIS_SILVER_LEAVES_ITEM, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(ObsidiumMod.ARTEMIS_SILVER_FRUIT, 1));
        } else if (saplingItem == ObsidiumMod.HEPHAESTUS_VOLCANITE_SAPLING.asItem()) {
            drops.add(new ItemStack(ObsidiumMod.HEPHAESTUS_VOLCANITE_LEAVES_ITEM, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT, 1));
            // 5% Rare Hybrid Mutation into Pyretic Titan Volcanite Sapling!
            if (this.level.random.nextFloat() < 0.05F) {
                drops.add(new ItemStack(ObsidiumMod.PYRETIC_TITAN_SAPLING_ITEM, 1));
            }
            // Rare Mega Bonsai Apex Hybrid - DOUBLE VOLCANITE FRUITS!
        } else if (saplingItem == ObsidiumMod.PYRETIC_TITAN_SAPLING_ITEM) {
            drops.add(new ItemStack(ObsidiumMod.PYRETIC_TITAN_LEAVES_ITEM, 2));
            drops.add(new ItemStack(ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT, 2)); // Double Volcanite Fruit yield!
            if (this.level.random.nextFloat() < 0.35F)
                drops.add(new ItemStack(ObsidiumMod.VOLCANIC_ASH, 1));
        } else if (saplingItem == ObsidiumMod.ELECTRUM_SAPLING.asItem()) {
            drops.add(new ItemStack(ObsidiumMod.ELECTRUM_LEAVES_ITEM, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(ObsidiumMod.ELECTRUM_FRUIT, 1));
        } else if (saplingItem == ObsidiumMod.TARTAREAN_NETHERITE_SAPLING.asItem()) {
            drops.add(new ItemStack(ObsidiumMod.TARTAREAN_NETHERITE_LEAVES_ITEM, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(ObsidiumMod.TARTAREAN_NETHERITE_FRUIT, 1));
        } else if (saplingItem == ObsidiumMod.HADES_COAL_SAPLING.asItem()) {
            drops.add(new ItemStack(Items.OAK_LEAVES, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(Items.COAL, 1));
        } else if (saplingItem == ObsidiumMod.POSEIDON_LAPIS_SAPLING.asItem()) {
            drops.add(new ItemStack(Items.SPRUCE_LEAVES, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(Items.LAPIS_LAZULI, 1 + this.level.random.nextInt(2)));
        } else if (saplingItem == ObsidiumMod.ASTRAPE_REDSTONE_SAPLING.asItem()) {
            drops.add(new ItemStack(Items.BIRCH_LEAVES, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(Items.REDSTONE, 1 + this.level.random.nextInt(2)));
        } else if (saplingItem == ObsidiumMod.APOLLO_QUARTZ_SAPLING.asItem()) {
            drops.add(new ItemStack(Items.BIRCH_LEAVES, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(Items.QUARTZ, 1));
        } else if (saplingItem == ObsidiumMod.DIONYSIAN_AMETHYST_SAPLING.asItem()) {
            drops.add(new ItemStack(Items.DARK_OAK_LEAVES, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(Items.AMETHYST_SHARD, 1));
        } else if (saplingItem == ObsidiumMod.HYPERION_GLOWSTONE_SAPLING.asItem()) {
            drops.add(new ItemStack(Items.OAK_LEAVES, 1 + this.level.random.nextInt(2)));
            drops.add(new ItemStack(Items.GLOWSTONE_DUST, 1 + this.level.random.nextInt(2)));
        } else if (saplingItem == ObsidiumMod.PLOUTON_EMERALD_SAPLING.asItem()) {
            drops.add(new ItemStack(Items.JUNGLE_LEAVES, 1 + this.level.random.nextInt(2)));
            if (this.level.random.nextFloat() < 0.25F)
                drops.add(new ItemStack(Items.EMERALD, 1));
        } else if (saplingItem == ObsidiumMod.ADAMANTINE_DIAMOND_SAPLING.asItem()) {
            drops.add(new ItemStack(ObsidiumMod.HEPHAESTUS_VOLCANITE_LEAVES_ITEM, 1 + this.level.random.nextInt(2)));
            if (this.level.random.nextFloat() < 0.15F)
                drops.add(new ItemStack(Items.DIAMOND, 1));
            else
                drops.add(new ItemStack(Items.AMETHYST_SHARD, 1));
        } else {
            drops.add(new ItemStack(Items.OAK_LEAVES, 1 + this.level.random.nextInt(2)));
        }

        return drops;
    }

    private static Item getLogForSapling(Item saplingItem) {
        if (saplingItem == Items.BIRCH_SAPLING
                || saplingItem == ObsidiumMod.ASTRAPE_REDSTONE_SAPLING.asItem()
                || saplingItem == ObsidiumMod.APOLLO_QUARTZ_SAPLING.asItem()) {
            return Items.BIRCH_LOG;
        }
        if (saplingItem == Items.SPRUCE_SAPLING || saplingItem == ObsidiumMod.POSEIDON_LAPIS_SAPLING.asItem()) {
            return Items.SPRUCE_LOG;
        }
        if (saplingItem == Items.DARK_OAK_SAPLING || saplingItem == ObsidiumMod.DIONYSIAN_AMETHYST_SAPLING.asItem()) {
            return Items.DARK_OAK_LOG;
        }
        if (saplingItem == Items.ACACIA_SAPLING) return Items.ACACIA_LOG;
        if (saplingItem == Items.JUNGLE_SAPLING || saplingItem == ObsidiumMod.PLOUTON_EMERALD_SAPLING.asItem()) {
            return Items.JUNGLE_LOG;
        }
        if (saplingItem == Items.CHERRY_SAPLING) return Items.CHERRY_LOG;
        if (saplingItem == Items.MANGROVE_PROPAGULE) return Items.MANGROVE_LOG;
        if (saplingItem == ObsidiumMod.ARES_IRON_SAPLING.asItem()) return ObsidiumMod.ARES_IRON_LOG_ITEM;
        if (saplingItem == ObsidiumMod.MIDAS_GOLD_SAPLING.asItem()) return ObsidiumMod.MIDAS_GOLD_LOG_ITEM;
        if (saplingItem == ObsidiumMod.HERMES_COPPER_SAPLING.asItem()) return ObsidiumMod.HERMES_COPPER_LOG_ITEM;
        if (saplingItem == ObsidiumMod.CRONUS_LEAD_SAPLING.asItem()) return ObsidiumMod.CRONUS_LEAD_LOG_ITEM;
        if (saplingItem == ObsidiumMod.ZEUS_TIN_SAPLING.asItem()) return ObsidiumMod.ZEUS_TIN_LOG_ITEM;
        if (saplingItem == ObsidiumMod.ARTEMIS_SILVER_SAPLING.asItem()) return ObsidiumMod.ARTEMIS_SILVER_LOG_ITEM;
        if (saplingItem == ObsidiumMod.HEPHAESTUS_VOLCANITE_SAPLING.asItem()
                || saplingItem == ObsidiumMod.ADAMANTINE_DIAMOND_SAPLING.asItem()) {
            return ObsidiumMod.HEPHAESTUS_VOLCANITE_LOG_ITEM;
        }
        if (saplingItem == ObsidiumMod.PYRETIC_TITAN_SAPLING_ITEM) return ObsidiumMod.PYRETIC_TITAN_LOG_ITEM;
        if (saplingItem == ObsidiumMod.ELECTRUM_SAPLING.asItem()) return ObsidiumMod.ELECTRUM_LOG_ITEM;
        if (saplingItem == ObsidiumMod.TARTAREAN_NETHERITE_SAPLING.asItem()) return ObsidiumMod.TARTAREAN_NETHERITE_LOG_ITEM;
        return Items.OAK_LOG;
    }

    public ItemInteractionResult onInteract(Player player, InteractionHand hand, ItemStack held) {
        if (this.level == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        // 1. Sneak + Empty hand: remove sapling or soil
        if (player.isShiftKeyDown() && held.isEmpty()) {
            if (!items.get(SLOT_SAPLING).isEmpty()) {
                ItemStack sapling = items.get(SLOT_SAPLING).copy();
                items.set(SLOT_SAPLING, ItemStack.EMPTY);
                growthTicks = 0;
                if (!player.getInventory().add(sapling)) {
                    player.drop(sapling, false);
                }
                this.level.playSound(player, this.worldPosition, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0F,
                        1.0F);
                setChanged();
                syncClient();
                return ItemInteractionResult.sidedSuccess(this.level.isClientSide());
            } else if (!items.get(SLOT_SOIL).isEmpty()) {
                ItemStack soil = items.get(SLOT_SOIL).copy();
                items.set(SLOT_SOIL, ItemStack.EMPTY);
                if (!player.getInventory().add(soil)) {
                    player.drop(soil, false);
                }
                this.level.playSound(player, this.worldPosition, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0F,
                        1.0F);
                setChanged();
                syncClient();
                return ItemInteractionResult.sidedSuccess(this.level.isClientSide());
            }
        }

        // 2. MATURE BONSAI HARVEST ON RIGHT CLICK:
        // "si fara sa distrugi copacul doar iei frunzele, bete, puieti unde e cazul si
        // fruncte si fructe mai putine si revine la nivel de puiet cand dai click
        // dreapta iar cand creste se face copac mare"
        if (isMature()) {
            if (!this.level.isClientSide()) {
                List<ItemStack> drops = generateBonsaiDrops(items.get(SLOT_SAPLING).getItem());
                for (ItemStack drop : drops) {
                    if (!player.getInventory().add(drop)) {
                        player.drop(drop, false);
                    }
                }
                // Growth returns back to sprout level!
                growthTicks = 0;
                setChanged();
                syncClient();

                if (this.level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.COMPOSTER, this.worldPosition.getX() + 0.5,
                            this.worldPosition.getY() + 0.6, this.worldPosition.getZ() + 0.5, 12, 0.25, 0.25, 0.25,
                            0.04);
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.worldPosition.getX() + 0.5,
                            this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.02);
                }
            }
            this.level.playSound(player, this.worldPosition, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.2F);
            this.level.playSound(player, this.worldPosition, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            return ItemInteractionResult.sidedSuccess(this.level.isClientSide());
        }

        // 3. Volcanic Honey Bottle: instant max maturity
        if (held.getItem() instanceof VolcanicHoneyBottleItem) {
            if (!items.get(SLOT_SAPLING).isEmpty() && growthTicks < maxGrowthTicks) {
                if (!this.level.isClientSide()) {
                    growthTicks = maxGrowthTicks;
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                        if (!player.getInventory().add(bottle)) {
                            player.drop(bottle, false);
                        }
                    }
                    if (this.level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.worldPosition.getX() + 0.5,
                                this.worldPosition.getY() + 0.6, this.worldPosition.getZ() + 0.5, 15, 0.3, 0.2, 0.3,
                                0.05);
                        sl.sendParticles(ParticleTypes.FLAME, this.worldPosition.getX() + 0.5,
                                this.worldPosition.getY() + 0.7, this.worldPosition.getZ() + 0.5, 8, 0.2, 0.1, 0.2,
                                0.02);
                    }
                }
                this.level.playSound(player, this.worldPosition, SoundEvents.HONEY_DRINK, SoundSource.BLOCKS, 1.0F,
                        1.2F);
                setChanged();
                syncClient();
                return ItemInteractionResult.sidedSuccess(this.level.isClientSide());
            }
        }

        // 4. Bone Meal: accelerate by 150 ticks
        if (held.getItem() instanceof BoneMealItem) {
            if (!items.get(SLOT_SAPLING).isEmpty() && growthTicks < maxGrowthTicks) {
                if (!this.level.isClientSide()) {
                    advanceGrowth(150);
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    if (this.level instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.worldPosition.getX() + 0.5,
                                this.worldPosition.getY() + 0.6, this.worldPosition.getZ() + 0.5, 8, 0.2, 0.2, 0.2,
                                0.05);
                    }
                }
                this.level.playSound(player, this.worldPosition, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F,
                        1.0F);
                return ItemInteractionResult.sidedSuccess(this.level.isClientSide());
            }
        }

        // 5. Empty hand: collect any output drops (if deposited by Arborist)
        if (held.isEmpty()) {
            boolean collected = false;
            for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
                ItemStack out = items.get(i);
                if (!out.isEmpty()) {
                    if (!player.getInventory().add(out.copy())) {
                        player.drop(out.copy(), false);
                    }
                    items.set(i, ItemStack.EMPTY);
                    collected = true;
                }
            }
            if (collected) {
                this.level.playSound(player, this.worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F,
                        1.2F);
                setChanged();
                return ItemInteractionResult.sidedSuccess(this.level.isClientSide());
            }
        }

        // 5. Soil item: insert soil into slot 0
        if (isSoilItem(held)) {
            if (items.get(SLOT_SOIL).isEmpty()) {
                if (!this.level.isClientSide()) {
                    ItemStack soil = held.split(1);
                    items.set(SLOT_SOIL, soil);
                }
                this.level.playSound(player, this.worldPosition, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 1.0F,
                        1.0F);
                setChanged();
                syncClient();
                return ItemInteractionResult.sidedSuccess(this.level.isClientSide());
            }
        }

        // 6. Sapling item: insert sapling into slot 1
        if (isSaplingItem(held)) {
            // If soil is empty, default to dirt automatically if player has dirt, or
            // require soil
            if (items.get(SLOT_SOIL).isEmpty()) {
                items.set(SLOT_SOIL, new ItemStack(Items.DIRT));
            }
            if (items.get(SLOT_SAPLING).isEmpty()) {
                if (!this.level.isClientSide()) {
                    ItemStack planted = held.split(1);
                    items.set(SLOT_SAPLING, planted);
                    growthTicks = 0;
                }
                this.level.playSound(player, this.worldPosition, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F,
                        1.0F);
                setChanged();
                syncClient();
                return ItemInteractionResult.sidedSuccess(this.level.isClientSide());
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public boolean hasSapling() {
        return !items.get(SLOT_SAPLING).isEmpty();
    }

    public boolean hasSoil() {
        return !items.get(SLOT_SOIL).isEmpty();
    }

    public boolean hasOutputs() {
        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_COUNT; i++) {
            if (!items.get(i).isEmpty())
                return true;
        }
        return false;
    }

    public void advanceGrowth(int ticks) {
        if (!hasSapling())
            return;
        this.growthTicks = Math.min(this.maxGrowthTicks, this.growthTicks + ticks);
        setChanged();
        syncClient();
    }

    public static boolean isSoilItem(ItemStack stack) {
        return stack.is(Items.DIRT) || stack.is(Items.COARSE_DIRT) || stack.is(Items.PODZOL)
                || stack.is(Items.ROOTED_DIRT) || stack.is(Items.MOSS_BLOCK)
                || stack.is(ObsidiumMod.VOLCANIC_ASH);
    }

    public static boolean isSaplingItem(ItemStack stack) {
        return stack.is(ItemTags.SAPLINGS)
                || stack.getItem().getDescriptionId().contains("sapling")
                || stack.is(ObsidiumMod.ARES_IRON_SAPLING.asItem())
                || stack.is(ObsidiumMod.MIDAS_GOLD_SAPLING.asItem())
                || stack.is(ObsidiumMod.HERMES_COPPER_SAPLING.asItem())
                || stack.is(ObsidiumMod.HADES_COAL_SAPLING.asItem())
                || stack.is(ObsidiumMod.POSEIDON_LAPIS_SAPLING.asItem())
                || stack.is(ObsidiumMod.ASTRAPE_REDSTONE_SAPLING.asItem())
                || stack.is(ObsidiumMod.APOLLO_QUARTZ_SAPLING.asItem())
                || stack.is(ObsidiumMod.DIONYSIAN_AMETHYST_SAPLING.asItem())
                || stack.is(ObsidiumMod.PLOUTON_EMERALD_SAPLING.asItem())
                || stack.is(ObsidiumMod.ELECTRUM_SAPLING.asItem())
                || stack.is(ObsidiumMod.PYRETIC_TITAN_SAPLING_ITEM)
                || stack.is(ObsidiumMod.ADAMANTINE_DIAMOND_SAPLING.asItem())
                || stack.is(ObsidiumMod.TARTAREAN_NETHERITE_SAPLING.asItem());
    }

    public void dropAllInventory() {
        if (this.level != null && !this.level.isClientSide()) {
            for (ItemStack item : items) {
                if (!item.isEmpty()) {
                    Block.popResource(this.level, this.worldPosition, item);
                }
            }
        }
    }

    public void syncClient() {
        if (this.level instanceof ServerLevel sl) {
            sl.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("GrowthTicks", growthTicks);
        tag.putInt("MaxGrowthTicks", maxGrowthTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.growthTicks = tag.getInt("GrowthTicks");
        this.maxGrowthTicks = tag.getInt("MaxGrowthTicks");
        if (this.maxGrowthTicks < BASE_GROWTH_TICKS)
            this.maxGrowthTicks = BASE_GROWTH_TICKS;
    }

    // WorldlyContainer implementation for automation & hoppers
    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[] { OUTPUT_START, OUTPUT_START + 1, OUTPUT_START + 2, OUTPUT_START + 3, OUTPUT_START + 4 };
        } else {
            return new int[] { SLOT_SOIL, SLOT_SAPLING };
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (slot == SLOT_SOIL)
            return isSoilItem(stack);
        if (slot == SLOT_SAPLING)
            return isSaplingItem(stack);
        return false;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && slot >= OUTPUT_START;
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack res = ContainerHelper.removeItem(this.items, slot, amount);
        if (!res.isEmpty())
            setChanged();
        return res;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }
}
