package su.terrafirmagreg.core.mixins.common.tfc;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.IngotPileBlockEntity;
import net.dries007.tfc.common.blocks.devices.DoubleIngotPileBlock;
import net.dries007.tfc.common.blocks.devices.IngotPileBlock;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

@Mixin(value = CapabilityProvider.class)
public abstract class IngotPileBlockEntityMixin {

    @Unique
    private @Nullable LazyOptional<IItemHandler> tfg$itemHandler;

    @Inject(method = "getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;", at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void tfg$getCapability(Capability<T> cap, @Nullable Direction side, CallbackInfoReturnable<LazyOptional<T>> cir) {
        if ((Object) this instanceof IngotPileBlockEntity && cap == ForgeCapabilities.ITEM_HANDLER) {
            cir.setReturnValue(tfg$getItemHandler().cast());
        }
    }

    @Unique
    private IItemHandler tfg$createItemHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                if (slot != 0) {
                    return ItemStack.EMPTY;
                }
                return tfg$getTopEntryStack().copy();
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot != 0 || stack.isEmpty() || !tfg$isSupportedIngot(stack)) {
                    return stack;
                }

                final IngotPileBlockEntity pile = tfg$getTopTowerPile();
                if (pile == null) {
                    return stack;
                }

                final Level level = pile.getLevel();
                final BlockState state = pile.getBlockState();
                if (level == null || !(state.getBlock() instanceof IngotPileBlock pileBlock)) {
                    return stack;
                }

                final BlockPos pilePos = pile.getBlockPos();

                final int currentCount = state.getValue(pileBlock.getCountProperty());
                final int maxCount = pileBlock instanceof DoubleIngotPileBlock ? 36 : 64;
                if (currentCount >= maxCount) {
                    return stack;
                }

                final int insertableCount = Math.min(stack.getCount(), maxCount - currentCount);
                if (insertableCount <= 0) {
                    return stack;
                }

                if (!simulate) {
                    for (int i = 0; i < insertableCount; i++) {
                        pile.addIngot(stack.copyWithCount(1));
                    }
                    level.setBlock(pilePos, state.setValue(pileBlock.getCountProperty(), currentCount + insertableCount), Block.UPDATE_CLIENTS);
                }

                return stack.getCount() == insertableCount ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - insertableCount);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot != 0 || amount <= 0) {
                    return ItemStack.EMPTY;
                }

                final IngotPileBlockEntity pile = tfg$getTopTowerPile();
                if (pile == null) {
                    return ItemStack.EMPTY;
                }

                final ItemStack topStack = tfg$getTopEntryStack(pile);
                if (topStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                final Level level = pile.getLevel();
                final BlockState state = pile.getBlockState();
                if (level == null || !(state.getBlock() instanceof IngotPileBlock pileBlock)) {
                    return ItemStack.EMPTY;
                }

                final BlockPos pilePos = pile.getBlockPos();

                final int currentCount = state.getValue(pileBlock.getCountProperty());
                if (currentCount <= 0) {
                    return ItemStack.EMPTY;
                }

                if (!simulate) {
                    pile.removeIngot();
                    if (currentCount == 1) {
                        level.removeBlock(pilePos, false);
                    } else {
                        level.setBlock(pilePos, state.setValue(pileBlock.getCountProperty(), currentCount - 1), Block.UPDATE_CLIENTS);
                    }
                }

                return topStack.copyWithCount(1);
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == 0 ? 64 : 0;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 && tfg$isSupportedIngot(stack);
            }
        };
    }

    @Unique
    private LazyOptional<IItemHandler> tfg$getItemHandler() {
        if (tfg$itemHandler == null) {
            tfg$itemHandler = LazyOptional.of(this::tfg$createItemHandler);
        }
        return tfg$itemHandler;
    }

    @Unique
    private @Nullable IngotPileBlockEntity tfg$getTopTowerPile() {
        final IngotPileBlockEntity pile = (IngotPileBlockEntity) (Object) this;
        final Level level = pile.getLevel();
        final BlockState state = pile.getBlockState();
        if (level == null || !(state.getBlock() instanceof IngotPileBlock pileBlock)) {
            return null;
        }

        BlockPos topPos = pile.getBlockPos();
        while (Helpers.isBlock(level.getBlockState(topPos.above()), pileBlock)) {
            topPos = topPos.above();
        }

        return level.getBlockEntity(topPos) instanceof IngotPileBlockEntity topPile ? topPile : null;
    }

    @Unique
    private ItemStack tfg$getTopEntryStack() {
        final IngotPileBlockEntity pile = tfg$getTopTowerPile();
        return pile == null ? ItemStack.EMPTY : tfg$getTopEntryStack(pile);
    }

    @Unique
    private ItemStack tfg$getTopEntryStack(IngotPileBlockEntity pile) {
        final List<?> entries = ((IIngotPileBlockEntityAccessor) (Object) pile).getEntries();
        if (entries.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final Object entry = entries.get(entries.size() - 1);
        return ((IIngotPileBlockEntityEntryAccessor) entry).getStack();
    }

    @Unique
    private static boolean tfg$isSupportedIngot(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(TFCTags.Items.PILEABLE_INGOTS)
                || stack.is(TFCTags.Items.PILEABLE_DOUBLE_INGOTS));
    }
}
