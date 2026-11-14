package net.valentin.obsidium.registry;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.valentin.obsidium.block.*;
import net.valentin.obsidium.event.StygianDeathHandler;
import net.valentin.obsidium.item.HephaesteanArmorItem;
import net.valentin.obsidium.item.TalariaOfHermesItem;
import net.valentin.obsidium.item.VolcaniteCableConnectorItem;
import net.valentin.obsidium.item.VolcaniteLogisticsModuleItem;
import net.valentin.obsidium.item.VolcaniteOverclockUpgradeItem;
import net.valentin.obsidium.item.VolcanitePipeEnergyUpgradeItem;
import net.valentin.obsidium.item.VolcaniteStackUpgradeItem;
import net.valentin.obsidium.item.VolcaniteWrenchItem;
import net.valentin.obsidium.network.GeyserJumpPayload;
import net.valentin.obsidium.network.SortContainerPayload;
import net.valentin.obsidium.network.TeleportActionPayload;
import net.valentin.obsidium.network.TeleportNetworkSyncPayload;
import net.valentin.obsidium.network.TransferRecipeToCrafterPayload;
import net.valentin.obsidium.teleport.VolcaniteTeleportNetworkState;
import net.valentin.obsidium.util.ContainerSorter;


public final class ModEventsAndNetworking {
	private static final ThreadLocal<Boolean> IS_PROCESSING_AEGIS = ThreadLocal.withInitial(() -> false);

	private ModEventsAndNetworking() {}

