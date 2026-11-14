package net.valentin.obsidium.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.valentin.obsidium.ObsidiumMod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModTooltips {
	private static final Map<Item, List<Component>> STATIC_TOOLTIPS = new HashMap<>();

	private ModTooltips() {}

	public static void register() {
		initTooltips();

		ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
			// Append Divine Pact tooltip if any weapon/tool/armor holds an Olympian Pact
			net.valentin.obsidium.mythology.DivinePactHelper.appendPactTooltip(stack, lines);

			Item item = stack.getItem();

			// Dynamic tooltip for Shulker-style dismantled Volcanite Modular Chest
			if (item == ObsidiumMod.VOLCANITE_MODULAR_CHEST_ITEM) {
				lines.add(Component.literal("▸ 54–216 Slots (Up to 4 Pages with Stack/Storage/Barrel Modules)").withStyle(ChatFormatting.GOLD));
				lines.add(Component.literal("▸ 4 Universal Flex Module Bays (Vacuum, Compactor, Void, Filter, Logistics)").withStyle(ChatFormatting.GRAY));
				lines.add(Component.literal("▸ Shift + Right-Click with Wrench to pick up with contents intact").withStyle(ChatFormatting.DARK_GRAY));

				CustomData beData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
				if (beData != null) {
					CompoundTag tag = beData.copyTag();
					if (tag.contains("Items")) {
						int savedStacks = tag.getList("Items", 10).size();
						lines.add(Component.literal("📦 Stored Stacks Inside: " + savedStacks).withStyle(ChatFormatting.GREEN));
					}
				}
				return;
			}

			List<Component> extra = STATIC_TOOLTIPS.get(item);
			if (extra != null) {
				lines.addAll(extra);
			}
		});
	}

	private static void initTooltips() {
		add(ObsidiumMod.VOLCANITE_CHEST_ITEM,
			Component.literal("▸ 54-Slot Blast-Resistant Obsidian Chest").withStyle(ChatFormatting.GRAY),
			Component.literal("▸ Press [R] inside to sort items by quantity").withStyle(ChatFormatting.DARK_GRAY));

		add(ObsidiumMod.VOLCANITE_WORKTABLE_ITEM,
			Component.literal("▸ 3x3 Crafting Station with 18-Slot Internal Buffer & Blueprint Memory").withStyle(ChatFormatting.GRAY));

		add(ObsidiumMod.VOLCANITE_SORTING_STATION_ITEM,
			Component.literal("▸ Automated Multi-Channel Item Router & Categorizer").withStyle(ChatFormatting.GOLD));

		add(ObsidiumMod.VOLCANITE_STORAGE_CONTROLLER_ITEM,
			Component.literal("▸ Central Brain of the Volcanite Digital Storage Network").withStyle(ChatFormatting.GOLD));

		add(ObsidiumMod.VOLCANITE_STORAGE_INTERFACE_ITEM,
			Component.literal("▸ Crafting & Search Terminal for the Volcanite Storage Network").withStyle(ChatFormatting.GRAY));

		add(ObsidiumMod.VOLCANITE_STORAGE_DISK_DRIVE_ITEM,
			Component.literal("▸ Holds up to 8 Volcanite Item or Liquid Storage Disks").withStyle(ChatFormatting.GRAY));

		add(ObsidiumMod.VOLCANITE_STORAGE_UNIT_ITEM,
			Component.literal("▸ High-Density Network Storage Buffer Unit").withStyle(ChatFormatting.GRAY));

		add(ObsidiumMod.VOLCANITE_STORAGE_PIPE_ITEM,
			Component.literal("▸ Transfers Items, Liquids & RF Energy when upgraded").withStyle(ChatFormatting.GRAY),
			Component.literal("▸ Conceal with Universal Pipe Facade").withStyle(ChatFormatting.DARK_GRAY));

		add(ObsidiumMod.HERMES_ITEM_PIPE_ITEM,
			Component.literal("▸ High-Speed Dedicated Item Logistics Pipe (2x Transfer Rate)").withStyle(ChatFormatting.GOLD),
			Component.literal("▸ Supports Hermes Filter & Stack/Overclock Modules").withStyle(ChatFormatting.GRAY));

		add(ObsidiumMod.NAIAD_FLUID_PIPE_ITEM,
			Component.literal("▸ Pressurized Fluid Conduit for Lava, Water, Steam & Distillate").withStyle(ChatFormatting.AQUA));

		add(ObsidiumMod.VOLCANITE_UNIVERSAL_CONDUIT_ITEM,
			Component.literal("▸ All-in-One Item, Fluid & RF Energy Conduit").withStyle(ChatFormatting.LIGHT_PURPLE));

		add(ObsidiumMod.VOLCANITE_STORAGE_PIPE_ITEM,
			Component.literal("▸ High-Speed Fiber-Optic Data Cable for Volcanite Storage Network").withStyle(ChatFormatting.AQUA),
			Component.literal("▸ Attach Import or Export Connectors to link chests & machines").withStyle(ChatFormatting.GRAY),
			Component.literal("▸ Data-Only: Does not transmit RF energy").withStyle(ChatFormatting.DARK_GRAY));

		add(ObsidiumMod.VOLCANITE_CRAFTER_INTERFACE_ITEM,
			Component.literal("▸ Interactive 3x3 Network Crafting Terminal & 4-Bay Auto-Crafter").withStyle(ChatFormatting.GOLD),
			Component.literal("▸ Automatically pulls ingredients from & stocks outputs into the Network").withStyle(ChatFormatting.GRAY));

		add(ObsidiumMod.VOLCANITE_NETWORK_NODE_EMITTER_ITEM,
			Component.literal("▸ Wireless Network Signal Emitter (Link with Supreme Link Tool)").withStyle(ChatFormatting.GOLD));

		add(ObsidiumMod.VOLCANITE_NETWORK_NODE_RECEIVER_ITEM,
			Component.literal("▸ Wireless Network Signal Receiver").withStyle(ChatFormatting.GOLD));

		add(ObsidiumMod.VOLCANITE_CHUNK_LOADER_ITEM,
			Component.literal("▸ Keeps the current 3x3 Chunks loaded for machines & pipes").withStyle(ChatFormatting.AQUA));

		add(ObsidiumMod.DEMETRIAN_SAPLING_BARREL_ITEM,
			Component.literal("▸ 3-Chamber Nursery Cache (4,096 saplings/chamber = 12,288 total)").withStyle(ChatFormatting.GREEN),
			Component.literal("▸ Right-Click to deposit / Left-Click to extract").withStyle(ChatFormatting.GRAY));

		add(ObsidiumMod.DEMETRIAN_LUMBER_DEPOT_ITEM,
			Component.literal("▸ 4-Chamber Heavy Timber Depot (4,096 logs/chamber = 16,384 total)").withStyle(ChatFormatting.GOLD),
			Component.literal("▸ Right-Click with logs to deposit / Left-Click to extract 1").withStyle(ChatFormatting.GRAY),
			Component.literal("▸ Shift + Left-Click to extract a full 64-stack").withStyle(ChatFormatting.DARK_GRAY));

		add(ObsidiumMod.TIMBER_BUNDLE_ITEM,
			Component.literal("▸ Compressed Timber Bundle (9 Tree Trunks/Logs)").withStyle(ChatFormatting.YELLOW),
			Component.literal("▸ Can be crafted back into 9 logs anytime").withStyle(ChatFormatting.GRAY));

		add(ObsidiumMod.FOLIAGE_BALE_ITEM,
			Component.literal("▸ Compressed Foliage Biomass Bale (9 Leaves)").withStyle(ChatFormatting.GREEN),
			Component.literal("▸ Supercharged fuel for Demetrian Biomass Generator (+20% synergy)").withStyle(ChatFormatting.YELLOW),
			Component.literal("▸ Right-Click directly onto generator to insert").withStyle(ChatFormatting.GRAY));

		add(ObsidiumMod.DEMETRIAN_BONSAI_PLANTER_ITEM,
			Component.literal("▸ Sacred Miniature Tree Cultivator").withStyle(ChatFormatting.DARK_GREEN),
			Component.literal("▸ Plant any sapling (mineral or vanilla) to automatically harvest logs & ore drops").withStyle(ChatFormatting.GRAY),
			Component.literal("▸ Accelerate with Volcanic Ash, Bonemeal, or Volcanic Honey!").withStyle(ChatFormatting.GOLD));

		add(ObsidiumMod.VOLCANIC_BEEHIVE_ITEM,
			Component.literal("▸ Fiery Volcanic Bee Sanctuary").withStyle(ChatFormatting.GOLD),
			Component.literal("▸ Houses up to 3 Volcanic Bees. Shear for combs or bottle for glowing honey").withStyle(ChatFormatting.GRAY),
			Component.literal("▸ Use campfire smoke to harvest peacefully without angering the swarm!").withStyle(ChatFormatting.RED));

		add(ObsidiumMod.VOLCANIC_HONEYCOMB,
			Component.literal("▸ Fiery Crystallized Honeycomb").withStyle(ChatFormatting.GOLD),
			Component.literal("▸ Harvested from Volcanic Beehives using shears").withStyle(ChatFormatting.GRAY),
			Component.literal("▸ Used to craft beehives, fire-resistant waxes, and alchemical catalysts").withStyle(ChatFormatting.YELLOW));

		add(ObsidiumMod.VOLCANIC_HONEY_BOTTLE,
			Component.literal("▸ Golden Volcanic Ambrosia").withStyle(ChatFormatting.GOLD),
			Component.literal("▸ Drinking: Cleanses debuffs, grants Fire Resistance (5m) & Haste II (3m)").withStyle(ChatFormatting.AQUA),
			Component.literal("▸ On Plants: Instantly completes Bonsai harvest cycle with 2x yield!").withStyle(ChatFormatting.GREEN));

		add(ObsidiumMod.POLLINATING_BRUSH,
			Component.literal("▸ Sacred Pollinating Brush").withStyle(ChatFormatting.GREEN),
			Component.literal("▸ Right-click leaves or hives to collect Sacred Pollen (up to 8 charges)").withStyle(ChatFormatting.GRAY),
			Component.literal("▸ Right-click grafted saplings to trigger instant metamorphosis!").withStyle(ChatFormatting.GOLD));
	}

	private static void add(Item item, Component... lines) {
		STATIC_TOOLTIPS.put(item, List.of(lines));
	}
}
