package net.valentin.obsidium.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Captures only the selected animal's normal item-drop pipeline during synchronous damage. */
public final class PastoralHarvestDrops implements AutoCloseable {
    private static final ThreadLocal<PastoralHarvestDrops> CURRENT = new ThreadLocal<>();
    private final PastoralHarvestDrops previous;
    private final Entity target;
    private final List<ItemEntity> drops = new ArrayList<>();
    private PastoralHarvestDrops(Entity target) { this.target = target; previous = CURRENT.get(); CURRENT.set(this); }
    public static PastoralHarvestDrops begin(Entity target) { return new PastoralHarvestDrops(target); }
    public static ItemEntity capture(Entity source, ItemStack stack, float offset) {
        PastoralHarvestDrops scope = CURRENT.get();
        if (scope == null || scope.target != source || stack.isEmpty() || source.level().isClientSide) return null;
        ItemEntity drop = new ItemEntity(source.level(), source.getX(), source.getY() + offset, source.getZ(), stack);
        scope.drops.add(drop);
        return drop;
    }
    public List<ItemStack> items() { return drops.stream().map(ItemEntity::getItem).filter(s -> !s.isEmpty()).map(ItemStack::copy).toList(); }
    @Override public void close() { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
}