	public static void register() {
		// Olympian Divine Pacts Combat & Gameplay System
		net.valentin.obsidium.mythology.DivinePactCombatHandler.register();

		// Divine Flight System (Hermes' Talaria)
		ServerTickEvents.END_SERVER_TICK.register(TalariaOfHermesItem::tickServerFlight);

		// Clean up static network caches on world/server shutdown to prevent cross-session memory leaks
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			VolcaniteNetworkNodeEmitterBlockEntity.clearStaticRegistries();
			VolcaniteNetworkNodeReceiverBlockEntity.clearStaticRegistries();
		});
		ServerWorldEvents.UNLOAD.register((server, world) -> {
			VolcaniteNetworkNodeEmitterBlockEntity.clearDimension(world.dimension());
			VolcaniteNetworkNodeReceiverBlockEntity.clearDimension(world.dimension());
		});

		// Aether-Faraday Suit Lightning Immunity & Aegis of Athena Divine Reflection
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT)
					&& net.valentin.obsidium.item.FaradayArmorItem.hasFullSuit(entity)) {
				return false;
			}
			if (IS_PROCESSING_AEGIS.get()) return true;

			if (entity instanceof Player player && player.isBlocking()) {
				ItemStack active = player.getUseItem();
				if (active.getItem() instanceof net.valentin.obsidium.item.AegisShieldItem) {
					// 1. Projectile Reflection
					net.minecraft.world.entity.Entity direct = source.getDirectEntity();
					if (direct instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
						net.minecraft.world.phys.Vec3 look = player.getLookAngle();
						projectile.setDeltaMovement(look.scale(1.8));
						projectile.hurtMarked = true;
						if (projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow) {
							arrow.setOwner(player);
						}
						player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
								SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2F, 1.5F);
						if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
							sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, direct.getX(), direct.getY(), direct.getZ(), 1, 0, 0, 0, 0);
						}
						active.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(player.getUsedItemHand()));
						return false;
					}

					// 2. Melee Volcanic Shockwave Backlash
					net.minecraft.world.entity.Entity attacker = source.getEntity();
					if (attacker instanceof net.minecraft.world.entity.LivingEntity livingAttacker && attacker != player) {
						IS_PROCESSING_AEGIS.set(true);
						try {
							livingAttacker.igniteForSeconds(4);
							net.minecraft.world.phys.Vec3 knockback = livingAttacker.position().subtract(player.position()).normalize().scale(1.4).add(0, 0.35, 0);
							livingAttacker.setDeltaMovement(knockback);
							livingAttacker.hurtMarked = true;
							livingAttacker.hurt(player.level().damageSources().thorns(player), 6.0F);

							player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
									SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.3F);
							if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
								sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, livingAttacker.getX(), livingAttacker.getY() + 1.0, livingAttacker.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
							}
							active.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(player.getUsedItemHand()));
						} finally {
							IS_PROCESSING_AEGIS.set(false);
						}
					}
				}
			}
			return true;
		});

		// Stygian Death System
		StygianDeathHandler.register();

		// Keraunos High-Voltage Matrix Unsafe Disassembly Arc Blast Hook
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (!level.isClientSide()) {
				KeraunosHVCoreBlockEntity core = null;
				if (blockEntity instanceof KeraunosHVCoreBlockEntity c) {
					core = c;
				} else if (KeraunosHVCoreBlockEntity.isValidShellBlock(state.getBlock())) {
					core = KeraunosHVCoreBlockEntity.findFormedCoreForShellBlock(level, pos);
				}
				if (core != null && core.isEnergized()) {
					core.triggerArcBlast(level, pos, player);
				}
			}
			return true;
		});

		// Geyser Jump C2S Packet
		PayloadTypeRegistry.playC2S().register(GeyserJumpPayload.TYPE, GeyserJumpPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(GeyserJumpPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				Player player = context.player();
				ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
				if (boots.getItem() instanceof HephaesteanArmorItem hephBoots) {
					if (hephBoots.getStoredEnergy(boots) >= 5000L) {
						hephBoots.tryUseEnergy(boots, 5000L);
						player.setDeltaMovement(player.getDeltaMovement().add(0, 1.3, 0));
						player.hurtMarked = true;
						player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0F, 0.8F);
					}
				}
			});
		});

		// Global Container Sort (R / Shift+R) C2S Packet
		PayloadTypeRegistry.playC2S().register(SortContainerPayload.TYPE, SortContainerPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SortContainerPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ContainerSorter.sortOpenContainer(context.player(), payload.descending());
			});
		});

		// Storage Terminal search text C2S Packet
		PayloadTypeRegistry.playC2S().register(net.valentin.obsidium.network.TerminalSearchPayload.TYPE, net.valentin.obsidium.network.TerminalSearchPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(net.valentin.obsidium.network.TerminalSearchPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				if (context.player().containerMenu instanceof net.valentin.obsidium.block.VolcaniteStorageTerminalMenu terminal) {
					terminal.setSearchFilter(payload.query());
					terminal.broadcastChanges();
				}
			});
		});

		// Advanced Volcanite Quantum Teleportation Network (S2C Sync + C2S Actions)
		PayloadTypeRegistry.playS2C().register(TeleportNetworkSyncPayload.TYPE, TeleportNetworkSyncPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportActionPayload.TYPE, TeleportActionPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TeleportActionPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				if (player.server == null) return;
				VolcaniteTeleportNetworkState state = VolcaniteTeleportNetworkState.get(player.server);

				boolean fromBlock = payload.fromBlock();
				VolcanicTeleporterBlockEntity pad = null;
				net.minecraft.world.InteractionHand hand = null;
				ItemStack gauntletStack = ItemStack.EMPTY;

				if (fromBlock) {
					if (player.blockPosition().distSqr(payload.blockPos()) <= 100.0
							&& player.level().getBlockEntity(payload.blockPos()) instanceof VolcanicTeleporterBlockEntity tp) {
						pad = tp;
					} else {
						return;
					}
				} else {
					if (player.containerMenu instanceof VolcaniteTeleporterMenu tpMenu && tpMenu.getGauntletHand() != null) {
						hand = tpMenu.getGauntletHand();
						gauntletStack = player.getItemInHand(hand);
					}
					if (!(gauntletStack.getItem() instanceof net.valentin.obsidium.item.VolcaniteWarpGauntletItem)) {
						if (player.getMainHandItem().getItem() instanceof net.valentin.obsidium.item.VolcaniteWarpGauntletItem) {
							hand = net.minecraft.world.InteractionHand.MAIN_HAND;
							gauntletStack = player.getMainHandItem();
						} else if (player.getOffhandItem().getItem() instanceof net.valentin.obsidium.item.VolcaniteWarpGauntletItem) {
							hand = net.minecraft.world.InteractionHand.OFF_HAND;
							gauntletStack = player.getOffhandItem();
						} else {
							return;
						}
					}
				}

				switch (payload.action()) {
					case TeleportActionPayload.ACTION_WARP -> {
						if (state.executeTeleport(player, payload.targetId(), fromBlock ? payload.blockPos() : null, hand)) {
							player.closeContainer();
						} else {
							state.sendSyncToPlayer(player, pad, gauntletStack);
						}
					}
					case TeleportActionPayload.ACTION_PIN -> {
						String newPin = payload.targetId() == null ? "" : payload.targetId();
						if (fromBlock && pad != null) {
							String nextPin = newPin.equals(pad.getPinnedTargetId()) ? "" : newPin;
							pad.setPinnedTargetId(nextPin);
						} else if (!gauntletStack.isEmpty()) {
							String curPin = net.valentin.obsidium.item.VolcaniteWarpGauntletItem.getPinnedTarget(gauntletStack);
							if (newPin.isEmpty() || newPin.equals(curPin)) {
								net.minecraft.world.item.component.CustomData.update(
										net.minecraft.core.component.DataComponents.CUSTOM_DATA,
										gauntletStack,
										tag -> {
											tag.remove("PinnedTarget");
											tag.remove("PinnedName");
											tag.remove("WpX");
											tag.remove("WpY");
											tag.remove("WpZ");
											tag.remove("WpDim");
										}
								);
								player.displayClientMessage(Component.literal("§7⛩ Warp Gauntlet Quick-Warp pin cleared."), true);
							} else {
								VolcaniteTeleportNetworkState.ResolvedDestination dest = state.findDestination(player, newPin);
								if (dest != null) {
									net.valentin.obsidium.item.VolcaniteWarpGauntletItem.setPinnedTarget(
											gauntletStack, dest.id(), dest.name(), dest.dimension(),
											dest.pos().getX(), dest.pos().getY(), dest.pos().getZ()
									);
									player.displayClientMessage(Component.literal("§b⛩ Warp Gauntlet pinned to §e" + dest.name()), true);
								}
							}
						}
						state.sendSyncToPlayer(player, pad, gauntletStack);
					}
					case TeleportActionPayload.ACTION_UPDATE_PAD -> {
						if (fromBlock && pad != null) {
							java.util.UUID owner = pad.getOwnerUuid();
							boolean isUnowned = owner.getMostSignificantBits() == 0L && owner.getLeastSignificantBits() == 0L;
							if (isUnowned || owner.equals(player.getUUID()) || player.hasPermissions(2)) {
								pad.updateSettings(payload.name(), payload.iconIndex(), payload.isPublic());
							} else {
								player.displayClientMessage(Component.literal("§c⛩ Only the pad owner can modify name or privacy!"), true);
							}
						}
						state.sendSyncToPlayer(player, pad, gauntletStack);
					}
					case TeleportActionPayload.ACTION_SAVE_WAYPOINT -> {
						String cleanName = payload.name() == null ? "" : payload.name().trim();
						if (cleanName.isEmpty()) {
							cleanName = "Field [" + player.blockPosition().getX() + ", " + player.blockPosition().getZ() + "]";
						}
						if (cleanName.length() > 32) {
							cleanName = cleanName.substring(0, 32);
						}
						String wpId = "wp:" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
						VolcaniteTeleportNetworkState.PersonalWaypoint wp = new VolcaniteTeleportNetworkState.PersonalWaypoint(
								wpId, cleanName, player.level().dimension().location().toString(),
								player.blockPosition(), player.getYRot(), Math.max(0, Math.min(7, payload.iconIndex()))
						);
						if (!state.addOrUpdatePersonalWaypoint(player.getUUID(), wp)) {
							player.displayClientMessage(Component.literal("§c⛩ Personal waypoint limit reached (" + VolcaniteTeleportNetworkState.MAX_PERSONAL_WAYPOINTS + ")!"), true);
						} else {
							player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.3F);
						}
						state.sendSyncToPlayer(player, pad, gauntletStack);
					}
					case TeleportActionPayload.ACTION_DELETE_WAYPOINT -> {
						state.removePersonalWaypoint(player.getUUID(), payload.targetId());
						state.sendSyncToPlayer(player, pad, gauntletStack);
					}
					default -> state.sendSyncToPlayer(player, pad, gauntletStack);
				}
			});
		});

		// Auto-Craft Recipe Transfer to Crafter Interface from REI / Storage Network
		PayloadTypeRegistry.playC2S().register(TransferRecipeToCrafterPayload.TYPE, TransferRecipeToCrafterPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TransferRecipeToCrafterPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				if (!(player.containerMenu instanceof VolcaniteCrafterInterfaceMenu menu)) return;
				VolcaniteCrafterInterfaceBlockEntity crafter = menu.getCrafterBlockEntity();
				if (crafter == null || !menu.stillValid(player)) return;
				VolcaniteStorageControllerBlockEntity controller = crafter.findController();

				RecipeGridTransfer.Network network = controller == null ? null : new RecipeGridTransfer.Network() {
					@Override
					public ItemStack extract(Item item, ItemStack existing) {
						return existing.isEmpty() ? controller.extractItem(item, 1, false)
								: controller.extractMatchingItem(existing, 1, false);
					}

					@Override
					public ItemStack insert(ItemStack stack) {
						return controller.insertItem(stack, false);
					}
				};
				int roundsDone = RecipeGridTransfer.transfer(crafter, player.getInventory(), payload.candidates(),
						payload.maxCraft() ? 64 : 1, network, remaining -> {
							if (!player.getInventory().add(remaining)) player.drop(remaining, false);
						});

				if (roundsDone == 0) {
					player.displayClientMessage(net.minecraft.network.chat.Component.literal(
							"§6[Crafter] §cNu ai toate materialele (inventar + rețea)!"), true);
				}

				crafter.updateRecipeOutput();
				crafter.setChanged();
				menu.broadcastChanges();
			});
		});

		// Smart Module & Wrench placement on chests and containers
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.isEmpty()) return InteractionResult.PASS;

			BlockPos pos = hitResult.getBlockPos();
			boolean isCableConnector = stack.getItem() instanceof VolcaniteCableConnectorItem;
			boolean isLogisticsModule = stack.getItem() instanceof VolcaniteLogisticsModuleItem;
			boolean isEnergyUpgrade = stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem
					|| stack.getItem() instanceof VolcaniteOverclockUpgradeItem
					|| stack.getItem() instanceof VolcaniteStackUpgradeItem;
			boolean isWrench = VolcaniteWrenchItem.isWrench(stack);

			if (!isCableConnector && !isLogisticsModule && !isEnergyUpgrade && !isWrench) return InteractionResult.PASS;

			BlockEntity clickedBe = world.getBlockEntity(pos);
			boolean isChestOrContainer = VolcaniteStoragePipeBlockEntity.isExternalContainer(world, pos, clickedBe);

			if (isChestOrContainer) {
				for (Direction d : Direction.values()) {
					BlockPos neighborPos = pos.relative(d);
					BlockEntity neighborBe = world.getBlockEntity(neighborPos);
					if (neighborBe instanceof VolcaniteStoragePipeBlockEntity pipeBe) {
						Direction pipeSideTowardsChest = d.getOpposite();
						if (isCableConnector && stack.getItem() instanceof VolcaniteCableConnectorItem connItem) {
							return connItem.attachConnector(world, neighborPos, pipeBe, pipeSideTowardsChest, player, stack);
						}
						if (!world.isClientSide()) {
							if (isEnergyUpgrade) {
								if (pipeBe.isOpticFiber() && stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem) {
									player.displayClientMessage(Component.literal("§c[Optic Fiber] Fiber cables are data-only and do not transmit RF energy!"), true);
									return InteractionResult.FAIL;
								}
								int newTier = (stack.getItem() instanceof VolcanitePipeEnergyUpgradeItem eu) ? eu.getTier()
										: (stack.getItem() instanceof VolcaniteStackUpgradeItem ? 3 : 2);
								ItemStack currentUpgrade = pipeBe.getEnergyUpgradeItem(pipeSideTowardsChest);
								if (currentUpgrade.isEmpty() || pipeBe.getEnergyUpgradeTier(pipeSideTowardsChest) != newTier) {
									if (!currentUpgrade.isEmpty() && !player.getInventory().add(currentUpgrade.copy())) {
										player.drop(currentUpgrade.copy(), false);
									}
									ItemStack toInsert = stack.copy();
									toInsert.setCount(1);
									pipeBe.setEnergyUpgrade(pipeSideTowardsChest, newTier, toInsert);
									if (!player.getAbilities().instabuild) stack.shrink(1);
									world.playSound(null, neighborPos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7F, 1.4F);
									player.displayClientMessage(Component.literal("§6[Conduit] §aInstalled §eTier " + newTier + " Upgrade §atowards machine!"), true);
								}
							} else if (isLogisticsModule) {
								VolcaniteStoragePipeBlockEntity.ModuleType cur = pipeBe.getModule(pipeSideTowardsChest);
								if (cur == VolcaniteStoragePipeBlockEntity.ModuleType.NONE) {
									pipeBe.setModule(pipeSideTowardsChest, VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT);
									if (!player.getAbilities().instabuild) stack.shrink(1);
									world.playSound(null, neighborPos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
									player.displayClientMessage(Component.literal("§6[Pipe] §aMounted §6Logistics Module (EXTRACT) §atowards container!"), true);
								} else {
									VolcaniteStoragePipeBlockEntity.ModuleType next = (cur == VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT) ?
										VolcaniteStoragePipeBlockEntity.ModuleType.INSERT : VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT;
									pipeBe.setModule(pipeSideTowardsChest, next);
									world.playSound(null, neighborPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 1.2F);
									String modeText = (next == VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT) ? "§6EXTRACT" : "§bINSERT";
									player.displayClientMessage(Component.literal("§6[Pipe] §ePipe side facing this container set to " + modeText), true);
								}
							} else {
								VolcaniteStoragePipeBlockEntity.ModuleType cur = pipeBe.getModule(pipeSideTowardsChest);
								if (cur != VolcaniteStoragePipeBlockEntity.ModuleType.NONE && !player.isShiftKeyDown()) {
									VolcaniteStoragePipeBlockEntity.ModuleType next = (cur == VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT) ?
										VolcaniteStoragePipeBlockEntity.ModuleType.INSERT : VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT;
									pipeBe.setModule(pipeSideTowardsChest, next);
									world.playSound(null, neighborPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.6F);
									String modeText = (next == VolcaniteStoragePipeBlockEntity.ModuleType.EXTRACT) ? "§6EXTRACT" : "§bINSERT";
									player.displayClientMessage(Component.literal("§6[Pipe Wrench] §ePipe side facing this chest set to " + modeText), true);
								} else {
									boolean nowDisc = pipeBe.toggleSideDisconnected(pipeSideTowardsChest);
									if (nowDisc) {
										world.playSound(null, neighborPos, SoundEvents.SPYGLASS_STOP_USING, SoundSource.BLOCKS, 1.0F, 1.2F);
										player.displayClientMessage(Component.literal("§6[Pipe Wrench] §cDisconnected pipe side pointing here!"), true);
									} else {
										world.playSound(null, neighborPos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0F, 1.4F);
										player.displayClientMessage(Component.literal("§6[Pipe Wrench] §aConnected pipe side pointing here!"), true);
									}
								}
							}
						}
						return InteractionResult.sidedSuccess(world.isClientSide());
					}
				}
			}
			return InteractionResult.PASS;
		});
	}
}
