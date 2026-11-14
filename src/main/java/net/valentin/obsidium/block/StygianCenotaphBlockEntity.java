package net.valentin.obsidium.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ContainerHelper;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.item.StygianKeyItem;

import java.util.UUID;

public class StygianCenotaphBlockEntity extends BlockEntity {

    public static final int INVENTORY_SIZE = 50;
    private NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private UUID ownerUUID = null;
    private String ownerName = "";
    private int storedXp = 0;
    private long deathTime = 0;

    public StygianCenotaphBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.STYGIAN_CENOTAPH_BE, pos, state);
    }

    public void initializeGrave(Player player, NonNullList<ItemStack> capturedItems, int xp) {
        this.ownerUUID = player.getUUID();
        this.ownerName = player.getName().getString();
        this.storedXp = xp;
        this.deathTime = System.currentTimeMillis();

        for (int i = 0; i < capturedItems.size() && i < this.items.size(); i++) {
            this.items.set(i, capturedItems.get(i));
        }
        setChanged();
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean tryOpen(Player player) {
        if (level == null || level.isClientSide()) return false;

        boolean isCreative = player.isCreative();
        boolean isAdmin = player.hasPermissions(2);
        boolean isOwner = this.ownerUUID != null && this.ownerUUID.equals(player.getUUID());
        
        ItemStack matchingKey = findMatchingKey(player);
        boolean hasKey = matchingKey != null;

        // Security restriction: ONLY keyholder, creative, or admin can access/destroy
        if (!isCreative && !isAdmin && (!isOwner || !hasKey)) {
            player.sendSystemMessage(Component.literal("§c[Stygian Cenotaph] This grave belongs to §e" + (ownerName.isEmpty() ? "another player" : ownerName) + "§c!").withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("§7Only the holder of the matching Stygian Key or an administrator may open it.").withStyle(ChatFormatting.GRAY));
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5, 15, 0.2, 0.2, 0.2, 0.02);
            }
            return false;
        }

        // Consume the key if not admin/creative
        if (!isCreative && !isAdmin && matchingKey != null) {
            matchingKey.shrink(1);
        }

        // 1. Restore Armor (Slots 36..39: Helmet, Chestplate, Leggings, Boots)
        restoreEquipmentSlot(player, EquipmentSlot.HEAD, 36);
        restoreEquipmentSlot(player, EquipmentSlot.CHEST, 37);
        restoreEquipmentSlot(player, EquipmentSlot.LEGS, 38);
        restoreEquipmentSlot(player, EquipmentSlot.FEET, 39);

        // 2. Restore Offhand (Slot 40)
        restoreEquipmentSlot(player, EquipmentSlot.OFFHAND, 40);

        // 3. Restore Main Inventory (Slots 0..35 and remaining)
        for (int i = 0; i < this.items.size(); i++) {
            if (i >= 36 && i <= 40) continue; // Already handled armor and offhand
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                    Containers.dropItemStack(level, player.getX(), player.getY() + 0.5, player.getZ(), stack);
                }
                this.items.set(i, ItemStack.EMPTY);
            }
        }

        // 4. Restore XP
        if (this.storedXp > 0) {
            player.giveExperiencePoints(this.storedXp);
            this.storedXp = 0;
        }

        // 5. Sound & VFX
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 30, 0.3, 0.5, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5, 20, 0.2, 0.4, 0.2, 0.08);
            sl.playSound(null, worldPosition, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
            sl.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        player.sendSystemMessage(Component.literal("§a[Stygian Cenotaph] Your soul and belongings have been restored!").withStyle(ChatFormatting.GREEN));

        // 6. Dissolve Cenotaph
        level.removeBlock(worldPosition, false);
        return true;
    }

    private void restoreEquipmentSlot(Player player, EquipmentSlot slot, int inventoryIndex) {
        ItemStack stack = this.items.get(inventoryIndex);
        if (!stack.isEmpty()) {
            if (player.getItemBySlot(slot).isEmpty()) {
                player.setItemSlot(slot, stack.copy());
            } else if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                Containers.dropItemStack(level, player.getX(), player.getY() + 0.5, player.getZ(), stack);
            }
            this.items.set(inventoryIndex, ItemStack.EMPTY);
        }
    }

    private ItemStack findMatchingKey(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ObsidiumMod.STYGIAN_KEY)) {
                BlockPos targetPos = StygianKeyItem.getGravePos(stack);
                if (targetPos != null && targetPos.equals(this.worldPosition)) {
                    return stack;
                }
            }
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        if (ownerUUID != null) tag.putUUID("OwnerUUID", ownerUUID);
        tag.putString("OwnerName", ownerName);
        tag.putInt("StoredXp", storedXp);
        tag.putLong("DeathTime", deathTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        if (tag.hasUUID("OwnerUUID")) this.ownerUUID = tag.getUUID("OwnerUUID");
        this.ownerName = tag.getString("OwnerName");
        this.storedXp = tag.getInt("StoredXp");
        this.deathTime = tag.getLong("DeathTime");
    }
}

