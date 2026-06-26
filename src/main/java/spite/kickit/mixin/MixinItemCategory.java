package spite.kickit.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(CreativeModeTab.class)
public abstract class MixinItemCategory {

    private static volatile Field displayItemsField;
    private static volatile boolean reflectionInitialized;

    private static Field getDisplayItemsField() throws ReflectiveOperationException {
        if (reflectionInitialized) return displayItemsField;
        synchronized (MixinItemCategory.class) {
            if (reflectionInitialized) return displayItemsField;
            displayItemsField = CreativeModeTab.class.getDeclaredField("displayItems");
            displayItemsField.setAccessible(true);
            reflectionInitialized = true;
            return displayItemsField;
        }
    }

    @Inject(method = "buildContents", at = @At("RETURN"))
    private void kickit$removeMalumPrideFromCreative(
            CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        try {
            Field field = getDisplayItemsField();
            if (field == null) return;

            @SuppressWarnings("unchecked")
            Collection<ItemStack> displayItems = (Collection<ItemStack>) field.get(this);
            if (displayItems == null) return;

            // Wrap in ArrayList to avoid ConcurrentModificationException on LinkedSet
            List<ItemStack> toRemove = new ArrayList<>();
            for (ItemStack stack : displayItems) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id != null
                    && "malum".equals(id.getNamespace())
                    && id.getPath().endsWith("_prideweave")) {
                    toRemove.add(stack);
                }
            }
            displayItems.removeAll(toRemove);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
