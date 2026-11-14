package net.valentin.obsidium.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

/**
 * Manages client-side audio settings for Obsidium machine sounds.
 * Allows toggling mute and adjusting machine volume independently of Minecraft's master volume.
 * Persists settings to config/obsidium_sound.properties.
 */
public final class MachineSoundConfig {
    private static final File CONFIG_FILE = new File(Minecraft.getInstance().gameDirectory, "config/obsidium_sound.properties");

    private static boolean muted = false;
    private static float machineVolume = 1.0f; // 0.0 to 1.0
    private static boolean loaded = false;

    private MachineSoundConfig() {}

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (!CONFIG_FILE.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            Properties props = new Properties();
            props.load(reader);
            muted = Boolean.parseBoolean(props.getProperty("machine_muted", "false"));
            machineVolume = Float.parseFloat(props.getProperty("machine_volume", "1.0"));
            machineVolume = Math.max(0.0f, Math.min(1.0f, machineVolume));
        } catch (Exception e) {
            // fallback to defaults
        }
    }

    public static synchronized void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                Properties props = new Properties();
                props.setProperty("machine_muted", Boolean.toString(muted));
                props.setProperty("machine_volume", Float.toString(machineVolume));
                props.store(writer, "Obsidium Machine Sound Configuration");
            }
        } catch (Exception e) {
            // ignore save errors
        }
    }

    public static boolean isMuted() {
        ensureLoaded();
        return muted;
    }

    public static void setMuted(boolean value) {
        ensureLoaded();
        muted = value;
        save();
    }

    public static boolean toggleMute() {
        ensureLoaded();
        muted = !muted;
        save();
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                Component.literal(muted ? "§6[Obsidium] §cMachine Sounds: MUTED §7(Click sound icon in GUI to unmute)"
                                        : "§6[Obsidium] §aMachine Sounds: UNMUTED §7(100% volume)"),
                true
            );
        }
        return muted;
    }

    public static float getEffectiveVolume(String soundPath) {
        ensureLoaded();
        if (muted) return 0.0f;
        return machineVolume;
    }
}

