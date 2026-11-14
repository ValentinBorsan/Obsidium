package net.valentin.obsidium.client.rei;

import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.SimpleGridMenuDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.valentin.obsidium.block.VolcaniteCrafterInterfaceMenu;
import net.valentin.obsidium.network.TransferRecipeToCrafterPayload;

import java.util.ArrayList;
import java.util.List;

public class VolcaniteCrafterTransferHandler implements TransferHandler {
    private static final CategoryIdentifier<?> CRAFTING_CATEGORY = CategoryIdentifier.of("minecraft", "plugins/crafting");

    @Override
    public ApplicabilityResult checkApplicable(Context context) {
        if (!(context.getMenu() instanceof VolcaniteCrafterInterfaceMenu)) {
            return ApplicabilityResult.createNotApplicable();
        }
        if (!CRAFTING_CATEGORY.equals(context.getDisplay().getCategoryIdentifier())) {
            return ApplicabilityResult.createNotApplicable();
        }
        return ApplicabilityResult.createApplicable();
    }

    @Override
    public Result handle(Context context) {
        if (!(context.getMenu() instanceof VolcaniteCrafterInterfaceMenu)) {
            return Result.createNotApplicable();
        }
        if (!CRAFTING_CATEGORY.equals(context.getDisplay().getCategoryIdentifier())) {
            return Result.createNotApplicable();
        }

        Display display = context.getDisplay();
        List<EntryIngredient> inputs = display.getInputEntries();

        List<List<Item>> candidatesList = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            candidatesList.add(new ArrayList<>());
        }

        if (display instanceof SimpleGridMenuDisplay gridDisplay) {
            int w = Math.min(3, gridDisplay.getWidth());
            int h = Math.min(3, gridDisplay.getHeight());
            int fullW = Math.max(1, gridDisplay.getWidth());
            for (int r = 0; r < h; r++) {
                for (int c = 0; c < w; c++) {
                    int srcIdx = c + r * fullW;
                    int dstIdx = c + r * 3;
                    if (srcIdx < inputs.size()) {
                        EntryIngredient ingredient = inputs.get(srcIdx);
                        for (EntryStack<?> es : ingredient) {
                            Object val = es.getValue();
                            if (val instanceof ItemStack stack && !stack.isEmpty()) {
                                if (!candidatesList.get(dstIdx).contains(stack.getItem())) {
                                    candidatesList.get(dstIdx).add(stack.getItem());
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < Math.min(9, inputs.size()); i++) {
                EntryIngredient ingredient = inputs.get(i);
                for (EntryStack<?> es : ingredient) {
                    Object val = es.getValue();
                    if (val instanceof ItemStack stack && !stack.isEmpty()) {
                        if (!candidatesList.get(i).contains(stack.getItem())) {
                            candidatesList.get(i).add(stack.getItem());
                        }
                    }
                }
            }
        }

        // When REI queries whether the + button is active:
        if (!context.isActuallyCrafting()) {
            // Always allow transfer when Crafter Interface is connected to the network
            return Result.createSuccessful();
        }

        // When the user actually clicked the + button:
        ClientPlayNetworking.send(new TransferRecipeToCrafterPayload(candidatesList, context.isStackedCrafting()));
        // Return to the Crafter Interface so the filled grid + ready output are visible immediately
        if (context.getContainerScreen() != null) {
            context.getMinecraft().setScreen(context.getContainerScreen());
        }
        return Result.createSuccessful();
    }

    @Override
    public double getPriority() {
        return 10.0; // Higher priority than default REI handlers so Crafter Interface uses this!
    }
}

