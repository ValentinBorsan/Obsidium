package net.valentin.obsidium.client.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.valentin.obsidium.ObsidiumMod;
import net.valentin.obsidium.block.VolcaniteWorktableMenu;
import net.valentin.obsidium.mythology.DivinePact;

public class ObsidiumReiClientPlugin implements REIClientPlugin {

        @Override
        public void registerCategories(CategoryRegistry registry) {
                // 1. Volcanite Grinding Category
                registry.add(new GrindingCategory());
                registry.addWorkstations(GrindingDisplay.CATEGORY, EntryStacks.of(ObsidiumMod.VOLCANITE_GRINDER_BLOCK));

                // 2. Dionysian Distillery Category
                registry.add(new DistilleryCategory());
                registry.addWorkstations(DistilleryDisplay.CATEGORY,
                                EntryStacks.of(ObsidiumMod.DIONYSIAN_DISTILLERY_BLOCK));

                // 3. Olympian Altar Divine Ritual Category
                registry.add(new OlympianAltarCategory());
                registry.addWorkstations(OlympianAltarDisplay.CATEGORY,
                                EntryStacks.of(ObsidiumMod.OLYMPIAN_ALTAR_BLOCK),
                                EntryStacks.of(ObsidiumMod.ASTRAPE_RITUAL_OBELISK_BLOCK));
        }

