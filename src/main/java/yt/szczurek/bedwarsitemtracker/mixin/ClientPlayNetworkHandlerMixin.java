package yt.szczurek.bedwarsitemtracker.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import yt.szczurek.bedwarsitemtracker.client.BedwarsitemtrackerClient;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

//    @Inject(
//            method = "onItemPickupAnimation",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/item/ItemStack;isEmpty()Z"
//
//            ),
//            slice = @Slice(
//                    from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;getStack()Lnet/minecraft/item/ItemStack;"),
//                    to = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ItemPickupAnimationS2CPacket;getStackAmount()I")
//            )
//    )
//    private void logItemPickup(ItemPickupAnimationS2CPacket packet, CallbackInfo ci, @Local ItemStack item) {
//        BedwarsitemtrackerClient.logItemPickup(item);
//    }

    @WrapOperation(method = "onEntitySpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;playSpawnSound(Lnet/minecraft/entity/Entity;)V"))
    private void markSpawnedItems(ClientPlayNetworkHandler instance, Entity entity, Operation<Void> original, @Local(argsOnly = true) EntitySpawnS2CPacket packet) {
        if (EntityType.ITEM == entity.getType()) {
            BedwarsitemtrackerClient.addEntityToCheck(packet.getEntityId());
        }
    }

    @ModifyExpressionValue(
            method = "onScreenHandlerSlotUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;getStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack storeCurrentItemStack(ItemStack original, @Share("playerItem") LocalRef<ItemStack> currentItemStack) {
        currentItemStack.set(original);
        return original;
    }

    @WrapOperation(
            method = "onScreenHandlerSlotUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;setBobbingAnimationTime(I)V")
    )
    private void logItemPickup(ItemStack item, int bobbingAnimationTime, Operation<Void> original, @Share("playerItem") LocalRef<ItemStack> currentItemStack) {
        BedwarsitemtrackerClient.logItemPickup(item.getItem(), item.getCount() - currentItemStack.get().getCount());
    }
}
