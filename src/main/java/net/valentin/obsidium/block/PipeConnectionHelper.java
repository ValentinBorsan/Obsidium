package net.valentin.obsidium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.valentin.obsidium.ObsidiumMod;

public class PipeConnectionHelper {
    /**
     * Pipe-to-pipe edges honor both endpoints' disconnect settings. Existing
     * storage components keep their established traversal behavior.
     */
    public static boolean canTraverseNetwork(BlockGetter level, BlockPos from, Direction direction) {
        if (level == null || from == null || direction == null) return false;
        BlockPos to = from.relative(direction);
        BlockEntity fromEntity = level.getBlockEntity(from);
        BlockEntity toEntity = level.getBlockEntity(to);
        boolean fromPipe = isLogisticsPipe(fromEntity);
        boolean toPipe = isLogisticsPipe(toEntity);
        if (!fromPipe && !toPipe) return true; // preserve traversal through existing network components
        if (fromEntity instanceof VolcaniteStoragePipeBlockEntity pipe && pipe.isSideDisconnected(direction)) return false;
        if (toEntity instanceof VolcaniteStoragePipeBlockEntity pipe && pipe.isSideDisconnected(direction.getOpposite())) return false;
        return true;
    }

    public static boolean isLogisticsPipe(BlockEntity entity) {
        return entity instanceof VolcaniteStoragePipeBlockEntity
                || entity instanceof VolcaniteUniversalConduitBlockEntity;
    }

    public static boolean isLogisticsPipe(Block block) {
        return block instanceof VolcaniteStoragePipeBlock;
    }

    public static boolean isStorageNetworkBlock(Block block) {
        return block instanceof VolcaniteStorageControllerBlock ||
               block instanceof VolcaniteStorageUnitBlock ||
               block instanceof VolcaniteModularChestBlock ||
               block instanceof VolcaniteChestBlock ||
               block instanceof VolcaniteStorageDiskDriveBlock ||
               block instanceof VolcaniteStorageInterfaceBlock ||
               block instanceof VolcaniteCrafterInterfaceBlock ||
               block instanceof VolcaniteLiquidStorageTankBlock ||
               block instanceof LiquidTankBlock ||
               block instanceof VolcaniteNetworkNodeEmitterBlock ||
               block instanceof VolcaniteNetworkNodeReceiverBlock ||
               block instanceof VolcaniteSortingStationBlock ||
               block instanceof HephaesteanArmoryBlock;
    }

    public static boolean canOpticFiberConnect(BlockGetter level, BlockPos neighborPos, Direction dir, VolcaniteStoragePipeBlockEntity fiberBe) {
        if (level == null || neighborPos == null) return false;
        BlockState state = level.getBlockState(neighborPos);
        if (state.isAir()) return false;
        Block block = state.getBlock();

        // 1. Always connects to other Optic Fibers / Logistics Pipes / Storage Network blocks (including Grand Chest & Modular Chest)
        if (isLogisticsPipe(block) || isStorageNetworkBlock(block)) {
            return true;
        }

        // 2. Connects to external chests, tanks, or machines ONLY if an Import or Export Connector is mounted on this face!
        if (fiberBe != null && dir != null && fiberBe.getModule(dir) != VolcaniteStoragePipeBlockEntity.ModuleType.NONE) {
            return canConnect(level, neighborPos, dir);
        }

        return false;
    }

    public static boolean canConnect(BlockGetter level, BlockPos pos, Direction dir) {
        if (level == null || pos == null) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        Block block = state.getBlock();

        // 1. Volcanite logistics pipes and universal conduits share a network.
        if (isLogisticsPipe(block)) {
            return true;
        }

        // 2. All Liquid Tanks & Fluid Producers
        if (block instanceof LiquidTankBlock ||
            block instanceof VolcaniteLavaPumpBlock ||
            block instanceof VolcaniteGeneratorBlock ||
            block instanceof VolcaniteSteamGeneratorBlock ||
            block instanceof VolcaniteGeothermalGeneratorBlock ||
            block instanceof GeothermalArboristBlock) {
            return true;
        }

        // 3. All Storage, Wireless Nodes & Sorting Stations
        if (isStorageNetworkBlock(block)) {
            return true;
        }

        // 4. All Machines & Forges
        if (block instanceof VolcaniteFurnaceBlock ||
            block instanceof AdvancedVolcaniteFurnaceBlock ||
            block instanceof VolcaniteGrinderBlock ||
            block instanceof MagmaForgeCoreBlock ||
            block instanceof MagmaForgeDummyBlock ||
            block instanceof DionysianBioCalderaBlock ||
            block instanceof DionysianBioCalderaPartBlock ||
            block instanceof DemetrianBiomassGeneratorBlock ||
            block instanceof DemetrianComposterBlock ||
            block instanceof DemetrianBonsaiPlanterBlock ||
            block instanceof DemetrianSaplingBarrelBlock ||
            block instanceof DemetrianLumberDepotBlock ||
            block instanceof DionysianAgingBarrelBlock ||
            block instanceof DionysianDistilleryBlock ||
            block instanceof DionysianPressBlock ||
            block instanceof HydrothermalCondenserBlock ||
            block instanceof HydrothermalLithofierBlock ||
            block instanceof HydrothermalVitrifierBlock ||
            block instanceof MagmaticCrucibleBlock ||
            block instanceof VolcanicDistillationChamberBlock ||
            block instanceof TartarusPistonEngineBlock ||
            block instanceof TartarusPistonEnginePartBlock ||
            block instanceof ThanatosExecutionerBlock ||
            block instanceof ApolloSolarGeneratorBlock ||
            block instanceof SupremeVolcanicGeneratorBlock) {
            return true;
        }

        // 5. Energy Cells & Units & Cores & Induction Charging Plates
        if (block instanceof VolcaniteEnergyCellBlock ||
            block instanceof VolcaniteEnergyUnitBlock ||
            block instanceof VolcaniteEnergyCoreBlock ||
            block instanceof HephaesteanChargingPlateBlock ||
            block instanceof AstrapeEnergyPylonBlock ||
            block == ObsidiumMod.VOLCANITE_CASING_BLOCK ||
            block == ObsidiumMod.ADVANCED_VOLCANITE_CASING_BLOCK ||
            block == ObsidiumMod.VOLCANITE_GLASS_BLOCK) {
            return true;
        }

        // 6. Vanilla Containers & Furnaces
        if (block instanceof net.minecraft.world.level.block.ChestBlock ||
            block instanceof net.minecraft.world.level.block.BarrelBlock ||
            block instanceof net.minecraft.world.level.block.HopperBlock ||
            block instanceof net.minecraft.world.level.block.ShulkerBoxBlock ||
            block instanceof net.minecraft.world.level.block.AbstractFurnaceBlock ||
            block instanceof net.minecraft.world.level.block.DispenserBlock ||
            block instanceof net.minecraft.world.level.block.DropperBlock ||
            block instanceof net.minecraft.world.level.block.BrewingStandBlock) {
            return true;
        }

        // 7. Dynamic BlockEntity checks (Container, InventoryHolder or Fluid/Item/Energy storage)
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container || be instanceof InventoryHolder) return true;
        if (level instanceof net.minecraft.world.level.Level world && dir != null) {
            Direction opposite = dir.getOpposite();
            if (net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(world, pos, opposite) != null) return true;
            if (net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(world, pos, opposite) != null) return true;
            if (team.reborn.energy.api.EnergyStorage.SIDED.find(world, pos, opposite) != null) return true;
        }

        return false;
    }
}
