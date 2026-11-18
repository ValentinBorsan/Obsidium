package net.valentin.obsidium.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.valentin.obsidium.block.VolcanitePipeModuleMenu;
import net.valentin.obsidium.block.VolcaniteStoragePipeBlockEntity;

public class VolcanitePipeModuleScreen extends AbstractContainerScreen<VolcanitePipeModuleMenu> {
    private static final String[] FACE_SHORT = { "D", "U", "N", "S", "W", "E" };

    private final Button[] faceButtons = new Button[6];
    private Button syncAllButton;
    private Button toggleModeButton;
    private Button toggleTypeButton;
    private Button routingModeButton;
    private Button redstoneModeButton;
    private Button toggleFilterModeButton;

    public VolcanitePipeModuleScreen(VolcanitePipeModuleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.titleLabelX = 1000;
        this.inventoryLabelX = 1000;
    }

    private void clickMenuBtn(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Row 1 (y+16): 6-Face Selector Tabs (D, U, N, S, W, E) + Sync ALL
        for (int i = 0; i < 6; i++) {
            final int btnId = 10 + i;
            this.faceButtons[i] = this.addRenderableWidget(Button.builder(Component.literal(FACE_SHORT[i]), b -> clickMenuBtn(btnId))
                    .bounds(x + 8 + i * 21, y + 16, 20, 14).build());
        }
        this.syncAllButton = this.addRenderableWidget(Button.builder(Component.literal("ALL"), b -> clickMenuBtn(16))
                .bounds(x + 136, y + 16, 32, 14).build());

        // Row 2 (y+33): Direction Mode (IMPORT/EXPORT/AUTO) & Transfer Channel (ITEMS/FLUIDS/ENERGY/OMNI)
        this.toggleModeButton = this.addRenderableWidget(Button.builder(Component.literal("Mode"), b -> clickMenuBtn(0))
                .bounds(x + 8, y + 33, 78, 16).build());
        this.toggleTypeButton = this.addRenderableWidget(Button.builder(Component.literal("Type"), b -> clickMenuBtn(1))
                .bounds(x + 90, y + 33, 78, 16).build());

        // Row 3 (y+74): Priority (-/+), Routing (RR/NEAR), Redstone Mode
        this.addRenderableWidget(Button.builder(Component.literal("-"), b -> clickMenuBtn(4))
                .bounds(x + 8, y + 74, 14, 15).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), b -> clickMenuBtn(5))
                .bounds(x + 47, y + 74, 14, 15).build());
        this.routingModeButton = this.addRenderableWidget(Button.builder(Component.literal("NEAR"), b -> clickMenuBtn(6))
                .bounds(x + 63, y + 74, 52, 15).build());
        this.redstoneModeButton = this.addRenderableWidget(Button.builder(Component.literal("ON"), b -> clickMenuBtn(7))
                .bounds(x + 117, y + 74, 51, 15).build());

        // Row 4 (y+96): Filter Mode (WHITE/BLACK) & Clear
        this.toggleFilterModeButton = this.addRenderableWidget(Button.builder(Component.literal("WHITE"), b -> clickMenuBtn(2))
                .bounds(x + 71, y + 96, 42, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> clickMenuBtn(3))
                .bounds(x + 114, y + 96, 15, 18).build());

        updateButtonText();
    }

    private void updateButtonText() {
        boolean isFiber = this.menu.isOpticFiber();
        boolean isConduit = this.menu.isUniversalConduit();
        int curFace = this.menu.getSide().get3DDataValue();
        int connMask = this.menu.getConnectedFacesMask();
        int activeMask = this.menu.getActiveFacesMask();

        for (int i = 0; i < 6; i++) {
            if (faceButtons[i] == null) continue;
            boolean selected = (i == curFace);
            boolean connected = (connMask & (1 << i)) != 0;
            boolean configured = (activeMask & (1 << i)) != 0;
            String color = selected ? "§e§l" : (configured ? "§b" : (connected ? "§a" : "§7"));
            faceButtons[i].setMessage(Component.literal(color + FACE_SHORT[i]));
        }

        if (syncAllButton != null) {
            syncAllButton.active = this.menu.canSwitchType();
            syncAllButton.setMessage(Component.literal(this.menu.canSwitchType() ? "§d⟳ALL" : "§8ALL"));
        }

        if (toggleModeButton != null) {
            int mode = this.menu.getModuleMode();
            if (isFiber) {
                toggleModeButton.active = false;
                toggleModeButton.setMessage(Component.literal(mode == 1 ? "§b📥 IMPORT LINK" : "§6📤 EXPORT LINK"));
            } else if (isConduit) {
                toggleModeButton.active = true;
                if (mode == 0) {
                    toggleModeButton.setMessage(Component.literal("§d⇄ AUTO"));
                } else if (mode == 1) {
                    toggleModeButton.setMessage(Component.literal("§6📥 IMPORT"));
                } else {
                    toggleModeButton.setMessage(Component.literal("§b📤 EXPORT"));
                }
            } else {
                toggleModeButton.active = true;
                if (mode == 0) {
                    toggleModeButton.setMessage(Component.literal("§e⚡ AUTO"));
                } else if (mode == 1) {
                    toggleModeButton.setMessage(Component.literal("§6⇄ EXTRACT"));
                } else {
                    toggleModeButton.setMessage(Component.literal("§b📥 INSERT"));
                }
            }
        }

        if (toggleTypeButton != null) {
            if (!this.menu.canSwitchType()) {
                toggleTypeButton.setMessage(Component.literal("§a📦 ITEMS"));
                toggleTypeButton.active = false;
            } else {
                toggleTypeButton.active = true;
                int type = this.menu.getTransferType();
                switch (type) {
                    case VolcaniteStoragePipeBlockEntity.TYPE_FLUID -> toggleTypeButton.setMessage(Component.literal("§b💧 FLUIDS"));
                    case VolcaniteStoragePipeBlockEntity.TYPE_ENERGY -> toggleTypeButton.setMessage(Component.literal("§6⚡ ENERGY"));
                    case VolcaniteStoragePipeBlockEntity.TYPE_OMNI -> toggleTypeButton.setMessage(Component.literal("§d🌐 OMNI"));
                    default -> toggleTypeButton.setMessage(Component.literal("§a📦 ITEMS"));
                }
            }
        }

        if (routingModeButton != null) {
            routingModeButton.setMessage(Component.literal(this.menu.isRoundRobin() ? "§e⟳ RR" : "§a🎯 NEAR"));
        }

        if (redstoneModeButton != null) {
            int rs = this.menu.getRedstoneMode();
            String rsText = switch (rs) {
                case 1 -> "§c⚡ HIGH";
                case 2 -> "§b🌙 LOW";
                default -> "§a☀ ON";
            };
            redstoneModeButton.setMessage(Component.literal(rsText));
        }

        if (toggleFilterModeButton != null) {
            boolean blacklist = this.menu.isFilterBlacklist();
            toggleFilterModeButton.setMessage(Component.literal(blacklist ? "§cBLACK" : "§aWHITE"));
        }
    }

    private static String getFluidShortName(int id) {
        return switch (id) {
            case 1 -> "Lava";
            case 2 -> "Water";
            case 3 -> "Pyroclastic";
            case 4 -> "Steam";
            case 5 -> "Bio-Oil";
            case 6 -> "Ethanol";
            case 7 -> "Biodiesel";
            case 8 -> "Pyro-Diesel";
            default -> "Empty";
        };
    }

    private String formatEnergyShort() {
        int hundreds = this.menu.getStoredEnergyHundreds();
        if (hundreds <= 0) return "0 RF";
        if (hundreds < 10) return (hundreds * 100) + " RF";
        double krf = hundreds / 10.0;
        if (krf >= 100.0) return String.format("%.0fk", krf);
        return String.format("%.1fk", krf);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        updateButtonText();
        int mode = this.menu.getModuleMode();
        int type = this.menu.getTransferType();
        Direction side = this.menu.getSide();
        boolean hasEnergy = this.menu.supportsEnergy();
        boolean isFiber = this.menu.isOpticFiber();
        boolean isConduit = this.menu.isUniversalConduit();

        // 1. Header Title with channel color & face badge
        int headerColor;
        String titleText;
        String typeName = switch (type) {
            case VolcaniteStoragePipeBlockEntity.TYPE_FLUID -> "FLUIDS";
            case VolcaniteStoragePipeBlockEntity.TYPE_ENERGY -> "ENERGY";
            case VolcaniteStoragePipeBlockEntity.TYPE_OMNI -> "OMNI";
            default -> "ITEMS";
        };
        if (isFiber) {
            boolean isImport = (mode == 1);
            headerColor = isImport ? 0x00E5FF : 0xFF9900;
            String connTitle = isImport ? "📥 IMPORT LINK" : "📤 EXPORT LINK";
            titleText = "§l" + connTitle + " §7[" + side.getName().toUpperCase() + "]";
        } else if (isConduit) {
            headerColor = switch (type) {
                case VolcaniteStoragePipeBlockEntity.TYPE_FLUID -> 0x00E5FF;
                case VolcaniteStoragePipeBlockEntity.TYPE_ENERGY -> 0xFF8800;
                case VolcaniteStoragePipeBlockEntity.TYPE_OMNI -> 0xD278FF;
                default -> 0x00FF7A;
            };
            String modeName = (mode == 0) ? "AUTO" : ((mode == 1) ? "IMPORT" : "EXPORT");
            titleText = "§l" + modeName + " · " + typeName + " §7[" + side.getName().toUpperCase() + "]";
        } else {
            headerColor = (!hasEnergy) ? 0xFFAA00 : ((type == 0) ? 0x00FF66 : 0x00D0FF);
            String modeName = (mode == 0) ? "CONDUIT AUTO" : ((mode == 1) ? "EXTRACT" : "INSERT");
            if (!this.menu.canSwitchType()) {
                titleText = "§lHermes " + modeName + " §7[" + side.getName().toUpperCase() + "]";
            } else {
                titleText = "§l" + modeName + " (" + typeName + ") §7[" + side.getName().toUpperCase() + "]";
            }
        }
        guiGraphics.drawString(this.font, Component.literal(titleText), 8, 5, headerColor, false);

        // 2. Machine recognition & Telemetry card (y = 51..72)
        int machineId = this.menu.getConnectedMachineBlockId();
        if (machineId > 0) {
            Block block = BuiltInRegistries.BLOCK.byId(machineId);
            String name = block.getName().getString();
            if (name.length() > 17) name = name.substring(0, 16) + "…";
            if (isFiber) {
                String flowArrow = (mode == 1) ? "§b" + name + " §f➔ §bNet" : "§6Net §f➔ §e" + name;
                guiGraphics.drawString(this.font, Component.literal(flowArrow), 11, 53, 0xFFFFFF, false);
            } else if (isConduit) {
                String arrow = switch (mode) {
                    case 1 -> "§6📥 " + name + " §f➔";
                    case 2 -> "§b📤 ➔ §f" + name;
                    default -> "§d⇄ " + name;
                };
                if (this.menu.getTargetHasEnergy()) arrow += " §e⚡";
                guiGraphics.drawString(this.font, Component.literal(arrow), 11, 53, 0xFFFFFF, false);
            } else if (hasEnergy && this.menu.getTargetHasEnergy()) {
                guiGraphics.drawString(this.font, Component.literal("§7Target: §e" + name + " §a⚡"), 11, 53, 0xFFFFAA, false);
            } else {
                guiGraphics.drawString(this.font, Component.literal("§7Target: §f" + name), 11, 53, 0xCCCCCC, false);
            }
        } else {
            guiGraphics.drawString(this.font, Component.literal("§7Target: §8[No Device on Face]"), 11, 53, 0x888888, false);
        }

        // Line 2 of Telemetry Card (y = 63)
        if (isConduit) {
            String energyStr = formatEnergyShort();
            int mb = this.menu.getStoredFluidMb();
            int fluidId = this.menu.getStoredFluidTypeId();
            String fluidPart = (mb > 0 && fluidId > 0)
                    ? ("§b💧" + mb + " " + getFluidShortName(fluidId))
                    : "§8💧Empty";
            guiGraphics.drawString(this.font, Component.literal("§6⚡" + energyStr + " §8| " + fluidPart), 11, 63, 0xEEEEEE, false);
        } else if (isFiber) {
            if (this.menu.isNetworkPowered()) {
                guiGraphics.drawString(this.font, Component.literal("§7Fiber Net: §aONLINE §8(No RF Needed)"), 11, 63, 0x55FF55, false);
            } else {
                guiGraphics.drawString(this.font, Component.literal("§7Fiber Net: §cOFFLINE §8(Need Ctrl)"), 11, 63, 0xFF5555, false);
            }
        } else if (hasEnergy) {
            if (this.menu.isNetworkPowered()) {
                guiGraphics.drawString(this.font, Component.literal("§7Net RF: §aONLINE §7(" + this.menu.getNetworkEnergyPercent() + "%)"), 11, 63, 0x55FF55, false);
            } else {
                guiGraphics.drawString(this.font, Component.literal("§7Net RF: §cOFFLINE §4(No Power)"), 11, 63, 0xFF5555, false);
            }
        } else {
            guiGraphics.drawString(this.font, Component.literal("§7Status: §aDirect Passive Transfer"), 11, 63, 0x66DD88, false);
        }

        // 3. Priority badge between - and + (x = 22..46, y = 78)
        int prio = this.menu.getPriority();
        String prioColor = prio > 0 ? "§a+" + prio : (prio < 0 ? "§c" + prio : "§e0");
        String prioStr = "P:" + prioColor;
        int prioWidth = this.font.width(Component.literal(prioStr));
        guiGraphics.drawString(this.font, Component.literal(prioStr), 34 - prioWidth / 2, 78, 0xFFFFFF, false);

        // 4. Tier badge next to Upgrade slot (x = 132, y = 101)
        int tier = this.menu.getEnergyUpgradeTier();
        if (tier > 0) {
            String tierBadge = switch (tier) {
                case 1 -> "§eT1";
                case 2 -> "§bT2";
                default -> "§dT3";
            };
            guiGraphics.drawString(this.font, Component.literal(tierBadge), 132, 101, 0xFFFFFF, false);
        }

        // 5. Player Inventory label (y = 127)
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 127, 0xCECAC5, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Subtle glowing highlight around active face tab & connected indicator pips
        int curFace = this.menu.getSide().get3DDataValue();
        int connMask = this.menu.getConnectedFacesMask();
        for (int i = 0; i < 6; i++) {
            int fx = x + 8 + i * 21;
            int fy = y + 16;
            if (i == curFace) {
                guiGraphics.fill(fx, fy - 1, fx + 20, fy, 0xFFFFCC00);
                guiGraphics.fill(fx, fy + 14, fx + 20, fy + 15, 0xFFFFCC00);
            } else if ((connMask & (1 << i)) != 0) {
                guiGraphics.fill(fx + 3, fy + 13, fx + 17, fy + 14, 0xFF00E588);
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Face selector tooltips
        for (int i = 0; i < 6; i++) {
            int fx = x + 8 + i * 21;
            int fy = y + 16;
            if (mouseX >= fx && mouseX <= fx + 20 && mouseY >= fy && mouseY <= fy + 14) {
                Direction d = Direction.from3DDataValue(i);
                boolean connected = (connMask & (1 << i)) != 0;
                boolean configured = (this.menu.getActiveFacesMask() & (1 << i)) != 0;
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                        Component.literal("§eFace: §f" + d.getName().toUpperCase()),
                        Component.literal(connected ? "§a● Connected" : "§8○ Not Connected"),
                        Component.literal(configured ? "§b◆ Explicit Import/Export" : "§7◆ Auto / Passive")
                ), mouseX, mouseY);
                return;
            }
        }

        // Sync ALL button tooltip
        if (mouseX >= x + 136 && mouseX <= x + 168 && mouseY >= y + 16 && mouseY <= y + 30) {
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("§d⟳ Sync Channel to ALL Faces"),
                    Component.literal("§7Applies current channel type to all 6 faces of this conduit.")
            ), mouseX, mouseY);
        }
        // Mode Button tooltip
        else if (mouseX >= x + 8 && mouseX <= x + 86 && mouseY >= y + 33 && mouseY <= y + 49) {
            int mode = this.menu.getModuleMode();
            if (mode == 0) {
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                        Component.literal("§d⇄ BIDIRECTIONAL (AUTO)"),
                        Component.literal("§7Balances RF energy & fluids automatically."),
                        Component.literal("§eClick to set IMPORT (Pull from device).")
                ), mouseX, mouseY);
            } else if (mode == 1) {
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                        Component.literal("§6📥 IMPORT / EXTRACT (Pull)"),
                        Component.literal("§7Pulls from connected container into conduit network."),
                        Component.literal("§eClick to set EXPORT (Push).")
                ), mouseX, mouseY);
            } else {
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                        Component.literal("§b📤 EXPORT / INSERT (Push)"),
                        Component.literal("§7Pushes from conduit network into connected container."),
                        Component.literal("§eClick to cycle mode.")
                ), mouseX, mouseY);
            }
        }
        // Type Button tooltip
        else if (mouseX >= x + 90 && mouseX <= x + 168 && mouseY >= y + 33 && mouseY <= y + 49) {
            if (!this.menu.canSwitchType()) {
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                        Component.literal("§aITEMS Mode §7(Fixed)"),
                        Component.literal("§7This pipe exclusively transfers items.")
                ), mouseX, mouseY);
            } else {
                int type = this.menu.getTransferType();
                String titleTip = switch (type) {
                    case VolcaniteStoragePipeBlockEntity.TYPE_FLUID -> "§b💧 FLUIDS Channel";
                    case VolcaniteStoragePipeBlockEntity.TYPE_ENERGY -> "§6⚡ ENERGY Channel";
                    case VolcaniteStoragePipeBlockEntity.TYPE_OMNI -> "§d🌐 OMNI Channel (All-In-One)";
                    default -> "§a📦 ITEMS Channel";
                };
                String descTip = switch (type) {
                    case VolcaniteStoragePipeBlockEntity.TYPE_FLUID -> "§7Transfers liquids, steam, bio-oil & diesels.";
                    case VolcaniteStoragePipeBlockEntity.TYPE_ENERGY -> "§7Dedicated high-throughput RF power conduit.";
                    case VolcaniteStoragePipeBlockEntity.TYPE_OMNI -> "§7Simultaneously transfers Items, Fluids & RF Energy!";
                    default -> "§7Transfers solid items & materials.";
                };
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                        Component.literal(titleTip),
                        Component.literal(descTip),
                        Component.literal("§eClick to cycle channel & pipe color.")
                ), mouseX, mouseY);
            }
        }
        // Priority tooltips
        else if (mouseX >= x + 8 && mouseX <= x + 61 && mouseY >= y + 74 && mouseY <= y + 89) {
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("§eTarget Priority: §f" + this.menu.getPriority() + " §8(-5 .. +5)"),
                    Component.literal("§7Higher priority EXPORT faces receive items/fluids first.")
            ), mouseX, mouseY);
        }
        // Round-Robin tooltip
        else if (mouseX >= x + 63 && mouseX <= x + 115 && mouseY >= y + 74 && mouseY <= y + 89) {
            if (this.menu.isRoundRobin()) {
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                        Component.literal("§e⟳ ROUND-ROBIN Routing"),
                        Component.literal("§7Distributes evenly across connected destinations.")
                ), mouseX, mouseY);
            } else {
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                        Component.literal("§a🎯 PRIORITY + NEAREST Routing"),
                        Component.literal("§7Fills highest-priority / closest targets first.")
                ), mouseX, mouseY);
            }
        }
        // Redstone tooltip
        else if (mouseX >= x + 117 && mouseX <= x + 168 && mouseY >= y + 74 && mouseY <= y + 89) {
            int rs = this.menu.getRedstoneMode();
            String rsTitle = switch (rs) {
                case 1 -> "§c⚡ REDSTONE HIGH";
                case 2 -> "§b🌙 REDSTONE LOW";
                default -> "§a☀ ALWAYS ACTIVE";
            };
            String rsDesc = switch (rs) {
                case 1 -> "§7Transfers only while powered by Redstone.";
                case 2 -> "§7Transfers only when NOT powered by Redstone.";
                default -> "§7Ignores Redstone signal and runs continuously.";
            };
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal(rsTitle),
                    Component.literal(rsDesc)
            ), mouseX, mouseY);
        }
        // Filter Mode tooltip
        else if (mouseX >= x + 71 && mouseX <= x + 113 && mouseY >= y + 96 && mouseY <= y + 114) {
            if (this.menu.isFilterBlacklist()) {
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                        Component.literal("§cBLACKLIST Mode"),
                        Component.literal("§7Blocks items/fluids matching the filter slots.")
                ), mouseX, mouseY);
            } else {
                guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                        Component.literal("§aWHITELIST Mode"),
                        Component.literal("§7Allows ONLY items/fluids matching the filter slots.")
                ), mouseX, mouseY);
            }
        }
        // Clear Filter tooltip
        else if (mouseX >= x + 114 && mouseX <= x + 129 && mouseY >= y + 96 && mouseY <= y + 114) {
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("§cClear Filters"),
                    Component.literal("§7Clears all 3 ghost filter slots on this face.")
            ), mouseX, mouseY);
        }
        // 3 Ghost Filter Slots tooltip
        for (int i = 0; i < 3; i++) {
            int slotX = x + 11 + i * 19;
            int slotY = y + 96;
            if (mouseX >= slotX && mouseX <= slotX + 18 && mouseY >= slotY && mouseY <= slotY + 18) {
                if (this.menu.getSlot(i).getItem().isEmpty()) {
                    guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                            Component.literal("§eSmart Ghost Filter #" + (i + 1)),
                            Component.literal("§7Drop any Item or Bucket (Water/Lava/Biofuel)"),
                            Component.literal("§7to filter both items & fluids without consuming it.")
                    ), mouseX, mouseY);
                }
            }
        }
        // Upgrade slot tooltip (slot 3 at 146, 97)
        if (mouseX >= x + 145 && mouseX <= x + 163 && mouseY >= y + 96 && mouseY <= y + 114) {
            if (this.menu.getSlot(3).getItem().isEmpty()) {
                if (this.menu.isOpticFiber()) {
                    guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                            Component.literal("§b★ Fiber Connector Speed Slot"),
                            Component.literal("§7Insert §dVolcanite Stack §7or §bOverclock Upgrade.")
                    ), mouseX, mouseY);
                } else {
                    guiGraphics.renderComponentTooltip(this.font, java.util.List.of(
                            Component.literal("§d★ Face Speed & Energy Upgrade Slot"),
                            Component.literal("§7Accepts §6Energy Upgrades§7, §bOverclock§7, or §dStack Upgrades"),
                            Component.literal("§7to boost items, fluids & RF throughput on this face.")
                    ), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        VolcaniteGuiTheme.renderExteriorFrame(guiGraphics, x, y, this.imageWidth, this.imageHeight, false);

        boolean isFiber = this.menu.isOpticFiber();
        boolean isConduit = this.menu.isUniversalConduit();
        int mode = this.menu.getModuleMode();
        int type = this.menu.getTransferType();

        int accentColor;
        int darkTrim;
        int cardBg;
        int cardBorder;
        if (isFiber) {
            if (mode == 1) {
                accentColor = 0xFF00E5FF;
                darkTrim = 0xFF003B4D;
                cardBg = 0xFF0D1B24;
                cardBorder = 0xFF16495E;
            } else {
                accentColor = 0xFFFF9000;
                darkTrim = 0xFF4D2600;
                cardBg = 0xFF24160D;
                cardBorder = 0xFF5E3816;
            }
        } else if (isConduit) {
            switch (type) {
                case VolcaniteStoragePipeBlockEntity.TYPE_FLUID -> {
                    accentColor = 0xFF00E5FF;
                    darkTrim = 0xFF003A4D;
                    cardBg = 0xFF0D1B24;
                    cardBorder = 0xFF164D63;
                }
                case VolcaniteStoragePipeBlockEntity.TYPE_ENERGY -> {
                    accentColor = 0xFFFF6E00;
                    darkTrim = 0xFF4D1A00;
                    cardBg = 0xFF23130C;
                    cardBorder = 0xFF662E14;
                }
                case VolcaniteStoragePipeBlockEntity.TYPE_OMNI -> {
                    accentColor = 0xFFC864FF;
                    darkTrim = 0xFF3A1652;
                    cardBg = 0xFF1B1224;
                    cardBorder = 0xFF4E2A6E;
                }
                default -> {
                    accentColor = 0xFF00FF7A;
                    darkTrim = 0xFF003B1D;
                    cardBg = 0xFF0F1C15;
                    cardBorder = 0xFF1A5233;
                }
            }
        } else {
            accentColor = (type == 0) ? 0xFF00FF66 : 0xFF00BFFF;
            darkTrim = (type == 0) ? 0xFF003816 : 0xFF002844;
            cardBg = 0xFF1B1622;
            cardBorder = 0xFF352B44;
        }

        // Base obsidian background
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF141216);

        // Dynamic channel accent border
        guiGraphics.fill(x, y, x + this.imageWidth, y + 2, accentColor);
        guiGraphics.fill(x, y + this.imageHeight - 2, x + this.imageWidth, y + this.imageHeight, accentColor);
        guiGraphics.fill(x, y, x + 2, y + this.imageHeight, accentColor);
        guiGraphics.fill(x + this.imageWidth - 2, y, x + this.imageWidth, y + this.imageHeight, accentColor);

        // Target & Telemetry card (y = 51..72)
        guiGraphics.fill(x + 7, y + 51, x + this.imageWidth - 7, y + 72, cardBg);
        guiGraphics.fill(x + 7, y + 51, x + this.imageWidth - 7, y + 52, cardBorder);
        guiGraphics.fill(x + 7, y + 71, x + this.imageWidth - 7, y + 72, cardBorder);

        // Priority pill background (between - and + at x+23..x+46, y+74..y+89)
        guiGraphics.fill(x + 23, y + 74, x + 46, y + 89, 0xFF0F0D14);
        guiGraphics.fill(x + 23, y + 74, x + 46, y + 75, cardBorder);
        guiGraphics.fill(x + 23, y + 88, x + 46, y + 89, cardBorder);

        // Filter & Upgrade panel (y = 91..120)
        guiGraphics.fill(x + 6, y + 91, x + this.imageWidth - 6, y + 120, cardBg);
        guiGraphics.fill(x + 6, y + 91, x + this.imageWidth - 6, y + 92, cardBorder);
        guiGraphics.fill(x + 6, y + 119, x + this.imageWidth - 6, y + 120, darkTrim);

        // 3 Ghost Filter Slots at (11 + i * 19, 96) matching Slot(12 + i * 19, 97)
        int filterBorder = this.menu.isFilterBlacklist() ? 0xFFFF4444 : 0xFF00EE88;
        for (int i = 0; i < 3; i++) {
            int sx = x + 11 + i * 19;
            int sy = y + 96;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, filterBorder);
            guiGraphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF0D0A10);
        }

        // Upgrade slot highlight box at (145, 96) matching Slot(146, 97)
        int upgradeBorderColor = switch (this.menu.getEnergyUpgradeTier()) {
            case 1 -> 0xFFFFAA00;
            case 2 -> 0xFF00DDFF;
            case 3 -> 0xFFFF55FF;
            default -> this.menu.supportsEnergy() ? 0xFF554433 : 0xFF6A3D82;
        };
        guiGraphics.fill(x + 145, y + 96, x + 163, y + 114, upgradeBorderColor);
        guiGraphics.fill(x + 146, y + 97, x + 162, y + 113, 0xFF140E16);
        if (this.menu.getSlot(3).getItem().isEmpty()) {
            guiGraphics.drawString(this.font, "✦", x + 151, y + 101, 0xFF9E65BA, false);
        }

        // Lower panel (Player Inventory Area, y = 123..218)
        guiGraphics.fill(x + 4, y + 123, x + this.imageWidth - 4, y + this.imageHeight - 4, 0xFF18151D);

        // Player inventory slot frames matching (8 + col * 18, 138 + row * 18)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int sx = x + 7 + col * 18;
                int sy = y + 137 + row * 18;
                guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF352F40);
                guiGraphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF100E14);
            }
        }

        // Hotbar slot frames matching (8 + col * 18, 196)
        for (int col = 0; col < 9; ++col) {
            int sx = x + 7 + col * 18;
            int sy = y + 195;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF352F40);
            guiGraphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF100E14);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
