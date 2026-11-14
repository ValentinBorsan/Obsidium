package net.valentin.obsidium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class VolcaniteBarrelUpgradeItem extends Item {
    public enum UpgradeType {
        BASIC(4, "Tier I Basic (+4x Capacity)", 0xFFFF9933),
        ADVANCED(16, "Tier II Advanced (+16x Capacity)", 0xFF00E5FF),
        SUPREME(64, "Tier III Supreme (+64x Capacity)", 0xFFFF3399),
        VOID(0, "Void Overflow (Destroys excess items)", 0xFFAA2233);

        private final int multiplier;
        private final String displayName;
        private final int color;

        UpgradeType(int multiplier, String displayName, int color) {
            this.multiplier = multiplier;
            this.displayName = displayName;
            this.color = color;
        }

        public int getMultiplier() {
            return multiplier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getColor() {
            return color;
        }
    }

    private final UpgradeType upgradeType;

    public VolcaniteBarrelUpgradeItem(Properties properties, UpgradeType upgradeType) {
        super(properties.stacksTo(16).fireResistant());
        this.upgradeType = upgradeType;
    }

    public UpgradeType getUpgradeType() {
        return upgradeType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6✦ Volcanite Storage Barrel Module"));
        tooltip.add(Component.literal("§eType: §f" + upgradeType.getDisplayName()));
        if (upgradeType == UpgradeType.VOID) {
            tooltip.add(Component.literal("§c• Automatically voids incoming items when barrel is 100% full"));
            tooltip.add(Component.literal("§8• Prevents factory backups and pipe stalling"));
        } else {
            tooltip.add(Component.literal("§7• Expands single-item barrel capacity by §6+" + upgradeType.getMultiplier() + "x §7base"));
            tooltip.add(Component.literal("§8• Stacks up to 4 upgrades in a single barrel"));
        }
        tooltip.add(Component.literal("§8• Right-click directly onto a Volcanite Barrel to install"));
    }
}

