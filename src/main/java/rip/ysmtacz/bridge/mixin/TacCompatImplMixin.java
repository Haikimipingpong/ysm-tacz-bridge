package rip.ysmtacz.bridge.mixin;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rip.ysmtacz.bridge.TacAnim;
import rip.ysmtacz.bridge.TacBindings;
import rip.ysmtacz.bridge.YsmTaczBridge;

/**
 * 官方 2.6.5.9 的 Fabric 构建里，TaCZ 兼容层全部是空实现
 * （isLoaded 恒 false、handleXxx 恒 null）。这里用 mixin 把真正的实现接上去，
 * 不需要改官方 jar。
 */
@Mixin(targets = "rip.ysm.compat.gun.tacz.fabric.TacCompatImpl")
public class TacCompatImplMixin {

    @Inject(method = "isLoaded", at = @At("HEAD"), cancellable = true)
    private static void ysmTaczBridge$isLoaded(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(YsmTaczBridge.taczLoaded());
    }

    @Inject(method = "registerControllerFunctions", at = @At("HEAD"), cancellable = true)
    private static void ysmTaczBridge$registerFunctions(CtrlBinding binding, CallbackInfo ci) {
        TacBindings.register(binding);
        ci.cancel();
    }

    @Inject(method = "handleTaczAnimState", at = @At("HEAD"), cancellable = true)
    private static void ysmTaczBridge$handleTaczAnimState(LivingEntity entity, AnimationEvent<? extends LivingAnimatable<?>> event,
                                                          String animation, ILoopType loopType,
                                                          CallbackInfoReturnable<PlayState> cir) {
        if (TacAnim.isGunItem(entity.getMainHandItem())) {
            cir.setReturnValue(TacAnim.handleTaczAnimState(event, animation, loopType));
        }
    }

    /**
     * 模型专属持枪动作（{@code tac:hold$tacz:ak47} 这种带 $ 的命名）靠 ConditionTAC 匹配，
     * 而 ConditionTAC 又是通过 TacCompat.getGunTexture(stack) 拿枪 ID 的 —— 官方这里也返回 null，
     * 所以专属动作永远匹配不到。这里补上真实的枪 ID。
     */
    @Inject(method = "getGunTexture", at = @At("HEAD"), cancellable = true)
    private static void ysmTaczBridge$getGunTexture(ItemStack stack, CallbackInfoReturnable<Identifier> cir) {
        IGun gun = IGun.getIGunOrNull(stack);
        Identifier id = gun == null ? null : gun.getGunId(stack);
        if (Boolean.getBoolean("ysmTaczBridge.debug")) {
            System.out.println("[YSM-TACZ] getGunTexture item=" + stack.getItem() + " id=" + id);
        }
        cir.setReturnValue(id);
    }

    /**
     * 本机玩家持枪时，TaCZ 的枪口火焰/抛壳需要标记为「自己」，否则第三人称下会被当作他人而不绘制。
     * 官方这两个方法也是空的，所以模型持枪开枪没有火光。
     */
    @Inject(method = "handleGunSound", at = @At("HEAD"), cancellable = true)
    private static void ysmTaczBridge$handleGunSound(LivingEntity entity, ItemStack stack, CallbackInfo ci) {
        boolean self = entity != null && entity == Minecraft.getInstance().player;
        if (self) {
            MuzzleFlashRender.isSelf = true;
            ShellRender.isSelf = true;
        }
        if (Boolean.getBoolean("ysmTaczBridge.debug")) {
            System.out.println("[YSM-TACZ] handleGunSound entity=" + (entity == null ? "null" : entity.getName().getString())
                    + " self=" + self);
        }
        ci.cancel();
    }

    /**
     * 不要在这里清标记：TaCZ 的火光/抛壳可能延迟到本次 item 渲染之后才提交，
     * 一清就永远画不出来（默认枪包同步渲染所以看不出问题，额外枪包就没了）。
     * 火光本身受 MuzzleFlashRender 的 50ms 时间窗约束，标记常亮不会一直亮。
     */
    @Inject(method = "handleItemSound", at = @At("HEAD"), cancellable = true)
    private static void ysmTaczBridge$handleItemSound(ItemStack stack, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "handleGunHoldAnimState", at = @At("HEAD"), cancellable = true)
    private static void ysmTaczBridge$handleGunHold(ItemStack stack, AnimationEvent<? extends LivingAnimatable<?>> event,
                                                    CallbackInfoReturnable<PlayState> cir) {
        if (TacAnim.isGunItem(stack)) {
            cir.setReturnValue(TacAnim.handleGunHold(event, stack));
        }
    }

    @Inject(method = "handleGunActionAnimState", at = @At("HEAD"), cancellable = true)
    private static void ysmTaczBridge$handleGunAction(ItemStack stack, AnimationEvent<? extends LivingAnimatable<?>> event,
                                                     CallbackInfoReturnable<PlayState> cir) {
        if (TacAnim.isGunItem(stack)) {
            cir.setReturnValue(TacAnim.handleGunAction(event, stack));
        }
    }
}
