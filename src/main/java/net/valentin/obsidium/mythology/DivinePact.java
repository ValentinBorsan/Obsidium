package net.valentin.obsidium.mythology;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum DivinePact {
    NONE("none", "None", ChatFormatting.GRAY, new String[0]),
    ZEUS("zeus", "Pact of Zeus", ChatFormatting.GOLD, new String[]{
            "§e⚡ Astrape Thunderbolt",
            "§7• Strikes foes with arc lightning (§b+4.0 Magic Dmg§7)",
            "§7• Chains to up to 3 nearby hostiles (§b+3.0 Dmg§7)",
            "§7• §6100% trigger rate §7during active Thunderstorms"
    }),
    APOLLO("apollo", "Pact of Apollo", ChatFormatting.YELLOW, new String[]{
            "§6☀ Solar Radiance",
            "§7• Ignites struck targets in holy flame for 6 seconds",
            "§7• Deals §c+3.5 True Fire Damage §7bypassing armor",
            "§7• Maximum potency under direct midday sunlight"
    }),
    ARTEMIS("artemis", "Pact of Artemis", ChatFormatting.AQUA, new String[]{
            "§b🌙 Moonlit Grace",
            "§7• Massive projectile velocity & critical strike chance",
            "§7• Carried Artemis Silver Fruit grants infinite arrows",
            "§7• Bonus piercing damage under the night sky"
    }),
    ATHENA("athena", "Pact of Athena", ChatFormatting.LIGHT_PURPLE, new String[]{
            "§d🛡 Aegis Fortitude",
            "§7• 35% chance to completely nullify incoming damage",
            "§7• Emits a kinetic shockwave that repels attacking foes",
            "§7• Effective when held in either main-hand or off-hand"
    }),
    HEPHAESTUS("hephaestus", "Pact of Hephaestus", ChatFormatting.RED, new String[]{
            "§c⚒ Volcanic Temper",
            "§7• Automatically smelts mined raw ores into ingots",
            "§7• Self-repairs durability when near magma, lava or heat",
            "§7• Ignites struck targets with volcanic heat"
    }),
    POSEIDON("poseidon", "Pact of Poseidon", ChatFormatting.BLUE, new String[]{
            "§9🌊 Abyssal Tide",
            "§7• Devastating §b+6.0 attack damage §7in rain or water",
            "§7• Unleashes an oceanic riptide surge that knocks foes back",
            "§7• Bestows water breathing and aquatic swiftness"
    }),
    THANATOS("thanatos", "Pact of Thanatos", ChatFormatting.DARK_PURPLE, new String[]{
            "§5💀 Soul Siphon",
            "§7• Siphons the soul life-force of slain enemies",
            "§7• Instantly heals the wielder (§a+2.0 Hearts§7) on kill",
            "§7• Potency doubled in the Nether and the Void"
    });

    private final String id;
    private final String displayName;
    private final ChatFormatting color;
    private final String[] benefits;

    DivinePact(String id, String displayName, ChatFormatting color, String[] benefits) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.benefits = benefits;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String[] getBenefits() {
        return benefits;
    }

    public String getDescription() {
        return benefits.length > 0 ? benefits[0] : "";
    }

    public Component getFormattedBadge() {
        return Component.literal(displayName).withStyle(color, ChatFormatting.BOLD);
    }

    public static DivinePact fromId(String id) {
        if (id == null) return NONE;
        for (DivinePact pact : values()) {
            if (pact.id.equalsIgnoreCase(id)) {
                return pact;
            }
        }
        return NONE;
    }
}

