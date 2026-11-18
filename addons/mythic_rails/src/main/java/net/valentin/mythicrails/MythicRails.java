package net.valentin.mythicrails;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;

public final class MythicRails implements ModInitializer {
    public static final String MOD_ID = "mythic_rails";

    public static final EntityType<HermesFreightCart> FREIGHT_CART = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, id("hermes_freight_cart"),
            EntityType.Builder.<HermesFreightCart>of(HermesFreightCart::new, MobCategory.MISC)
                    .sized(0.98F, 0.7F).clientTrackingRange(8).build("mythic_rails:hermes_freight_cart"));
    public static final Item FREIGHT_CART_ITEM = Registry.register(BuiltInRegistries.ITEM,
            id("hermes_freight_cart"), new FreightCartItem(new Item.Properties().stacksTo(1)));

    public static final net.minecraft.world.inventory.MenuType<FreightCartMenu> FREIGHT_MENU = Registry.register(
            BuiltInRegistries.MENU, id("hermes_freight_cart"), new net.minecraft.world.inventory.MenuType<>(
                    FreightCartMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // Renderer-only display models; no separate item or world-generation entry.
    public static final net.minecraft.world.level.block.Block CARGO_DISPLAY = Registry.register(
            BuiltInRegistries.BLOCK, id("hermes_cargo_display"), new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3.5F)));
    public static final net.minecraft.world.level.block.Block ENERGY_CELL_DISPLAY = Registry.register(
            BuiltInRegistries.BLOCK, id("energy_cell_display"), new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3.5F)));

    public static final FreightStationBlock LOADING_STATION = station("hermes_loading_station");
    public static final FreightStationBlock UNLOADING_STATION = station("hermes_unloading_station");

    public static final RailUtilityStationBlock FLUID_LOADING_STATION = utilityStation(
            "hermes_fluid_loading_station", RailUtilityStationBlock.StationKind.FLUID_LOADER);
    public static final RailUtilityStationBlock FLUID_UNLOADING_STATION = utilityStation(
            "hermes_fluid_unloading_station", RailUtilityStationBlock.StationKind.FLUID_UNLOADER);
    public static final RailUtilityStationBlock ENERGY_LOADING_STATION = utilityStation(
            "hermes_energy_loading_station", RailUtilityStationBlock.StationKind.ENERGY_LOADER);
    public static final RailUtilityStationBlock ENERGY_UNLOADING_STATION = utilityStation(
            "hermes_energy_unloading_station", RailUtilityStationBlock.StationKind.ENERGY_UNLOADER);

    public static final net.minecraft.world.level.block.entity.BlockEntityType<FreightStationBlockEntity> STATION_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("freight_station"),
            net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    FreightStationBlockEntity::new, LOADING_STATION, UNLOADING_STATION).build(null));

    public static final net.minecraft.world.level.block.entity.BlockEntityType<RailUtilityStationBlockEntity> UTILITY_STATION_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("utility_station"),
            net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    RailUtilityStationBlockEntity::new,
                    FLUID_LOADING_STATION, FLUID_UNLOADING_STATION,
                    ENERGY_LOADING_STATION, ENERGY_UNLOADING_STATION).build(null));

    public static final ObsidianRailBlock OBSIDIAN_RAIL = block("obsidian_rail", new ObsidianRailBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.RAIL)
                    .strength(2.0F, 1200.0F)));
    public static final EmberPoweredRailBlock EMBER_RAIL = block("ember_powered_rail", new EmberPoweredRailBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.POWERED_RAIL)));
    public static final VolcaniteSpeedRailBlock VOLCANITE_SPEED_RAIL = block("volcanite_speed_rail", new VolcaniteSpeedRailBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.RAIL)));
    public static final ObsidianDetectorRailBlock OBSIDIAN_DETECTOR_RAIL = block("obsidian_detector_rail", new ObsidianDetectorRailBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DETECTOR_RAIL)));
    public static final SwitchRailBlock SWITCH_RAIL = block("switch_rail", new SwitchRailBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.RAIL)));
    public static final RailBumperBlock RAIL_BUMPER = block("rail_bumper", new RailBumperBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .strength(3.5F).requiresCorrectToolForDrops().noOcclusion()));
    public static final RailSemaphoreBlock RAIL_SEMAPHORE = block("rail_semaphore", new RailSemaphoreBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .strength(3.0F).requiresCorrectToolForDrops().noOcclusion()));
    public static final SignalConnectorBlock SIGNAL_CONNECTOR = block("signal_connector", new SignalConnectorBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .strength(2.5F).requiresCorrectToolForDrops().noOcclusion()));
    public static final RailControlBlock RAIL_CONTROL_SYSTEM = block("rail_control_system", new RailControlBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .strength(3.5F).requiresCorrectToolForDrops()));

    public static final net.minecraft.world.level.block.entity.BlockEntityType<SignalConnectorBlockEntity> SIGNAL_CONNECTOR_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("signal_connector"),
            net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    SignalConnectorBlockEntity::new, SIGNAL_CONNECTOR).build(null));
    public static final net.minecraft.world.level.block.entity.BlockEntityType<RailControlBlockEntity> RAIL_CONTROL_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id("rail_control_system"),
            net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    RailControlBlockEntity::new, RAIL_CONTROL_SYSTEM).build(null));

    public static final Item SIGNAL_LINKER = module("signal_linker",
            new SignalLinkerItem(new Item.Properties().stacksTo(1)));

    public static final net.minecraft.world.inventory.MenuType<FreightStationMenu> STATION_MENU = Registry.register(
            BuiltInRegistries.MENU, id("freight_station"),
            new net.minecraft.world.inventory.MenuType<>(FreightStationMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    public static final net.minecraft.world.inventory.MenuType<RailUtilityStationMenu> UTILITY_STATION_MENU = Registry.register(
            BuiltInRegistries.MENU, id("utility_station"),
            new net.minecraft.world.inventory.MenuType<>(RailUtilityStationMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    public static final net.minecraft.world.inventory.MenuType<RailControlMenu> RAIL_CONTROL_MENU = Registry.register(
            BuiltInRegistries.MENU, id("rail_control_system"),
            new net.minecraft.world.inventory.MenuType<>(RailControlMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    // Modular Cart Motors & Machine Payloads
    public static final Item COAL_MOTOR_MODULE = module("coal_motor_module",
            new CartModuleItem(CartModuleItem.MotorType.COAL, "coal_motor"));
    public static final Item ELECTRIC_MOTOR_MODULE = module("electric_motor_module",
            new CartModuleItem(CartModuleItem.MotorType.ELECTRIC, "electric_motor"));
    public static final Item CARGO_MODULE = module("cargo_module",
            new CartModuleItem(CartModuleItem.PayloadType.CARGO, "cargo"));
    public static final Item LIQUID_BARREL_MODULE = module("liquid_barrel_module",
            new CartModuleItem(CartModuleItem.PayloadType.LIQUID_BARREL, "liquid_barrel"));
    public static final Item ENERGY_CELL_MODULE = module("energy_cell_module",
            new CartModuleItem(CartModuleItem.PayloadType.ENERGY_CELL, "energy_cell"));
    public static final Item ARBORIST_MODULE = module("arborist_module",
            new CartModuleItem(CartModuleItem.PayloadType.ARBORIST, "arborist"));
    public static final Item SMELTER_MODULE = module("smelter_module",
            new CartModuleItem(CartModuleItem.PayloadType.SMELTER, "smelter"));
    public static final Item GRINDER_MODULE = module("grinder_module",
            new CartModuleItem(CartModuleItem.PayloadType.GRINDER, "grinder"));

    private static Item module(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, id(name), item);
    }

    private static <T extends net.minecraft.world.level.block.Block> T block(String name, T block) {
        Registry.register(BuiltInRegistries.BLOCK, id(name), block);
        Registry.register(BuiltInRegistries.ITEM, id(name), new BlockItem(block, new Item.Properties()));
        return block;
    }

    private static FreightStationBlock station(String name) {
        var block = Registry.register(BuiltInRegistries.BLOCK, id(name),
                new FreightStationBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                        .strength(3.5F).requiresCorrectToolForDrops()));
        Registry.register(BuiltInRegistries.ITEM, id(name), new BlockItem(block, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                    java.util.List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
                for (String key : new String[]{"placement", "port", "redstone"}) {
                    tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.mythic_rails.station." + key)
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
        });
        return block;
    }

    private static RailUtilityStationBlock utilityStation(String name, RailUtilityStationBlock.StationKind kind) {
        var block = Registry.register(BuiltInRegistries.BLOCK, id(name),
                new RailUtilityStationBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                        .strength(3.5F).requiresCorrectToolForDrops(), kind));
        Registry.register(BuiltInRegistries.ITEM, id(name), new BlockItem(block, new Item.Properties()) {
            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                    java.util.List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
                tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.mythic_rails.station.placement")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
                tooltip.add(net.minecraft.network.chat.Component.translatable(
                        kind.isFluid ? "tooltip.mythic_rails.utility_station.fluid" : "tooltip.mythic_rails.utility_station.energy")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        });
        return block;
    }

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }

    @Override
    public void onInitialize() {
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity(
                (station, side) -> station.storage(), STATION_BE);
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity(
                (station, side) -> station.kind().isFluid ? station.fluidStorage() : null, UTILITY_STATION_BE);
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (station, side) -> !station.kind().isFluid ? station.sidedEnergyStorage() : null, UTILITY_STATION_BE);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> {
                    entries.accept(FREIGHT_CART_ITEM);
                    entries.accept(COAL_MOTOR_MODULE);
                    entries.accept(ELECTRIC_MOTOR_MODULE);
                    entries.accept(CARGO_MODULE);
                    entries.accept(LIQUID_BARREL_MODULE);
                    entries.accept(ENERGY_CELL_MODULE);
                    entries.accept(ARBORIST_MODULE);
                    entries.accept(SMELTER_MODULE);
                    entries.accept(GRINDER_MODULE);
                    entries.accept(LOADING_STATION);
                    entries.accept(UNLOADING_STATION);
                    entries.accept(FLUID_LOADING_STATION);
                    entries.accept(FLUID_UNLOADING_STATION);
                    entries.accept(ENERGY_LOADING_STATION);
                    entries.accept(ENERGY_UNLOADING_STATION);
                    entries.accept(OBSIDIAN_RAIL);
                    entries.accept(EMBER_RAIL);
                    entries.accept(VOLCANITE_SPEED_RAIL);
                    entries.accept(OBSIDIAN_DETECTOR_RAIL);
                    entries.accept(SWITCH_RAIL);
                    entries.accept(RAIL_BUMPER);
                    entries.accept(RAIL_SEMAPHORE);
                    entries.accept(SIGNAL_CONNECTOR);
                    entries.accept(RAIL_CONTROL_SYSTEM);
                    entries.accept(SIGNAL_LINKER);
                });
    }
}
