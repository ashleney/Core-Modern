package su.terrafirmagreg.core.mixins.common.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.simibubi.create.AllTags.AllItemTags;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsInputHandler;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@Mixin(value = ValueSettingsInputHandler.class, remap = false)
public class ValueSettingsInputHandlerMixin {
    private static final TagKey<Item> FORGE_WRENCHES = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath("forge", "tools/wrenches"));
    private static final TagKey<Item> C_WRENCHES = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath("c", "wrenches"));

    @Redirect(method = "onBlockActivated(Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock;)V", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/AllTags$AllItemTags;matches(Lnet/minecraft/world/item/ItemStack;)Z"), remap = false)
    private static boolean tfg$allowAnyWrench(AllItemTags tag, ItemStack stack) {
        return tag.matches(stack) || stack.is(FORGE_WRENCHES) || stack.is(C_WRENCHES);
    }
}
