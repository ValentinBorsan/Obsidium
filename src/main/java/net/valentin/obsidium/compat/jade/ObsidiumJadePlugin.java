package net.valentin.obsidium.compat.jade;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.valentin.obsidium.block.*;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import java.util.List;

/**
 * Optional Jade integration, loaded through the "jade" entrypoint only when Jade is installed.
 */
@WailaPlugin
public class ObsidiumJadePlugin implements IWailaPlugin {

    private record Machine(Class<? extends Block> block, Class<? extends BlockEntity> blockEntity) {}

    private static final List<Machine> MACHINES = List.of(
            new Machine(VolcaniteGrinderBlock.class, VolcaniteGrinderBlockEntity.class),
            new Machine(VolcaniteFurnaceBlock.class, VolcaniteFurnaceBlockEntity.class),
            new Machine(AdvancedVolcaniteFurnaceBlock.class, AdvancedVolcaniteFurnaceBlockEntity.class),
            new Machine(DionysianDistilleryBlock.class, DionysianDistilleryBlockEntity.class),
            new Machine(DionysianPressBlock.class, DionysianPressBlockEntity.class),
            new Machine(DionysianAgingBarrelBlock.class, DionysianAgingBarrelBlockEntity.class),
            new Machine(HydrothermalVitrifierBlock.class, HydrothermalVitrifierBlockEntity.class),
            new Machine(HydrothermalCondenserBlock.class, HydrothermalCondenserBlockEntity.class),
            new Machine(HydrothermalLithofierBlock.class, HydrothermalLithofierBlockEntity.class),
            new Machine(MagmaticCrucibleBlock.class, MagmaticCrucibleBlockEntity.class),
            new Machine(VolcanicDistillationChamberBlock.class, VolcanicDistillationChamberBlockEntity.class),
            new Machine(MagmaForgeCoreBlock.class, MagmaForgeCoreBlockEntity.class),
            new Machine(DemetrianComposterBlock.class, DemetrianComposterBlockEntity.class)
    );

    @Override
    public void register(IWailaCommonRegistration registration) {
        for (Machine machine : MACHINES) {
            registration.registerBlockDataProvider(MachineProgressProvider.INSTANCE, machine.blockEntity());
        }
        registration.registerBlockDataProvider(OlympianRitualProvider.INSTANCE, OlympianAltarBlockEntity.class);
        registration.registerBlockDataProvider(OlympianRitualProvider.INSTANCE, AstrapeRitualObeliskBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        for (Machine machine : MACHINES) {
            registration.registerBlockComponent(MachineProgressProvider.INSTANCE, machine.block());
        }
        registration.registerBlockComponent(OlympianRitualProvider.INSTANCE, OlympianAltarBlock.class);
        registration.registerBlockComponent(OlympianRitualProvider.INSTANCE, AstrapeRitualObeliskBlock.class);
    }
}
