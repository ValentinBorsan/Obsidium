package net.valentin.obsidium.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class QuantumLinkModuleItem extends Item {
    public QuantumLinkModuleItem(Properties properties) {
        super(properties.fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("§dQuantum Cross-Dimensional Link"));
        tooltipComponents.add(Component.literal("§7• §fModular Chest: §7Empty local storage, then insert into a Module Bay"));
        tooltipComponents.add(Component.literal("§7• §fLiquid Barrel: §7Right-click barrel to link"));
        tooltipComponents.add(Component.literal("§7• §fDye: §7Right-click linked block to change color (16 colors)"));
        tooltipComponents.add(Component.literal("§7• §fChannels: §7Configure #1–999 & Public/Private in GUI"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    /**
     * Returns a vivid RGB tint (0xRRGGBB) for block/GUI rendering of the 16 DyeColor channels.
     */
    public static int getChannelRgb(DyeColor color) {
        if (color == null) return 0xB84DFF;
        return switch (color) {
            case WHITE -> 0xF5F5FF;
            case ORANGE -> 0xFF8822;
            case MAGENTA -> 0xE04BFF;
            case LIGHT_BLUE -> 0x4BC8FF;
            case YELLOW -> 0xFFE135;
            case LIME -> 0x6BFF3B;
            case PINK -> 0xFF77B8;
            case GRAY -> 0x7A828E;
            case LIGHT_GRAY -> 0xBFC6D0;
            case CYAN -> 0x1AD9D9;
            case PURPLE -> 0xA84DFF;
            case BLUE -> 0x3F6EFF;
            case BROWN -> 0xA66A3E;
            case GREEN -> 0x39B54A;
            case RED -> 0xFF3B47;
            case BLACK -> 0x3B3447;
        };
    }

    public static String getColorDisplayName(DyeColor color) {
        if (color == null) return "Purple";
        String raw = color.getName().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String part : raw.split(" ")) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