        @Override
        public void registerDisplays(DisplayRegistry registry) {
                // ==========================================
                // 1. VOLCANITE GRINDER RECIPES
                // ==========================================
                registry.add(new GrindingDisplay(Items.RAW_IRON, new ItemStack(ObsidiumMod.IRON_DUST, 2)));
                registry.add(new GrindingDisplay(Items.IRON_ORE, new ItemStack(ObsidiumMod.IRON_DUST, 2)));
                registry.add(new GrindingDisplay(Items.IRON_INGOT, new ItemStack(ObsidiumMod.IRON_DUST, 1)));

                registry.add(new GrindingDisplay(Items.RAW_GOLD, new ItemStack(ObsidiumMod.GOLD_DUST, 2)));
                registry.add(new GrindingDisplay(Items.GOLD_ORE, new ItemStack(ObsidiumMod.GOLD_DUST, 2)));
                registry.add(new GrindingDisplay(Items.GOLD_INGOT, new ItemStack(ObsidiumMod.GOLD_DUST, 1)));

                registry.add(new GrindingDisplay(Items.RAW_COPPER, new ItemStack(ObsidiumMod.COPPER_DUST, 2)));
                registry.add(new GrindingDisplay(Items.COPPER_ORE, new ItemStack(ObsidiumMod.COPPER_DUST, 2)));
                registry.add(new GrindingDisplay(Items.COPPER_INGOT, new ItemStack(ObsidiumMod.COPPER_DUST, 1)));

                registry.add(new GrindingDisplay(ObsidiumMod.RAW_TIN, new ItemStack(ObsidiumMod.TIN_DUST, 2)));
                registry.add(new GrindingDisplay(ObsidiumMod.TIN_ORE_ITEM, new ItemStack(ObsidiumMod.TIN_DUST, 2)));
                registry.add(new GrindingDisplay(ObsidiumMod.TIN_INGOT, new ItemStack(ObsidiumMod.TIN_DUST, 1)));

                registry.add(new GrindingDisplay(ObsidiumMod.RAW_LEAD, new ItemStack(ObsidiumMod.LEAD_DUST, 2)));
                registry.add(new GrindingDisplay(ObsidiumMod.LEAD_ORE_ITEM, new ItemStack(ObsidiumMod.LEAD_DUST, 2)));
                registry.add(new GrindingDisplay(ObsidiumMod.LEAD_INGOT, new ItemStack(ObsidiumMod.LEAD_DUST, 1)));

                registry.add(new GrindingDisplay(ObsidiumMod.RAW_SILVER, new ItemStack(ObsidiumMod.SILVER_DUST, 2)));
                registry.add(new GrindingDisplay(ObsidiumMod.SILVER_ORE_ITEM,
                                new ItemStack(ObsidiumMod.SILVER_DUST, 2)));
                registry.add(new GrindingDisplay(ObsidiumMod.SILVER_INGOT, new ItemStack(ObsidiumMod.SILVER_DUST, 1)));
                registry.add(new GrindingDisplay(ObsidiumMod.ARTEMIS_SILVER_FRUIT,
                                new ItemStack(ObsidiumMod.SILVER_DUST, 2)));

                registry.add(new GrindingDisplay(ObsidiumMod.VOLCANITE_ORE_ITEM,
                                new ItemStack(ObsidiumMod.VOLCANITE_DUST, 2)));
                registry.add(new GrindingDisplay(ObsidiumMod.VOLCANITE, new ItemStack(ObsidiumMod.VOLCANITE_DUST, 2)));
                registry.add(new GrindingDisplay(ObsidiumMod.VOLCANITE_INGOT,
                                new ItemStack(ObsidiumMod.VOLCANITE_DUST, 1)));
                registry.add(new GrindingDisplay(ObsidiumMod.VOLCANIC_ALLOY,
                                new ItemStack(ObsidiumMod.VOLCANITE_DUST, 1)));

                registry.add(new GrindingDisplay(ObsidiumMod.PYRIC_BRONZE_INGOT,
                                new ItemStack(ObsidiumMod.PYRIC_BRONZE_DUST, 1)));
                registry.add(new GrindingDisplay(ObsidiumMod.FEROPLUMB_INGOT,
                                new ItemStack(ObsidiumMod.FEROPLUMB_DUST, 1)));

                registry.add(new GrindingDisplay(Items.COBBLESTONE, new ItemStack(Items.GRAVEL, 1)));
                registry.add(new GrindingDisplay(Items.GRAVEL, new ItemStack(Items.SAND, 1)));

                // ==========================================
                // 2. DIONYSIAN DISTILLERY RECIPES
                // ==========================================
                ItemStack ash = new ItemStack(ObsidiumMod.VOLCANIC_ASH, 1);
                registry.add(new DistilleryDisplay(ObsidiumMod.DIONYSIAN_POMACE, Items.GLASS_BOTTLE,
                                new ItemStack(ObsidiumMod.VOLCANIC_TUICA), ash));
                registry.add(new DistilleryDisplay(Items.APPLE, Items.GLASS_BOTTLE,
                                new ItemStack(ObsidiumMod.VOLCANIC_CIDER), ash));
                registry.add(new DistilleryDisplay(ObsidiumMod.ARTEMIS_SILVER_FRUIT, Items.GLASS_BOTTLE,
                                new ItemStack(ObsidiumMod.VINTAGE_VOLCANIC_TUICA), ash));
                registry.add(new DistilleryDisplay(ObsidiumMod.MIDAS_GOLD_FRUIT, Items.GLASS_BOTTLE,
                                new ItemStack(ObsidiumMod.VINTAGE_VOLCANIC_TUICA), ash));
                registry.add(new DistilleryDisplay(ObsidiumMod.HERMES_COPPER_FRUIT, Items.GLASS_BOTTLE,
                                new ItemStack(ObsidiumMod.VINTAGE_VOLCANIC_TUICA), ash));
                registry.add(new DistilleryDisplay(ObsidiumMod.ARES_IRON_FRUIT, Items.GLASS_BOTTLE,
                                new ItemStack(ObsidiumMod.VOLCANIC_TUICA), ash));
                registry.add(new DistilleryDisplay(ObsidiumMod.CRONUS_LEAD_FRUIT, Items.GLASS_BOTTLE,
                                new ItemStack(ObsidiumMod.VOLCANIC_TUICA), ash));
                registry.add(new DistilleryDisplay(ObsidiumMod.ZEUS_TIN_FRUIT, Items.GLASS_BOTTLE,
                                new ItemStack(ObsidiumMod.VOLCANIC_TUICA), ash));
                registry.add(new DistilleryDisplay(ObsidiumMod.HEPHAESTUS_VOLCANITE_FRUIT, Items.GLASS_BOTTLE,
                                new ItemStack(ObsidiumMod.VOLCANIC_TUICA_1L), ash));
                registry.add(new DistilleryDisplay(ObsidiumMod.TARTAREAN_NETHERITE_FRUIT, Items.GLASS_BOTTLE,
                                new ItemStack(ObsidiumMod.VOLCANIC_TUICA_1L), ash));

                // ==========================================
                // 3. OLYMPIAN ALTAR DIVINE RITUALS
                // ==========================================
                ItemStack papyrus = new ItemStack(ObsidiumMod.VOLCANIC_PAPYRUS);
                ItemStack sacrifice = new ItemStack(ObsidiumMod.VOLCANITE_INGOT);

        registry.add(new OlympianAltarDisplay(
                new ItemStack(ObsidiumMod.HEPHAESTEAN_SWORD),
                new ItemStack(ObsidiumMod.SCROLL_OF_ZEUS),
                papyrus, sacrifice,
                DivinePact.ZEUS,
                "§e⚡ Active Thunderstorm & Lightning"
        ));

        registry.add(new OlympianAltarDisplay(
                new ItemStack(ObsidiumMod.HEPHAESTEAN_PICKAXE),
                new ItemStack(ObsidiumMod.SCROLL_OF_APOLLO),
                papyrus, sacrifice,
                DivinePact.APOLLO,
                "§6☀️ Clear Midday Sky (Direct Sunlight)"
        ));

        registry.add(new OlympianAltarDisplay(
                new ItemStack(ObsidiumMod.BOW_OF_ARTEMIS),
                new ItemStack(ObsidiumMod.SCROLL_OF_ARTEMIS),
                papyrus, sacrifice,
                DivinePact.ARTEMIS,
                "§b🌙 Clear Night Sky (Under Moonlight)"
        ));

        registry.add(new OlympianAltarDisplay(
                new ItemStack(ObsidiumMod.AEGIS_OF_ATHENA),
                new ItemStack(ObsidiumMod.SCROLL_OF_ATHENA),
                papyrus, sacrifice,
                DivinePact.ATHENA,
                "§9🛡️ High Mountain Peak (Altitude Y > 90)"
        ));

        registry.add(new OlympianAltarDisplay(
                new ItemStack(ObsidiumMod.HEPHAESTEAN_PAXEL),
                new ItemStack(ObsidiumMod.SCROLL_OF_HEPHAESTUS),
                papyrus, sacrifice,
                DivinePact.HEPHAESTUS,
                "§c🔥 Active Lava or Magma Below Altar"
        ));

        registry.add(new OlympianAltarDisplay(
                new ItemStack(ObsidiumMod.HEPHAESTEAN_AXE),
                new ItemStack(ObsidiumMod.SCROLL_OF_POSEIDON),
                papyrus, sacrifice,
                DivinePact.POSEIDON,
                "§1🌊 Heavy Rain / Downpour on Altar"
        ));

        registry.add(new OlympianAltarDisplay(
                new ItemStack(ObsidiumMod.HEPHAESTEAN_SWORD),
                new ItemStack(ObsidiumMod.SCROLL_OF_THANATOS),
                papyrus, sacrifice,
                DivinePact.THANATOS,
                "§5💀 Depths of the Tartarus Abyss (Y < 0 / Nether)"
        ));
    }

        @Override
        public void registerTransferHandlers(TransferHandlerRegistry registry) {
                registry.register(SimpleTransferHandler.create(
                                VolcaniteWorktableMenu.class,
                                CategoryIdentifier.of("minecraft", "plugins/crafting"),
                                new SimpleTransferHandler.IntRange(0, 9)));

                // Intelligent Auto-Craft Transfer for Crafter Interface that pulls from both
                // player inventory & storage network!
                registry.register(new VolcaniteCrafterTransferHandler());
        }
}
