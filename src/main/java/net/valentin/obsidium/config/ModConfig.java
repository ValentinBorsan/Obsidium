package net.valentin.obsidium.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("Obsidium/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "obsidium.json");

    private static ModConfig INSTANCE = new ModConfig();

    public static ModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new ModConfig();
                }
                LOGGER.info("Obsidium: Configuration loaded successfully from obsidium.json");
            } catch (Exception e) {
                LOGGER.error("Obsidium: Failed to read obsidium.json, falling back to default values.", e);
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
    }

    public static void save() {
        try {
            File parent = CONFIG_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(INSTANCE, writer);
                LOGGER.info("Obsidium: Configuration generated and saved at {}", CONFIG_FILE.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("Obsidium: Failed to save obsidium.json", e);
        }
    }

    // ==========================================
    // 1. Storage & Terminal Network
    // ==========================================
    public int remoteTerminalRange = 256;
    public boolean remoteTerminalCrossDimension = false;

    // ==========================================
    // 2. Energy & Machine Balancing
    // ==========================================
    public long chargingPlateCapacity = 1_000_000L;
    public long chargingPlateMaxReceive = 50_000L;
    public long chargingPlateTransferRate = 50_000L;
    public long basicBatteryCapacity = 500_000L;
    public long advancedBatteryCapacity = 5_000_000L;

    // Modpacks can tune rare renewable production without changing common crops.
    public int rareResourceGrowthMultiplier = 4;
    public int getRareResourceGrowthMultiplier() {
        return Math.max(1, Math.min(64, rareResourceGrowthMultiplier));
    }

    public int solarGeneratorMiddayRate = 80;
    public int steamGeneratorOutputRate = 120;
    public int biomassGeneratorOutputRate = 100;

    // ==========================================
    // 3. Olympian Altar & Divine Pacts
    // ==========================================
    public int altarRitualDurationTicks = 65;
    public boolean zeusSummonsTrueLightning = true;
    public float zeusChainLightningChance = 0.35F;
    public float athenaShieldBlockChance = 0.25F;
    public float thanatosSoulSiphonHeal = 3.0F;

    // ==========================================
    // 4. World Generation (Ores Control)
    // ==========================================
    public boolean enableTinOreGen = true;
    public boolean enableLeadOreGen = true;
    public boolean enableSilverOreGen = true;
    public boolean enableVolcaniteOreGen = true;
}

