package net.valentin.obsidium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.block.VolcaniteModularChestBlockEntity;
import net.valentin.obsidium.storage.QuantumLinkNetworkState;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Fabric-only regression checks; staged separately from the production mod. */
public final class CompactorRuntimeChecks implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                server.tickRateManager().setFrozen(true);
                run(server);
                System.out.println("COMPACTOR_ALL_PASSED");
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.out.println("COMPACTOR_FAILED");
            } finally {
                server.halt(false);
            }
        });
    }

    private static void run(MinecraftServer server) throws Exception {
        ServerLevel level = server.overworld();
        Method compact = VolcaniteModularChestBlockEntity.class.getDeclaredMethod("runAutoCompaction", Level.class);
        compact.setAccessible(true);
        var chest = chest(level, new BlockPos(36, 100, 36));
        fill(chest, Items.GOLD_NUGGET, Items.GOLD_INGOT, 63);
        List<ItemStack> original = snapshot(chest);
        compact.invoke(chest, level);
        check(same(chest, original), "PARTIAL_OUTPUT_ROLLS_BACK_BOTH_SIDES");
        for (int i = 0; i < 20; i++) compact.invoke(chest, level);
        check(same(chest, original), "REPEATED_FAILURE_DOES_NOT_ACCUMULATE_OUTPUT");

        ItemStack named = new ItemStack(Items.GOLD_NUGGET, 64);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Original ingredient"));
        chest.setItem(0, named);
        original = snapshot(chest);
        compact.invoke(chest, level);
        check(same(chest, original), "ROLLBACK_PRESERVES_INGREDIENT_COMPONENTS");

        fill(chest, Items.GOLD_NUGGET, Items.BARRIER, 64);
        original = snapshot(chest);
        compact.invoke(chest, level);
        check(same(chest, original), "FULL_OUTPUT_ROLLS_BACK");

        fill(chest, Items.GOLD_NUGGET, Items.GOLD_INGOT, 60);
        compact.invoke(chest, level);
        check(chest.getItem(0).getCount() == 28 && chest.getItem(1).getCount() == 64,
                "SUCCESSFUL_3X3_BATCH_CONSUMES_EXACTLY_36_NUGGETS");

        fill(chest, Items.QUARTZ, Items.QUARTZ_BLOCK, 63);
        original = snapshot(chest);
        compact.invoke(chest, level);
        check(same(chest, original), "PARTIAL_2X2_OUTPUT_ROLLS_BACK");
        fill(chest, Items.QUARTZ, Items.QUARTZ_BLOCK, 60);
        compact.invoke(chest, level);
        check(chest.getItem(0).getCount() == 48 && chest.getItem(1).getCount() == 64,
                "SUCCESSFUL_2X2_BATCH_CONSUMES_EXACTLY_16_QUARTZ");

        fill(chest, Items.GOLD_NUGGET, Items.BARRIER, 64);
        chest.setItem(0, new ItemStack(Items.GOLD_NUGGET, 36));
        compact.invoke(chest, level);
        check(chest.getItem(0).is(Items.GOLD_INGOT) && chest.getItem(0).getCount() == 4,
                "SPACE_FREED_BY_INGREDIENTS_ACCEPTS_RESULT");

        var linked = chest(level, new BlockPos(44, 100, 36));
        linked.setQuantumChannel(811);
        linked.setItem(218, new ItemStack(ObsidiumMod.QUANTUM_LINK_MODULE));
        var peer = chest(level, new BlockPos(48, 100, 36));
        peer.setQuantumChannel(811);
        peer.setItem(218, new ItemStack(ObsidiumMod.QUANTUM_LINK_MODULE));
        fill(linked, Items.GOLD_NUGGET, Items.GOLD_INGOT, 63);
        original = snapshot(linked);
        compact.invoke(linked, level);
        check(same(linked, original) && peer.getItem(0).getCount() == 64 && peer.getItem(1).getCount() == 63,
                "QUANTUM_PEER_SEES_ROLLED_BACK_CONTENTS");
        fill(linked, Items.GOLD_NUGGET, Items.GOLD_INGOT, 60);
        QuantumLinkNetworkState.get(server).setDirty(false);
        compact.invoke(linked, level);
        check(peer.getItem(0).getCount() == 28 && peer.getItem(1).getCount() == 64,
                "QUANTUM_PEER_SEES_COMMITTED_COMPACTION");
        check(QuantumLinkNetworkState.get(server).isDirty(), "QUANTUM_RESULT_MARKED_FOR_SAVING");

        BlockPos faultPos = new BlockPos(56, 100, 36);
        level.setBlock(faultPos, ObsidiumMod.VOLCANITE_MODULAR_CHEST_BLOCK.defaultBlockState(), 3);
        var fault = new ThrowAfterInsertion(faultPos, level.getBlockState(faultPos));
        level.setBlockEntity(fault);
        fault.setItem(216, new ItemStack(ObsidiumMod.VOLCANITE_COMPACTOR_UPGRADE));
        fault.setItem(217, new ItemStack(ObsidiumMod.ADVANCED_VOLCANITE_OVERCLOCK_UPGRADE));
        fill(fault, Items.QUARTZ, Items.QUARTZ_BLOCK, 60);
        original = snapshot(fault);
        try {
            compact.invoke(fault, level);
            throw new AssertionError("Expected insertion failure");
        } catch (InvocationTargetException expected) {
            check(expected.getCause() instanceof IllegalStateException && same(fault, original),
                    "INSERTION_EXCEPTION_RESTORES_COMPLETE_INVENTORY");
        }
    }

    private static VolcaniteModularChestBlockEntity chest(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ObsidiumMod.VOLCANITE_MODULAR_CHEST_BLOCK.defaultBlockState(), 3);
        var chest = (VolcaniteModularChestBlockEntity) level.getBlockEntity(pos);
        chest.setItem(216, new ItemStack(ObsidiumMod.VOLCANITE_COMPACTOR_UPGRADE));
        chest.setItem(217, new ItemStack(ObsidiumMod.ADVANCED_VOLCANITE_OVERCLOCK_UPGRADE));
        return chest;
    }

    private static void fill(VolcaniteModularChestBlockEntity chest, Item ingredient, Item output, int outputCount) {
        for (int i = 0; i < 54; i++) chest.setItem(i, new ItemStack(Items.BARRIER, 64));
        chest.setItem(0, new ItemStack(ingredient, 64));
        chest.setItem(1, new ItemStack(output, outputCount));
    }

    private static List<ItemStack> snapshot(VolcaniteModularChestBlockEntity chest) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < chest.getContainerSize(); i++) items.add(chest.getItem(i).copy());
        return items;
    }

    private static boolean same(VolcaniteModularChestBlockEntity chest, List<ItemStack> items) {
        for (int i = 0; i < items.size(); i++) if (!ItemStack.matches(chest.getItem(i), items.get(i))) return false;
        return true;
    }

    private static void check(boolean passed, String name) {
        if (!passed) throw new AssertionError(name);
        System.out.println("COMPACTOR_OK_" + name);
    }

    private static final class ThrowAfterInsertion extends VolcaniteModularChestBlockEntity {
        ThrowAfterInsertion(BlockPos pos, BlockState state) { super(pos, state); }
        @Override
        public ItemStack insertStorageItem(ItemStack stack) {
            super.insertStorageItem(stack);
            throw new IllegalStateException("Simulated insertion failure");
        }
    }
}
