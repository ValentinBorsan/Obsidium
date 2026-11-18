package net.valentin.mythicrails;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Modular motor or machine payload item installable in a Hermes Modular Cart. */
public final class CartModuleItem extends Item {
    public enum MotorType {
        NONE(0), COAL(1), ELECTRIC(2);
        public final int id;
        MotorType(int id) { this.id = id; }
        public static MotorType byId(int id) {
            return switch (id) { case 1 -> COAL; case 2 -> ELECTRIC; default -> NONE; };
        }
    }

    public enum PayloadType {
        NONE(0), CARGO(1), LIQUID_BARREL(2), ENERGY_CELL(3), ARBORIST(4), SMELTER(5), GRINDER(6);
        public final int id;
        PayloadType(int id) { this.id = id; }
        public static PayloadType byId(int id) {
            return switch (id) {
                case 1 -> CARGO; case 2 -> LIQUID_BARREL; case 3 -> ENERGY_CELL;
                case 4 -> ARBORIST; case 5 -> SMELTER; case 6 -> GRINDER; default -> NONE;
            };
        }
    }

    private final MotorType motorType;
    private final PayloadType payloadType;
    private final String tooltipKey;

    public CartModuleItem(MotorType motorType, String tooltipKey) {
        super(new Properties().stacksTo(1));
        this.motorType = motorType;
        this.payloadType = PayloadType.NONE;
        this.tooltipKey = tooltipKey;
    }

    public CartModuleItem(PayloadType payloadType, String tooltipKey) {
        super(new Properties().stacksTo(1));
        this.motorType = MotorType.NONE;
        this.payloadType = payloadType;
        this.tooltipKey = tooltipKey;
    }

    public boolean isMotor() { return motorType != MotorType.NONE; }
    public boolean isPayload() { return payloadType != PayloadType.NONE; }
    public MotorType motorType() { return motorType; }
    public PayloadType payloadType() { return payloadType; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mythic_rails.module." + tooltipKey).withStyle(ChatFormatting.GRAY));
    }
}
