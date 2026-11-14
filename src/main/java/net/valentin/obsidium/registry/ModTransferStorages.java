package net.valentin.obsidium.registry;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.valentin.obsidium.block.*;
import net.valentin.obsidium.item.*;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyItem;

import static net.valentin.obsidium.ObsidiumMod.*;

public final class ModTransferStorages {
	private ModTransferStorages() {}

	public static void register() {
		// Sided Energy Storages
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, KERAUNOS_HV_CORE_BLOCK_ENTITY);
		EnergyStorage.SIDED.registerForBlocks((level, pos, state, blockEntity, direction) -> {
			KeraunosHVCoreBlockEntity core = KeraunosHVPortBlock.findCore(level, pos);
			return (core != null && core.isFormed()) ? core.energyStorage : null;
		}, KERAUNOS_HV_PORT_BLOCK);
		EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be.energyStorage, OLYMPIAN_HADRON_COLLIDER_BE);
		EnergyStorage.SIDED.registerForBlocks((level, pos, state, blockEntity, direction) -> {
			OlympianHadronColliderBlockEntity collider = SynchrotronRingCoilBlock.findController(level, pos);
			return (collider != null && collider.isFormed()) ? collider.energyStorage : null;
		}, SYNCHROTRON_RING_COIL_BLOCK, HADRON_ENERGY_PORT_BLOCK);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, VOLCANITE_GENERATOR_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, VOLCANITE_GRINDER_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, VOLCANITE_FURNACE_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, ADVANCED_VOLCANITE_FURNACE_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, VOLCANITE_ENERGY_WIRE_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.sidedEnergyStorage, HEPHAESTEAN_CHARGING_PLATE_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, ASTRAPE_ENERGY_PYLON_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, GEOTHERMAL_ARBORIST_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, VOLCANITE_STEAM_GENERATOR_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, VOLCANITE_GEOTHERMAL_GENERATOR_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, SUPREME_VOLCANIC_GENERATOR_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, VOLCANIC_DISTILLATION_CHAMBER_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, DEMETRIAN_BIOMASS_GENERATOR_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, DIONYSIAN_PRESS_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, ADVANCED_DIONYSIAN_PRESS_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, VOLCANITE_STORAGE_CONTROLLER_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyForwarder, VOLCANITE_NETWORK_NODE_RECEIVER_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, VOLCANITE_LIQUID_STORAGE_TANK_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, MAGMA_FORGE_CORE_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> ((HydrothermalLithofierBlockEntity) blockEntity).energyStorage, HYDROTHERMAL_LITHOFIER_BE);
		EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be.energyStorage, TARTARUS_PISTON_ENGINE_BE);
		EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be.energyStorage, VOLCANIC_DIESEL_GENERATOR_BE);
		EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be.energyStorage, DIONYSIAN_BIO_CALDERA_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, VOLCANITE_LAVA_PUMP_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, VOLCANITE_ENERGY_CORE_BE);
		EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be.energyStorage, THANATOS_EXECUTIONER_BE);
		EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be.energyStorage, APOLLO_SOLAR_GENERATOR_BE);
		EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be.energyStorage, AEOLUS_STORM_TURBINE_BE);
		EnergyStorage.SIDED.registerForBlockEntity((CreativeEnergyCoreBlockEntity blockEntity, Direction direction) -> blockEntity.energyStorage, CREATIVE_ENERGY_CORE_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, BASIC_VOLCANIC_TELEPORTER_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, VOLCANIC_CORE_TELEPORTER_BE);
		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energyStorage, VOLCANITE_UNIVERSAL_CONDUIT_BE);

		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> {
			if (blockEntity.getLevel() != null) {
				VolcaniteEnergyCoreBlockEntity formedCore = VolcaniteEnergyCoreBlockEntity.findFormedCoreForRingBlock(blockEntity.getLevel(), blockEntity.getBlockPos());
				if (formedCore != null) return formedCore.energyStorage;
			}
			return blockEntity.getSidedEnergyStorage(direction);
		}, VOLCANITE_ENERGY_CELL_BE);

		EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> {
			if (blockEntity.getLevel() != null) {
				VolcaniteEnergyCoreBlockEntity formedCore = VolcaniteEnergyCoreBlockEntity.findFormedCoreForRingBlock(blockEntity.getLevel(), blockEntity.getBlockPos());
				if (formedCore != null) return formedCore.energyStorage;
			}
			return blockEntity.getSidedEnergyStorage(direction);
		}, VOLCANITE_ENERGY_UNIT_BE);

		// Sided Fluid Storages (Direct O(1) BlockEntity registrations)
		FluidStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.fluidStorage, VOLCANITE_STEAM_GENERATOR_BE);
		FluidStorage.SIDED.registerForBlockEntity((chamber, direction) -> new SingleFluidStorage() {
			@Override
			protected long getCapacity(FluidVariant variant) {
				return VolcanicDistillationChamberBlockEntity.MAX_LAVA * 81L;
			}

			@Override
			public long getAmount() {
				if (chamber.getDistillateAmount() > 0) return chamber.getDistillateAmount() * 81L;
				if (chamber.getLavaAmount() > 0) return chamber.getLavaAmount() * 81L;
				return chamber.getEthanolAmount() * 81L;
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				long maxInsert_mB = maxAmount / 81L;
				int toInsert_mB = chamber.insertFluid(resource, (int) maxInsert_mB, true);
				if (toInsert_mB > 0) {
					transaction.addCloseCallback((t, result) -> {
						if (result.wasCommitted()) {
							chamber.insertFluid(resource, toInsert_mB, false);
						}
					});
				}
				return toInsert_mB * 81L;
			}

			@Override
			public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (chamber.getDistillateAmount() <= 0 || !chamber.getOutputFluid().equals(resource)) return 0;
				long maxExtract_mB = maxAmount / 81L;
				int toExtract_mB = chamber.extractDistillate(resource, (int) maxExtract_mB, true);
				if (toExtract_mB > 0) {
					transaction.addCloseCallback((t, result) -> {
						if (result.wasCommitted()) {
							chamber.extractDistillate(resource, toExtract_mB, false);
						}
					});
				}
				return toExtract_mB * 81L;
			}

			@Override
			public boolean isResourceBlank() {
				return chamber.getDistillateAmount() <= 0 && chamber.getLavaAmount() <= 0 && chamber.getEthanolAmount() <= 0;
			}

			@Override
			public FluidVariant getResource() {
				if (chamber.getDistillateAmount() > 0) return chamber.getOutputFluid();
				if (chamber.getLavaAmount() > 0) return chamber.getInputFluid();
				if (chamber.getEthanolAmount() > 0) return chamber.getSecondaryFluid();
				return FluidVariant.blank();
			}
		}, VOLCANIC_DISTILLATION_CHAMBER_BE);
		FluidStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.fluidStorage, MAGMA_FORGE_CORE_BE);
		FluidStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity, VOLCANITE_LIQUID_STORAGE_TANK_BE);
		FluidStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> ((HydrothermalVitrifierBlockEntity) blockEntity).fluidStorage, HYDROTHERMAL_VITRIFIER_BE);
		FluidStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> ((MagmaticCrucibleBlockEntity) blockEntity).fluidStorage, MAGMATIC_CRUCIBLE_BE);
		FluidStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> ((HydrothermalCondenserBlockEntity) blockEntity).fluidStorage, HYDROTHERMAL_CONDENSER_BE);
		FluidStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> ((HydrothermalLithofierBlockEntity) blockEntity).fluidStorage, HYDROTHERMAL_LITHOFIER_BE);
		FluidStorage.SIDED.registerForBlockEntity((be, dir) -> be, TARTARUS_PISTON_ENGINE_BE);
		FluidStorage.SIDED.registerForBlockEntity((caldera, dir) -> new SingleFluidStorage() {
			@Override
			protected long getCapacity(FluidVariant variant) {
				return DionysianBioCalderaBlockEntity.MAX_BIOGAS * 81L;
			}

			@Override
			public long getAmount() {
				return caldera.getBioOilAmount() * 81L;
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				return 0;
			}

			@Override
			public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (!resource.isOf(BIO_OIL_FLUID) || caldera.getBioOilAmount() <= 0) return 0;
				long maxExtract_mB = maxAmount / 81L;
				int toExtract_mB = caldera.extractBioOil((int) maxExtract_mB, true);
				if (toExtract_mB > 0) {
					transaction.addCloseCallback((t, result) -> {
						if (result.wasCommitted()) {
							caldera.extractBioOil(toExtract_mB, false);
						}
					});
				}
				return toExtract_mB * 81L;
			}

			@Override
			public boolean isResourceBlank() {
				return caldera.getBioOilAmount() <= 0;
			}

			@Override
			public FluidVariant getResource() {
				return caldera.getBioOilAmount() > 0 ? FluidVariant.of(BIO_OIL_FLUID) : FluidVariant.blank();
			}
		}, DIONYSIAN_BIO_CALDERA_BE);
		FluidStorage.SIDED.registerForBlockEntity((distillery, dir) -> new SingleFluidStorage() {
			@Override
			protected long getCapacity(FluidVariant variant) {
				return DionysianDistilleryBlockEntity.MAX_FLUID * 81L;
			}

			@Override
			public long getAmount() {
				return distillery.getFluidAmount() * 81L;
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				return 0;
			}

			@Override
			public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (distillery.getFluidAmount() <= 0 || !distillery.getStoredFluid().equals(resource)) return 0;
				long maxExtract_mB = maxAmount / 81L;
				int toExtract_mB = distillery.extractFluid(resource, (int) maxExtract_mB, true);
				if (toExtract_mB > 0) {
					transaction.addCloseCallback((t, result) -> {
						if (result.wasCommitted()) {
							distillery.extractFluid(resource, toExtract_mB, false);
						}
					});
				}
				return toExtract_mB * 81L;
			}

			@Override
			public boolean isResourceBlank() {
				return distillery.getFluidAmount() <= 0 || distillery.getStoredFluid().isBlank();
			}

			@Override
			public FluidVariant getResource() {
				return distillery.getFluidAmount() > 0 ? distillery.getStoredFluid() : FluidVariant.blank();
			}
		}, DIONYSIAN_DISTILLERY_BE);
		FluidStorage.SIDED.registerForBlockEntity((dieselGen, dir) -> new SingleFluidStorage() {
			@Override
			protected long getCapacity(FluidVariant variant) {
				return VolcanicDieselGeneratorBlockEntity.MAX_FLUID * 81L;
			}

			@Override
			public long getAmount() {
				return dieselGen.getFluidAmount() * 81L;
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (VolcanicDieselGeneratorBlockEntity.getFuelProfile(resource) == null) return 0;
				long maxInsert_mB = maxAmount / 81L;
				int toInsert_mB = dieselGen.insertFluidInternal(resource, (int) maxInsert_mB, true);
				if (toInsert_mB > 0) {
					transaction.addCloseCallback((t, result) -> {
						if (result.wasCommitted()) {
							dieselGen.insertFluidInternal(resource, toInsert_mB, false);
						}
					});
				}
				return toInsert_mB * 81L;
			}

			@Override
			public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				return 0;
			}

			@Override
			public boolean isResourceBlank() {
				return dieselGen.getFluidAmount() <= 0 || dieselGen.getStoredFluid().isBlank();
			}

			@Override
			public FluidVariant getResource() {
				return dieselGen.getFluidAmount() > 0 ? dieselGen.getStoredFluid() : FluidVariant.blank();
			}
		}, VOLCANIC_DIESEL_GENERATOR_BE);
		FluidStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.fluidStorage, VOLCANITE_UNIVERSAL_CONDUIT_BE);
		FluidStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.getFluidStorage(direction), NAIAD_FLUID_PIPE_BE);
		FluidStorage.SIDED.registerForBlockEntity((tank, direction) -> tank, SUPREME_LIQUID_TANK_BE);

		FluidStorage.SIDED.registerForBlockEntity((gen, direction) -> new SingleFluidStorage() {
			@Override
			protected long getCapacity(FluidVariant variant) {
				return gen.getMaxLava() * 81L;
			}

			@Override
			public long getAmount() {
				return gen.getLavaAmount() * 81L;
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (!resource.isOf(Fluids.LAVA)) return 0;
				long maxInsert_mB = maxAmount / 81L;
				long toInsert_mB = Math.min(maxInsert_mB, gen.getMaxLava() - gen.getLavaAmount());
				if (toInsert_mB > 0) {
					transaction.addCloseCallback((t, result) -> {
						if (result.wasCommitted()) {
							gen.addLava((int) toInsert_mB);
						}
					});
				}
				return toInsert_mB * 81L;
			}

			@Override
			public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				return 0;
			}

			@Override
			public boolean isResourceBlank() {
				return gen.getLavaAmount() <= 0;
			}

			@Override
			public FluidVariant getResource() {
				return gen.getLavaAmount() > 0 ? FluidVariant.of(Fluids.LAVA) : FluidVariant.blank();
			}
		}, VOLCANITE_GENERATOR_BE);

		FluidStorage.SIDED.registerForBlockEntity((pump, direction) -> new SingleFluidStorage() {
			@Override
			protected long getCapacity(FluidVariant variant) {
				return pump.getMaxCapacity() * 81L;
			}

			@Override
			public long getAmount() {
				return pump.getLavaAmount() * 81L;
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (!resource.isOf(Fluids.LAVA)) return 0;
				long maxInsert_mB = maxAmount / 81L;
				long toInsert_mB = Math.min(maxInsert_mB, pump.getMaxCapacity() - pump.getLavaAmount());
				if (toInsert_mB > 0) {
					transaction.addCloseCallback((t, result) -> {
						if (result.wasCommitted()) {
							pump.insertLava((int) toInsert_mB);
						}
					});
				}
				return toInsert_mB * 81L;
			}

			@Override
			public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (!resource.isOf(Fluids.LAVA)) return 0;
				long maxExtract_mB = maxAmount / 81L;
				long toExtract_mB = Math.min(maxExtract_mB, pump.getLavaAmount());
				if (toExtract_mB > 0) {
					transaction.addCloseCallback((t, result) -> {
						if (result.wasCommitted()) {
							pump.extractLava((int) toExtract_mB);
						}
					});
				}
				return toExtract_mB * 81L;
			}

			@Override
			public boolean isResourceBlank() {
				return pump.getLavaAmount() <= 0;
			}

			@Override
			public FluidVariant getResource() {
				return pump.getLavaAmount() > 0 ? FluidVariant.of(Fluids.LAVA) : FluidVariant.blank();
			}
		}, VOLCANITE_LAVA_PUMP_BE);

		FluidStorage.SIDED.registerForBlockEntity((supGen, direction) -> new SingleFluidStorage() {
			@Override
			protected long getCapacity(FluidVariant variant) {
				return SupremeVolcanicGeneratorBlockEntity.MAX_DISTILLATE * 81L;
			}

			@Override
			public long getAmount() {
				return supGen.getDistillateAmount() * 81L;
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (!resource.isOf(PYROCLASTIC_DISTILLATE_FLUID)) return 0;
				long maxInsert_mB = maxAmount / 81L;
				long toInsert_mB = Math.min(maxInsert_mB, SupremeVolcanicGeneratorBlockEntity.MAX_DISTILLATE - supGen.getDistillateAmount());
				if (toInsert_mB > 0) {
					transaction.addCloseCallback((t, result) -> {
						if (result.wasCommitted()) {
							supGen.insertDistillate((int) toInsert_mB);
						}
					});
				}
				return toInsert_mB * 81L;
			}

			@Override
			public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				return 0;
			}

			@Override
			public boolean isResourceBlank() {
				return supGen.getDistillateAmount() <= 0;
			}

			@Override
			public FluidVariant getResource() {
				return supGen.getDistillateAmount() > 0 ? FluidVariant.of(PYROCLASTIC_DISTILLATE_FLUID) : FluidVariant.blank();
			}
		}, SUPREME_VOLCANIC_GENERATOR_BE);

		FluidStorage.SIDED.registerForBlockEntity((geoGen, direction) -> new SingleFluidStorage() {
			@Override
			protected long getCapacity(FluidVariant variant) {
				return geoGen.getMaxLava() * 81L;
			}

			@Override
			public long getAmount() {
				return geoGen.getLavaAmount() * 81L;
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (!resource.isOf(Fluids.LAVA)) return 0;
				long maxInsert_mB = maxAmount / 81L;
				long toInsert_mB = Math.min(maxInsert_mB, geoGen.getMaxLava() - geoGen.getLavaAmount());
				if (toInsert_mB > 0) {
					transaction.addCloseCallback((t, result) -> {
						if (result.wasCommitted()) {
							geoGen.insertLava((int) toInsert_mB);
						}
					});
				}
				return toInsert_mB * 81L;
			}

			@Override
			public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				return 0;
			}

			@Override
			public boolean isResourceBlank() {
				return geoGen.getLavaAmount() <= 0;
			}

			@Override
			public FluidVariant getResource() {
				return geoGen.getLavaAmount() > 0 ? FluidVariant.of(Fluids.LAVA) : FluidVariant.blank();
			}
		}, VOLCANITE_GEOTHERMAL_GENERATOR_BE);

		FluidStorage.SIDED.registerForBlockEntity((tank, direction) -> new SingleFluidStorage() {
			@Override
			protected long getCapacity(FluidVariant variant) {
				return Long.MAX_VALUE;
			}

			@Override
			public long getAmount() {
				return Long.MAX_VALUE;
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				return maxAmount;
			}

			@Override
			public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (!resource.isOf(Fluids.LAVA)) return 0;
				return maxAmount;
			}

			@Override
			public boolean isResourceBlank() {
				return false;
			}

			@Override
			public FluidVariant getResource() {
				return FluidVariant.of(Fluids.LAVA);
			}
		}, CREATIVE_LAVA_TANK_BE);

		FluidStorage.SIDED.registerForBlockEntity((tank, direction) -> new SingleFluidStorage() {
			@Override
			protected long getCapacity(FluidVariant variant) {
				return Long.MAX_VALUE;
			}

			@Override
			public long getAmount() {
				return Long.MAX_VALUE;
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				return maxAmount;
			}

			@Override
			public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (!resource.isOf(Fluids.WATER)) return 0;
				return maxAmount;
			}

			@Override
			public boolean isResourceBlank() {
				return false;
			}

			@Override
			public FluidVariant getResource() {
				return FluidVariant.of(Fluids.WATER);
			}
		}, CREATIVE_WATER_TANK_BE);

		FluidStorage.SIDED.registerForBlockEntity((arborist, direction) -> new SingleFluidStorage() {
			@Override
			protected long getCapacity(FluidVariant variant) {
				return (long) GeothermalArboristBlockEntity.MAX_LAVA * 81L;
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				if (resource.isBlank() || maxAmount <= 0) return 0;
				if (!resource.isOf(Fluids.LAVA) && !resource.isOf(Fluids.WATER)) return 0;
				int space = arborist.getRemainingFluidCapacity();
				int canAdd = (int) Math.min(space, maxAmount / 81L);
				if (canAdd <= 0) return 0;
				transaction.addCloseCallback((t, result) -> {
					if (result.wasCommitted()) {
						arborist.insertFluid(canAdd);
					}
				});
				return (long) canAdd * 81L;
			}

			@Override
			public FluidVariant getResource() {
				return FluidVariant.of(Fluids.LAVA);
			}

			@Override
			public long getAmount() {
				return (long) arborist.getLavaAmount() * 81L;
			}
		}, GEOTHERMAL_ARBORIST_BE);

		FluidStorage.SIDED.registerForBlockEntity((be, dir) -> createColliderFluidStorage(be), OLYMPIAN_HADRON_COLLIDER_BE);
		FluidStorage.SIDED.registerForBlocks((level, pos, state, blockEntity, direction) -> {
			OlympianHadronColliderBlockEntity collider = SynchrotronRingCoilBlock.findController(level, pos);
			return (collider != null && collider.isFormed()) ? createColliderFluidStorage(collider) : null;
		}, SYNCHROTRON_RING_COIL_BLOCK, HADRON_FLUID_PORT_BLOCK);

		// Sided Item Storages
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, VOLCANITE_WORKTABLE_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, HYDROTHERMAL_VITRIFIER_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, HYDROTHERMAL_LITHOFIER_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, MAGMATIC_CRUCIBLE_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, HYDROTHERMAL_CONDENSER_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, ADVANCED_VOLCANITE_FURNACE_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, VOLCANITE_FURNACE_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, HEPHAESTEAN_ARMORY_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, VOLCANITE_GRINDER_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, GEOTHERMAL_ARBORIST_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, VOLCANITE_STEAM_GENERATOR_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, DIONYSIAN_DISTILLERY_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, DIONYSIAN_PRESS_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, ADVANCED_DIONYSIAN_PRESS_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, DEMETRIAN_BIOMASS_GENERATOR_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, DEMETRIAN_COMPOSTER_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, DIONYSIAN_AGING_BARREL_BE);
		ItemStorage.SIDED.registerForBlockEntity((be, dir) -> InventoryStorage.of(be.getInventory(), dir), VOLCANIC_WOOD_LIQUID_BARREL_BE);
		ItemStorage.SIDED.registerForBlockEntity((be, dir) -> InventoryStorage.of(be.getInventory(), dir), REINFORCED_VOLCANIC_LIQUID_BARREL_BE);
		ItemStorage.SIDED.registerForBlockEntity((be, dir) -> InventoryStorage.of(be.getInventory(), dir), MEGA_VOLCANIC_BARREL_BE);
		ItemStorage.SIDED.registerForBlockEntity((be, dir) -> InventoryStorage.of(be.getInventory(), dir), TARTARUS_PISTON_ENGINE_BE);
		ItemStorage.SIDED.registerForBlockEntity((be, dir) -> InventoryStorage.of(be.getInventory(), dir), VOLCANIC_DIESEL_GENERATOR_BE);
		ItemStorage.SIDED.registerForBlockEntity((be, dir) -> InventoryStorage.of(be.getInventory(), dir), DIONYSIAN_BIO_CALDERA_BE);
		ItemStorage.SIDED.registerForBlockEntity((be, dir) -> be.itemStorage, BASIC_VOLCANITE_ITEM_BARREL_BE);
		ItemStorage.SIDED.registerForBlockEntity((be, dir) -> be.itemStorage, ADVANCED_VOLCANITE_ITEM_BARREL_BE);
		ItemStorage.SIDED.registerForBlockEntity((be, dir) -> be.itemStorage, SUPREME_VOLCANITE_ITEM_BARREL_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, VOLCANITE_MODULAR_CHEST_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, VOLCANITE_CHEST_BE);
		ItemStorage.SIDED.registerForBlockEntity((be, dir) -> InventoryStorage.of(be.getInventory(), dir), MAGMA_FORGE_CORE_BE);
		ItemStorage.SIDED.registerForBlockEntity((be, dir) -> InventoryStorage.of(be.getInventory(), dir), THANATOS_EXECUTIONER_BE);
		ItemStorage.SIDED.registerForBlockEntity((be, dir) -> InventoryStorage.of(be.getInventory(), dir), APOLLO_SOLAR_GENERATOR_BE);
		ItemStorage.SIDED.registerForBlockEntity((be, dir) -> InventoryStorage.of(be.getInventory(), dir), AEOLUS_STORM_TURBINE_BE);
		ItemStorage.SIDED.registerForBlockEntity(InventoryStorage::of, OLYMPIAN_HADRON_COLLIDER_BE);
		ItemStorage.SIDED.registerForBlocks((level, pos, state, blockEntity, direction) -> {
			OlympianHadronColliderBlockEntity collider = SynchrotronRingCoilBlock.findController(level, pos);
			return (collider != null && collider.isFormed()) ? InventoryStorage.of(collider, direction) : null;
		}, SYNCHROTRON_RING_COIL_BLOCK, HADRON_ITEM_PORT_BLOCK);

		// Multiblock Fallbacks
		FluidStorage.SIDED.registerFallback((level, pos, state, blockEntity, direction) -> {
			if (state.getBlock() instanceof MegaVolcanicBarrelPartBlock) {
				MegaVolcanicBarrelBlockEntity controller = MegaVolcanicBarrelPartBlock.findController(level, pos);
				if (controller != null) return controller;
			}
			if (state.getBlock() instanceof TartarusPistonEnginePartBlock) {
				TartarusPistonEngineBlockEntity controller = TartarusPistonEnginePartBlock.findController(level, pos);
				if (controller != null) return controller;
			}
			if (blockEntity instanceof LiquidTankBlockEntity tank) {
				return tank;
			}
			if (state.getBlock() == MAGMA_FORGE_DUMMY_BLOCK) {
				BlockPos corePos = MagmaForgeDummyBlock.findCore(level, pos);
				if (corePos != null && level.getBlockEntity(corePos) instanceof MagmaForgeCoreBlockEntity core) {
					return core.fluidStorage;
				}
			}
			return null;
		});

		ItemStorage.SIDED.registerFallback((level, pos, state, blockEntity, direction) -> {
			if (state.getBlock() instanceof MegaVolcanicBarrelPartBlock) {
				MegaVolcanicBarrelBlockEntity controller = MegaVolcanicBarrelPartBlock.findController(level, pos);
				if (controller != null) return InventoryStorage.of(controller.getInventory(), direction);
			}
			if (state.getBlock() instanceof TartarusPistonEnginePartBlock) {
				TartarusPistonEngineBlockEntity controller = TartarusPistonEnginePartBlock.findController(level, pos);
				if (controller != null) return InventoryStorage.of(controller.getInventory(), direction);
			}
			if (state.getBlock() instanceof DionysianBioCalderaPartBlock) {
				DionysianBioCalderaBlockEntity controller = DionysianBioCalderaPartBlock.findController(level, pos);
				if (controller != null) return InventoryStorage.of(controller.getInventory(), direction);
			}
			if (state.getBlock() == MAGMA_FORGE_DUMMY_BLOCK) {
				BlockPos corePos = MagmaForgeDummyBlock.findCore(level, pos);
				if (corePos != null && level.getBlockEntity(corePos) instanceof MagmaForgeCoreBlockEntity core) {
					return InventoryStorage.of(core.getInventory(), direction);
				}
			}
			return null;
		});

		EnergyStorage.SIDED.registerFallback((level, pos, state, blockEntity, direction) -> {
			if (state.getBlock() instanceof TartarusPistonEnginePartBlock) {
				TartarusPistonEngineBlockEntity controller = TartarusPistonEnginePartBlock.findController(level, pos);
				if (controller != null) return controller.energyStorage;
			}
			if (state.getBlock() instanceof DionysianBioCalderaPartBlock) {
				DionysianBioCalderaBlockEntity controller = DionysianBioCalderaPartBlock.findController(level, pos);
				if (controller != null) return controller.energyStorage;
			}
			if (state.getBlock() == VOLCANITE_CASING_BLOCK || state.getBlock() == ADVANCED_VOLCANITE_CASING_BLOCK || state.getBlock() == VOLCANITE_ENERGY_CELL_BLOCK || state.getBlock() == VOLCANITE_ENERGY_UNIT_BLOCK) {
				VolcaniteEnergyCoreBlockEntity formedCore = VolcaniteEnergyCoreBlockEntity.findFormedCoreForRingBlock(level, pos);
				if (formedCore != null) {
					return formedCore.energyStorage;
				}
			}
			if (state.getBlock() == MAGMA_FORGE_DUMMY_BLOCK) {
				BlockPos corePos = MagmaForgeDummyBlock.findCore(level, pos);
				if (corePos != null && level.getBlockEntity(corePos) instanceof MagmaForgeCoreBlockEntity core) {
					return core.energyStorage;
				}
			}
			return null;
		});

		// Item Fluid Storages
		FluidStorage.ITEM.registerForItems((itemStack, context) -> {
			if (itemStack.getItem() instanceof VolcaniteBottleItem bottle) {
				return new SingleSlotStorage<FluidVariant>() {
					@Override
					public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
						if (resource.isBlank() || maxAmount <= 0) return 0;
						ItemStack currentStack = context.getItemVariant().toStack();
						FluidVariant current = VolcaniteBottleItem.getFluid(currentStack);
						int currentAmt = VolcaniteBottleItem.getFluidAmount(currentStack);
						if (!current.isBlank() && current != resource) return 0;
						int space = bottle.getCapacity() - currentAmt;
						int toInsert = (int) Math.min(space, maxAmount / 81L);
						if (toInsert <= 0) return 0;
						transaction.addCloseCallback((t, result) -> {
							if (result.wasCommitted()) {
								ItemStack newStack = context.getItemVariant().toStack();
								VolcaniteBottleItem.setFluid(newStack, resource, currentAmt + toInsert);
								context.exchange(ItemVariant.of(newStack), 1, t);
							}
						});
						return toInsert * 81L;
					}

					@Override
					public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
						if (resource.isBlank() || maxAmount <= 0) return 0;
						ItemStack currentStack = context.getItemVariant().toStack();
						FluidVariant current = VolcaniteBottleItem.getFluid(currentStack);
						int currentAmt = VolcaniteBottleItem.getFluidAmount(currentStack);
						if (current.isBlank() || current != resource || currentAmt <= 0) return 0;
						int toExtract = (int) Math.min(currentAmt, maxAmount / 81L);
						if (toExtract <= 0) return 0;
						transaction.addCloseCallback((t, result) -> {
							if (result.wasCommitted()) {
								ItemStack newStack = context.getItemVariant().toStack();
								VolcaniteBottleItem.setFluid(newStack, current, currentAmt - toExtract);
								context.exchange(ItemVariant.of(newStack), 1, t);
							}
						});
						return toExtract * 81L;
					}

					@Override
					public boolean isResourceBlank() {
						return getResource().isBlank();
					}

					@Override
					public FluidVariant getResource() {
						return VolcaniteBottleItem.getFluid(context.getItemVariant().toStack());
					}

					@Override
					public long getAmount() {
						return VolcaniteBottleItem.getFluidAmount(context.getItemVariant().toStack()) * 81L;
					}

					@Override
					public long getCapacity() {
						return bottle.getCapacity() * 81L;
					}
				};
			}
			return null;
		}, VOLCANITE_BOTTLE, ADVANCED_VOLCANITE_BOTTLE);

		// Item Energy Storages
		EnergyStorage.ITEM.registerForItems((stack, context) -> SimpleEnergyItem.createStorage(context, 500000, 10000, 10000), VOLCANITE_PORTABLE_BATTERY);
		EnergyStorage.ITEM.registerForItems((stack, context) -> SimpleEnergyItem.createStorage(context, 5000000, 100000, 100000), ADVANCED_VOLCANITE_PORTABLE_BATTERY);
		EnergyStorage.ITEM.registerForItems((stack, context) -> SimpleEnergyItem.createStorage(context, VolcaniteWarpGauntletItem.ENERGY_CAPACITY, VolcaniteWarpGauntletItem.MAX_INSERT, VolcaniteWarpGauntletItem.MAX_EXTRACT), VOLCANITE_WARP_GAUNTLET);
		EnergyStorage.ITEM.registerForItems((stack, context) -> SimpleEnergyItem.createStorage(context, 1000000, 10000, 10000), ADVANCED_VOLCANITE_BOTTLE);
		EnergyStorage.ITEM.registerForItems((stack, context) -> SimpleEnergyItem.createStorage(context, AdvancedBackpackItem.MAX_ENERGY, AdvancedBackpackItem.MAX_INPUT, AdvancedBackpackItem.MAX_OUTPUT), ADVANCED_BACKPACK);

		EnergyStorage.ITEM.registerForItems((stack, context) -> SimpleEnergyItem.createStorage(context, 1000000, 50000, 0),
			HEPHAESTEAN_SWORD, HEPHAESTEAN_PICKAXE, HEPHAESTEAN_AXE, HEPHAESTEAN_SHOVEL, HEPHAESTEAN_HOE,
			HEPHAESTEAN_HELMET, HEPHAESTEAN_CHESTPLATE, HEPHAESTEAN_LEGGINGS, HEPHAESTEAN_BOOTS);
		EnergyStorage.ITEM.registerForItems((stack, context) -> SimpleEnergyItem.createStorage(context, 3000000, 100000, 0),
			HEPHAESTEAN_PAXEL);

		EnergyStorage.ITEM.registerForItems((stack, context) -> SimpleEnergyItem.createStorage(context, EternalCoinItem.CAPACITY, EternalCoinItem.MAX_INPUT, 0),
			ETERNAL_COIN);
		EnergyStorage.ITEM.registerForItems((stack, context) -> SimpleEnergyItem.createStorage(context, HephaesteanResonatorItem.CAPACITY, HephaesteanResonatorItem.MAX_INPUT, 0),
			HEPHAESTEAN_RESONATOR);
		EnergyStorage.ITEM.registerForItems((stack, context) -> SimpleEnergyItem.createStorage(context, TalariaOfHermesItem.CAPACITY, TalariaOfHermesItem.MAX_INPUT, 0),
			TALARIA_OF_HERMES);
		EnergyStorage.ITEM.registerForItems((stack, context) -> SimpleEnergyItem.createStorage(context, ThyrsusItem.CAPACITY, 10_000, 0),
			THYRSUS_OF_DIONYSUS);
	}

	private static SingleFluidStorage createColliderFluidStorage(OlympianHadronColliderBlockEntity collider) {
		return new SingleFluidStorage() {
			@Override
			protected long getCapacity(FluidVariant variant) {
				if (OlympianHadronColliderBlockEntity.getFuelTypeForFluid(variant) != OlympianHadronColliderBlockEntity.FUEL_NONE) {
					return OlympianHadronColliderBlockEntity.MAX_FUEL * 81L;
				}
				return OlympianHadronColliderBlockEntity.MAX_COOLANT * 81L;
			}

			@Override
			public long getAmount() {
				int amt = collider.getFuelAmount() > 0 ? collider.getFuelAmount() : collider.getCoolantAmount();
				return amt * 81L;
			}

			@Override
			public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				long maxInsertMb = maxAmount / 81L;
				int toInsertMb = collider.insertFluid(resource, (int) maxInsertMb, true);
				if (toInsertMb > 0) {
					transaction.addCloseCallback((t, result) -> {
						if (result.wasCommitted()) {
							collider.insertFluid(resource, toInsertMb, false);
						}
					});
				}
				return toInsertMb * 81L;
			}

			@Override
			public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
				return 0;
			}

			@Override
			public boolean isResourceBlank() {
				return collider.getFuelAmount() <= 0 && collider.getCoolantAmount() <= 0;
			}

			@Override
			public FluidVariant getResource() {
				FluidVariant fuel = collider.getFuelFluidVariant();
				if (!fuel.isBlank()) return fuel;
				return collider.getCoolantFluidVariant();
			}
		};
	}
}
