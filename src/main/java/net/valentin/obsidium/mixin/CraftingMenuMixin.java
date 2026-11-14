package net.valentin.obsidium.mixin;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.valentin.obsidium.ObsidiumMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(CraftingMenu.class)
public class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("HEAD"), cancellable = true)
    private static void onSlotChangedCraftingGrid(
            AbstractContainerMenu menu,
            Level level,
            Player player,
            CraftingContainer craftSlots,
            ResultContainer resultSlots,
            RecipeHolder<CraftingRecipe> recipe,
            CallbackInfo ci
    ) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // Fast pre-check: only scan all Crafting recipes if at least one slot contains an Obsidium item
            boolean hasObsidiumItem = false;
            boolean hasObsidiumIngot = false;
            for (int i = 0; i < craftSlots.getContainerSize(); i++) {
                ItemStack st = craftSlots.getItem(i);
                if (!st.isEmpty()) {
                    if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()).getNamespace().equals("obsidium")) {
                        hasObsidiumItem = true;
                        if (st.is(ObsidiumMod.PYRIC_BRONZE_INGOT) || st.is(ObsidiumMod.FEROPLUMB_INGOT) || st.is(ObsidiumMod.VOLCANITE_INGOT)) {
                            hasObsidiumIngot = true;
                        }
                    }
                }
            }

            if (!hasObsidiumItem) {
                return;
            }

            CraftingInput craftingInput = craftSlots.asCraftInput();

            // 1. Prioritize Obsidium's own recipes when crafting with Obsidium materials (e.g. Pyric Bronze -> Promethean Tools)
            Optional<RecipeHolder<CraftingRecipe>> obsidiumMatch = level.getServer().getRecipeManager()
                    .getAllRecipesFor(RecipeType.CRAFTING).stream()
                    .filter(r -> r.id().getNamespace().equals("obsidium") && r.value().matches(craftingInput, level))
                    .findFirst();

            if (obsidiumMatch.isPresent()) {
                RecipeHolder<CraftingRecipe> holder = obsidiumMatch.get();
                if (isEarlyGameRecipe(holder.id().getPath())) {
                    if (resultSlots.setRecipeUsed(level, serverPlayer, holder)) {
                        ItemStack result = holder.value().assemble(craftingInput, level.registryAccess());
                        if (result.isItemEnabled(level.enabledFeatures())) {
                            resultSlots.setItem(0, result);
                            menu.setRemoteSlot(0, result);
                            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                                    menu.containerId, menu.incrementStateId(), 0, result
                            ));
                            ci.cancel();
                            return;
                        }
                    }
                } else {
                    // Mid/Late game Obsidium machines & equipment are forbidden in vanilla 3x3 table (require Volcanite Worktable / Magma Forge)
                    resultSlots.setItem(0, ItemStack.EMPTY);
                    menu.setRemoteSlot(0, ItemStack.EMPTY);
                    serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                            menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY
                    ));
                    ci.cancel();
                    return;
                }
            }

            // 2. Prevent other mods (like Tech Reborn) from using Obsidium custom ingots (Pyric Bronze, Feroplumb, Volcanite) to craft generic tools
            if (hasObsidiumIngot) {
                resultSlots.setItem(0, ItemStack.EMPTY);
                menu.setRemoteSlot(0, ItemStack.EMPTY);
                serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                        menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY
                ));
                ci.cancel();
            }
        }
    }

    private static boolean isEarlyGameRecipe(String path) {
        return path.startsWith("volcanite_worktable")
                || path.startsWith("volcanic_alloy")
                || path.startsWith("volcanic_ash")
                || path.startsWith("volcanic_plate")
                || path.startsWith("volcanite_wrench")
                || path.startsWith("volcanite_binding")
                || path.startsWith("volcanite_ingot")
                || path.startsWith("hydrothermal_vitrifier")
                || path.startsWith("magmatic_crucible")
                || path.startsWith("lava_bucket_from_magma_droplets")
                || path.startsWith("naiad_fluid_pipe")
                || path.startsWith("volcanite_logistics_module")
                || path.startsWith("volcanite_energy_wire")
                || path.startsWith("astrape_energy_wire")
                || path.startsWith("hermes_item_pipe")
                || path.startsWith("hydrothermal_condenser")
                || path.startsWith("water_bucket_from_dew_droplets")
                || path.startsWith("plouton_rod")
                || path.startsWith("telluric_astrolabe")
                || path.startsWith("charon_coin")
                || path.startsWith("magma_forge_manual")
                || path.startsWith("basic_backpack")
                || path.startsWith("dionysian_distillery")
                || path.startsWith("dionysian_press")
                || path.startsWith("demetrian_biomass_generator")
                || path.startsWith("demetrian_composter")
                || path.startsWith("demetrian_grafting_knife")
                || path.startsWith("dionysian_aging_barrel")
                || path.startsWith("empty_tuica_bottle_1l")
                || path.startsWith("pyrogenic_bloom_infuser")
                || path.startsWith("thermic_soil_tiller")
                || path.startsWith("stygian_cenotaph")
                || path.startsWith("volcanite_lamp")
                || path.startsWith("volcanite_bottle")
                || path.startsWith("ash_briquette")
                || path.startsWith("stick_bundle")
                || path.startsWith("volcanite_energy_cell")
                || path.startsWith("volcanite_energy_unit")
                || path.startsWith("advanced_volcanite_energy_cell")
                || path.startsWith("volcanic_cement")
                || path.startsWith("volcanic_wood_liquid_barrel")
                || path.startsWith("reinforced_volcanic_liquid_barrel")
                || path.startsWith("dionysian_barrel_core")
                || path.startsWith("mega_volcanic_barrel")
                || path.startsWith("thyrsus_of_dionysus")
                || path.startsWith("cornucopia")
                || path.startsWith("talaria_of_hermes")
                || path.startsWith("tartarean_")
                || path.startsWith("promethean_")
                || path.startsWith("pyric_bronze_")
                || path.startsWith("feroplumb_")
                || path.contains("_planks")
                || path.contains("_slab")
                || path.contains("_stairs")
                || path.contains("_fence")
                || path.equals("pipe_facade")
                || path.contains("_pipe_facade")
                || path.contains("_planks")
                || path.contains("_slab")
                || path.contains("_stairs")
                || path.contains("_fence")
                || path.contains("_pipe_facade")
                || path.contains("_block")
                || path.contains("_nugget")
                || path.contains("_petal")
                || path.contains("_seed")
                || path.contains("_sapling");
    }
}
