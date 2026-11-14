package net.valentin.obsidium.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class ClientHooks {
    public static void openManualScreen() {
        Minecraft.getInstance().setScreen(new MagmaForgeManualScreen());
    }

    public static void openBotanyBookScreen() {
        Minecraft.getInstance().setScreen(new DemetrianBotanyScreen());
    }
}
