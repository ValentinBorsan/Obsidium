package net.valentin.mythicrails;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class FreightStationMenu extends AbstractContainerMenu {
    private final Level level;
    private final BlockPos pos;
    private final Player owner;
    private final ContainerData data;
    private final net.minecraft.world.Container buffer;
    public FreightStationMenu(int id, Inventory inventory) { this(id,inventory,null,BlockPos.ZERO); }
    public FreightStationMenu(int id, Inventory inventory, Level level, BlockPos pos) {
        super(MythicRails.STATION_MENU,id);this.level=level;this.pos=pos.immutable();this.owner=inventory.player;
        data=level==null ? new SimpleContainerData(7) : new ContainerData() {
            public int get(int index) {
                if(!available())return 0;
                var state=level.getBlockState(pos);var cart=findCart();
                return switch(index) {
                    case 0 -> state.getValue(FreightStationBlock.FACING).get2DDataValue();
                    case 1 -> state.getValue(FreightStationBlock.ENABLED)?1:0;
                    case 2 -> ((FreightStationBlock)state.getBlock()).isLoader()?1:0;
                    case 3 -> cart!=null?cart.getDockStatus():0;
                    case 4 -> {int used=0;if(cart!=null)for(int i=0;i<HermesFreightCart.CARGO_SIZE;i++)if(!cart.getItem(i).isEmpty())used++;yield used;}
                    case 5 -> FreightStationBlock.hasDepartureSignal(level,pos)?1:0;
                    case 6 -> cart==null?0:1;
                    default -> 0;
                };
            }
            public void set(int index,int value) {}
            public int getCount(){return 7;}
        };
        buffer=level==null?new net.minecraft.world.SimpleContainer(9):
                level.getBlockEntity(pos) instanceof FreightStationBlockEntity station?station:new net.minecraft.world.SimpleContainer(9);
        for(int col=0;col<9;col++)addSlot(new Slot(buffer,col,23+col*18,156));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,23+col*18,194+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,23+col*18,252));
        addDataSlots(data);
    }
    private boolean available() {
        return level!=null && level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4)
                && level.getBlockState(pos).getBlock() instanceof FreightStationBlock
                && level.getBlockEntity(pos) instanceof FreightStationBlockEntity && level.getBlockEntity(pos)==buffer;
    }
    private HermesFreightCart findCart() {
        return level.getEntitiesOfClass(HermesFreightCart.class,new AABB(pos.above()).inflate(0.1,0.2,0.1)).stream()
                .filter(cart->!cart.isRemoved()).min(java.util.Comparator.comparingDouble(cart->cart.distanceToSqr(Vec3.atCenterOf(pos.above())))).orElse(null);
    }
    public int value(int index){return data.get(index);}
    @Override public boolean stillValid(Player player) {
        return level==null || player==owner && player.level()==level && available()
                && player.distanceToSqr(Vec3.atCenterOf(pos))<=64;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var result=stack.copy();
        if(index<9){if(!moveItemStackTo(stack,9,slots.size(),true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,9,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return result;
    }
    @Override public boolean clickMenuButton(Player player,int id) {
        if(level==null || level.isClientSide() || !stillValid(player))return false;
        var state=level.getBlockState(pos);
        if(id==0){level.setBlock(pos,state.cycle(FreightStationBlock.ENABLED),3);return true;}
        if(id==1 || id==2) {
            if(findCart()!=null && state.getValue(FreightStationBlock.ENABLED))return false;
            var direction=state.getValue(FreightStationBlock.FACING);
            level.setBlock(pos,state.setValue(FreightStationBlock.FACING,id==1?direction.getCounterClockWise():direction.getClockWise()),3);
            return true;
        }
        if(id==3){var cart=findCart();return cart!=null&&cart.releaseFromStation(pos);}
        return false;
    }
}
