package net.valentin.obsidium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.valentin.obsidium.ObsidiumMod;

import java.util.List;

public class DemetrianScionItem extends Item {

    public DemetrianScionItem(Properties properties) {
        super(properties.stacksTo(64).rarity(Rarity.UNCOMMON));
    }

    public static ItemStack create(String species, int quality) {
        ItemStack stack = new ItemStack(ObsidiumMod.DEMETRIAN_SCION);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("Species", species != null ? species : "oak");
            tag.putInt("Quality", Math.max(1, Math.min(3, quality)));
        });
        return stack;
    }

    public static String getSpecies(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) return "oak";
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getString("Species");
    }

    public static int getQuality(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) return 1;
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        int q = data.copyTag().getInt("Quality");
        return q > 0 ? q : 1;
    }

    @Override
    public Component getName(ItemStack stack) {
        String species = getSpecies(stack);
        String formattedName = formatSpeciesName(species);
        return Component.literal(formattedName + " Scion").withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String species = getSpecies(stack);
        int quality = getQuality(stack);

        tooltip.add(Component.literal("§6[Specie Donatoare] §e" + formatSpeciesName(species)));
        String stars = switch (quality) {
            case 3 -> "§a★★★ §7(Apex Vigor)";
            case 2 -> "§e★★☆ §7(Pure Vigor)";
            default -> "§7★☆☆ (Standard)";
        };
        tooltip.add(Component.literal("§7Calitate: " + stars));
        tooltip.add(Component.literal("§8Apply to an incised sapling using the Demetrian Grafting Knife"));
    }

    public static String formatSpeciesName(String species) {
        if (species == null) return "Oak";
        return switch (species) {
            case "midas_gold" -> "Midas Gold";
            case "ares_iron" -> "Ares Iron";
            case "hermes_copper" -> "Hermes Copper";
            case "hephaestus_volcanite" -> "Hephaestus Volcanite";
            case "cronus_lead" -> "Cronus Lead";
            case "zeus_tin" -> "Zeus Tin";
            case "artemis_silver" -> "Artemis Silver";
            case "electrum" -> "Electrum";
            case "tartarean_netherite" -> "Tartarean Netherite";
            case "hades_coal" -> "Hades Coal";
            case "astrape_redstone" -> "Astrape Redstone";
            case "poseidon_lapis" -> "Poseidon Lapis";
            case "apollo_quartz" -> "Apollo Quartz";
            case "dionysian_amethyst" -> "Dionysian Amethyst";
            case "plouton_emerald" -> "Plouton Emerald";
            case "adamantine_diamond" -> "Adamantine Diamond";
            case "birch" -> "Birch";
            case "spruce" -> "Spruce";
            case "jungle" -> "Jungle";
            case "acacia" -> "Acacia";
            case "dark_oak" -> "Dark Oak";
            case "cherry" -> "Cherry";
            case "mangrove" -> "Mangrove";
            default -> {
                String clean = species.replace('_', ' ');
                yield Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
            }
        };
    }
}
