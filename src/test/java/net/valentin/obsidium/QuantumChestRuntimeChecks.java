package net.valentin.obsidium;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.valentin.obsidium.block.VolcaniteModularChestBlockEntity;
import net.valentin.obsidium.block.VolcaniteModularChestMenu;
import net.valentin.obsidium.storage.QuantumLinkNetworkState;

import java.util.HashMap;
import java.util.UUID;

/** Fabric integration entrypoint, packaged only as a separate test mod in a temporary server. */
public final class QuantumChestRuntimeChecks implements ModInitializer {
    private static final java.util.List<String> messages = new java.util.ArrayList<>();
    @Override
    public void onInitialize() {
        // Item entities become queryable only once their chunk is entity-ticking.
        // Load the fixture chunks before asserting the drops from physical removal.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            server.overworld().setChunkForced(2, 2, true);
            server.overworld().setChunkForced(3, 2, true);
        });
        int[] warmupTicks = {0};
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++warmupTicks[0] != 20) return;
            try {
                server.tickRateManager().setFrozen(true);
                run(server);
                System.out.println("QUANTUM_CHEST_ALL_PASSED");
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.out.println("QUANTUM_CHEST_FAILED");
            } finally {
                server.halt(false);
            }
        });
    }

    private static void run(MinecraftServer server) throws Exception {
        ServerLevel level = server.overworld();
        QuantumLinkNetworkState network = QuantumLinkNetworkState.get(server);
        VolcaniteModularChestBlockEntity chest = chest(level, new BlockPos(36, 100, 36), 801);
        check(chest.canInstallQuantumLink(), "EMPTY_CHEST_ACCEPTS_LINK");
        chest.setItem(216, new ItemStack(ObsidiumMod.QUANTUM_LINK_MODULE));
        VolcaniteModularChestBlockEntity peer = chest(level, new BlockPos(40, 100, 36), 801);
        peer.setItem(216, new ItemStack(ObsidiumMod.QUANTUM_LINK_MODULE));
        check(chest.insertItem(new ItemStack(Items.DIAMOND, 10), false).isEmpty()
                && peer.getItem(0).getCount() == 10, "AUTOMATION_AND_PEER_SEE_SAME_ITEMS");
        network.setDirty(false);
        check(chest.insertItem(new ItemStack(Items.IRON_INGOT, 5), true).isEmpty()
                && chest.getItem(1).isEmpty() && !network.isDirty(), "SIMULATION_HAS_NO_MUTATIONS");
        chest.insertItem(new ItemStack(Items.IRON_INGOT, 5), false);
        HashMap<net.minecraft.world.item.Item, Integer> totals = new HashMap<>();
        chest.collectStoredItems(totals);
        check(totals.getOrDefault(Items.DIAMOND, 0) == 10 && totals.getOrDefault(Items.IRON_INGOT, 0) == 5,
                "NETWORK_COUNTS_ACTIVE_VAULT");
        check(chest.extractMatchingItem(new ItemStack(Items.DIAMOND), 2, false).getCount() == 2
                && peer.getItem(0).getCount() == 8, "EXACT_EXTRACTION_UPDATES_PEER");
        chest.sortAlphabetically();
        chest.sortByQuantity(true);
        check(chest.getTotalStored() == 13 && peer.getTotalStored() == 13, "SORT_PRESERVES_SHARED_CONTENTS");

        ServerPlayer player = player(server, level, chest.getBlockPos());
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.BARRIER, 64));
        player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 63));
        chest.setItem(0, new ItemStack(Items.DIAMOND, 10));
        level.getDataStorage().save();
        VolcaniteModularChestMenu menu = new VolcaniteModularChestMenu(1, player.getInventory(), chest);
        menu.quickMoveStack(player, 0);
        check(chest.getItem(0).getCount() == 9 && network.isDirty(), "PARTIAL_SHIFT_WITHDRAWAL_MARKS_SAVED_DATA");
        level.getDataStorage().save();
        check(savedCount(server, chest.getActiveQuantumKey()) == 9, "PARTIAL_WITHDRAWAL_PERSISTS_ON_DISK");
        player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 5));
        network.setDirty(false);
        menu.quickMoveStack(player, VolcaniteModularChestMenu.MENU_MOD_SLOT_END);
        check(chest.getItem(0).getCount() == 14 && peer.getItem(0).getCount() == 14 && network.isDirty(),
                "SHIFT_INSERT_MERGE_MARKS_SAVED_DATA");
        level.getDataStorage().save();
        check(savedCount(server, chest.getActiveQuantumKey()) == 14, "MERGED_INSERTION_PERSISTS_ON_DISK");

        VolcaniteModularChestBlockEntity local = chest(level, new BlockPos(48, 100, 36), 802);
        local.setItem(0, new ItemStack(Items.EMERALD, 3));
        VolcaniteModularChestMenu localMenu = new VolcaniteModularChestMenu(2, player.getInventory(), local);
        check(!local.canInstallQuantumLink() && !localMenu.slots.get(216).mayPlace(new ItemStack(ObsidiumMod.QUANTUM_LINK_MODULE)),
                "NONEMPTY_LOCAL_CHEST_REJECTS_LINK_MODULE");
        local.setItem(0, ItemStack.EMPTY);
        local.setItem(100, new ItemStack(Items.EMERALD));
        check(!local.canInstallQuantumLink(), "LOCKED_LOCAL_PAGES_ALSO_PROTECTED");
        local.setItem(100, ItemStack.EMPTY);
        check(local.canInstallQuantumLink(), "EMPTYING_LOCAL_CONTENTS_ALLOWS_INSTALL");

        // Exercise actual menu interactions, rather than bypassing validation with setItem.
        localMenu.setCarried(new ItemStack(ObsidiumMod.QUANTUM_LINK_MODULE));
        localMenu.clicked(216, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        check(local.hasQuantumLinkModule() && localMenu.getCarried().isEmpty(), "PICKUP_INSTALLS_LINK_IN_EMPTY_CHEST");
        localMenu.clicked(216, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        check(!local.hasQuantumLinkModule() && localMenu.getCarried().is(ObsidiumMod.QUANTUM_LINK_MODULE),
                "PICKUP_REMOVES_LINK_MODULE");
        localMenu.setCarried(ItemStack.EMPTY);
        local.setItem(0, new ItemStack(Items.EMERALD, 3));
        player.getInventory().setItem(9, new ItemStack(ObsidiumMod.QUANTUM_LINK_MODULE));
        localMenu.quickMoveStack(player, VolcaniteModularChestMenu.MENU_MOD_SLOT_END);
        check(!local.hasQuantumLinkModule() && player.getInventory().getItem(9).is(ObsidiumMod.QUANTUM_LINK_MODULE)
                && local.getTotalStored() == 3, "REJECTED_SHIFT_LINK_STAYS_IN_PLAYER_INVENTORY");
        localMenu.setCarried(new ItemStack(ObsidiumMod.QUANTUM_LINK_MODULE));
        localMenu.clicked(216, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        check(!local.hasQuantumLinkModule() && localMenu.getCarried().is(ObsidiumMod.QUANTUM_LINK_MODULE),
                "REJECTED_PICKUP_PRESERVES_MODULE_AND_CONTENTS");
        check(messages.size() == 2 && messages.stream().allMatch(message -> message.contains("Empty all local")),
                "REJECTED_INSTALL_EXPLAINS_EMPTY_STORAGE_REQUIREMENT");
        localMenu.setCarried(ItemStack.EMPTY);
        local.setItem(0, ItemStack.EMPTY);
        localMenu.quickMoveStack(player, VolcaniteModularChestMenu.MENU_MOD_SLOT_END);
        check(local.hasQuantumLinkModule() && player.getInventory().getItem(9).isEmpty(),
                "SHIFT_INSTALLS_LINK_AFTER_EMPTYING_CHEST");
        local.setItem(216, ItemStack.EMPTY);

        // Recreate a pre-fix save with hidden local items via the low-level setter.
        local.setItem(0, new ItemStack(Items.EMERALD, 3));
        local.setItem(216, new ItemStack(ObsidiumMod.QUANTUM_LINK_MODULE));
        local.setItem(0, new ItemStack(Items.DIAMOND, 7));
        String legacyKey = local.getActiveQuantumKey();
        level.setBlock(local.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
        int emeralds = level.getEntitiesOfClass(ItemEntity.class, new AABB(48, 100, 36, 49, 101, 37).inflate(2)).stream()
                .filter(entity -> entity.getItem().is(Items.EMERALD)).mapToInt(entity -> entity.getItem().getCount()).sum();
        check(emeralds == 3, "BREAK_RECOVERS_LEGACY_HIDDEN_LOCAL_ITEMS");
        check(network.getItemVault(legacyKey).get(0).getCount() == 7, "BREAK_PRESERVES_SHARED_VAULT");

        ItemStack packed = new ItemStack(ObsidiumMod.VOLCANITE_MODULAR_CHEST_ITEM);
        chest.saveToItem(packed, server.registryAccess());
        chest.clearLocalContents();
        level.removeBlock(chest.getBlockPos(), false);
        check(peer.getItem(0).getCount() == 14, "DISMANTLE_PRESERVES_PEER_CONTENTS");
        check(packed.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA), "DISMANTLE_SERIALIZES_PHYSICAL_CHEST");
    }

    private static int savedCount(MinecraftServer server, String key) throws Exception {
        var data = NbtIo.readCompressed(server.getWorldPath(LevelResource.ROOT)
                .resolve("data/obsidium_quantum_link_network.dat"), NbtAccounter.unlimitedHeap());
        return QuantumLinkNetworkState.load(data.getCompound("data"), server.registryAccess()).getItemVault(key).get(0).getCount();
    }

    private static VolcaniteModularChestBlockEntity chest(ServerLevel level, BlockPos pos, int channel) {
        level.setBlock(pos, ObsidiumMod.VOLCANITE_MODULAR_CHEST_BLOCK.defaultBlockState(), 3);
        var chest = (VolcaniteModularChestBlockEntity) level.getBlockEntity(pos);
        chest.setQuantumChannel(channel);
        return chest;
    }

    private static ServerPlayer player(MinecraftServer server, ServerLevel level, BlockPos pos) {
        var player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "QuantumChestTest"), ClientInformation.createDefault()) {
            @Override public void displayClientMessage(net.minecraft.network.chat.Component message, boolean overlay) {
                messages.add(message.getString());
            }
        };
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return player;
    }

    private static void check(boolean success, String name) {
        if (!success) throw new AssertionError(name);
        System.out.println("QUANTUM_CHEST_OK_" + name);
    }
}
