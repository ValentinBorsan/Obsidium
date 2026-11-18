package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;
import org.jetbrains.annotations.Nullable;

public class GraftedSaplingBlockEntity extends BlockEntity {
    private ItemStack rootstockStack = new ItemStack(Items.OAK_SAPLING);
    private String rootstockSpecies = "oak";
    private ItemStack scionStack = ItemStack.EMPTY;
    private String scionSpecies = "";
    private GraftedSaplingBlock.Stage stage = GraftedSaplingBlock.Stage.INCISED;

    public GraftedSaplingBlockEntity(BlockPos pos, BlockState state) {
        super(ObsidiumMod.GRAFTED_SAPLING_BE, pos, state);
    }

    public void initRootstock(ItemStack rootstock, String species) {
        this.rootstockStack = rootstock.copy();
        this.rootstockStack.setCount(1);
        this.rootstockSpecies = species != null ? species : "oak";
        this.stage = GraftedSaplingBlock.Stage.INCISED;
        setChanged();
    }

    public void applyScion(ItemStack scion, String species) {
        this.scionStack = scion.copy();
        this.scionStack.setCount(1);
        this.scionSpecies = species != null ? species : "oak";
        this.stage = GraftedSaplingBlock.Stage.GRAFTED;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.setBlock(worldPosition, getBlockState().setValue(GraftedSaplingBlock.STAGE, GraftedSaplingBlock.Stage.GRAFTED), 3);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ItemStack getRootstockStack() {
        return rootstockStack;
    }

    public String getRootstockSpecies() {
        return rootstockSpecies;
    }

    public ItemStack getScionStack() {
        return scionStack;
    }

    public String getScionSpecies() {
        return scionSpecies;
    }

    public GraftedSaplingBlock.Stage getStage() {
        return stage;
    }

    public boolean canMetamorphose() {
        return stage == GraftedSaplingBlock.Stage.GRAFTED;
    }

    public void forceMetamorphosis() {
        if (this.level instanceof ServerLevel sl && canMetamorphose()) {
            boolean isVolcanicOrHybrid = GraftingRegistry.canProduceRareMutation(rootstockSpecies, scionSpecies);

            Block hybridBlock;
            boolean isRareTitanMutation = isVolcanicOrHybrid && sl.random.nextFloat() < 0.05F;
            if (isRareTitanMutation) {
                hybridBlock = ObsidiumMod.PYRETIC_TITAN_SAPLING;
                sl.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.0F);
                sl.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.9F);
                sl.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5, 30, 0.35, 0.35, 0.35, 0.08);
                sl.sendParticles(ParticleTypes.DRAGON_BREATH, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5, 20, 0.25, 0.25, 0.25, 0.05);
            } else {
                hybridBlock = GraftingRegistry.resolveHybrid(rootstockSpecies, scionSpecies);
                sl.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.5F);
                sl.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
            }

            sl.setBlock(worldPosition, hybridBlock.defaultBlockState(), 3);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5, 25, 0.35, 0.35, 0.35, 0.05);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5, 15, 0.25, 0.25, 0.25, 0.08);
        }
    }

    public void dropGraftContents() {
        if (level == null || level.isClientSide()) return;
        if (!rootstockStack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, rootstockStack);
        }
        if (!scionStack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, scionStack);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!rootstockStack.isEmpty()) {
            tag.put("RootstockStack", rootstockStack.saveOptional(registries));
        }
        tag.putString("RootstockSpecies", rootstockSpecies);
        if (!scionStack.isEmpty()) {
            tag.put("ScionStack", scionStack.saveOptional(registries));
        }
        tag.putString("ScionSpecies", scionSpecies);
        tag.putString("Stage", stage.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("RootstockStack")) {
            rootstockStack = ItemStack.parseOptional(registries, tag.getCompound("RootstockStack"));
        }
        rootstockSpecies = tag.getString("RootstockSpecies");
        if (tag.contains("ScionStack")) {
            scionStack = ItemStack.parseOptional(registries, tag.getCompound("ScionStack"));
        } else {
            scionStack = ItemStack.EMPTY;
        }
        scionSpecies = tag.getString("ScionSpecies");
        if (tag.contains("Stage")) {
            try {
                stage = GraftedSaplingBlock.Stage.valueOf(tag.getString("Stage"));
            } catch (Exception ignored) {
                stage = GraftedSaplingBlock.Stage.INCISED;
            }
        }
    }

    @Nullable
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
}
