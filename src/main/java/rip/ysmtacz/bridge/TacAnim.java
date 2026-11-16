package rip.ysmtacz.bridge;

import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import rip.ysm.compat.gun.tacz.ConditionTAC;

import java.util.Locale;
import java.util.Optional;

/** TaCZ 枪械驱动的动画状态机（从 OpenYSM 上游 forge 实现移植）。 */
public final class TacAnim {

    private static final boolean DEBUG = Boolean.getBoolean("ysmTaczBridge.debug");

    private TacAnim() {
    }

    public static boolean isGunItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof IGun;
    }

    /** 「tac:xxx」优先；没有对应动画就退回模型自带的同名动画。 */
    public static PlayState handleTaczAnimState(AnimationEvent<?> event, String animation, ILoopType loopType) {
        String taczAnimation = "tac:" + animation;
        if (event.getAnimatable().getAnimation(taczAnimation) != null) {
            return setAnimationAndContinue(event, taczAnimation, loopType);
        }
        return setAnimationAndContinue(event, animation, loopType);
    }

    /** 持枪待机：瞄准 / 跑动 / 游泳爬行 / 普通持枪。 */
    public static PlayState handleGunHold(AnimationEvent<? extends LivingAnimatable<?>> event, ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) {
            return PlayState.STOP;
        }
        Optional<CommonGunIndex> indexOptional = TimelessAPI.getCommonGunIndex(gun.getGunId(stack));
        if (indexOptional.isEmpty()) {
            return PlayState.STOP;
        }
        String type = indexOptional.get().getType();
        LivingEntity entity = event.getAnimatable().getEntity();
        IGunOperator operator = IGunOperator.fromLivingEntity(entity);
        if (operator == null) {
            return PlayState.STOP;
        }
        if (!entity.isSwimming() && entity.getPose() == Pose.SWIMMING) {
            if (Math.abs(event.getLimbSwingAmount()) > 0.05d) {
                return playGunAnimation(event, type, "tac:climb:");
            }
            return playGunAnimation(event, type, "tac:climbing:");
        }
        if (operator.getSynAimingProgress() > 0.0f) {
            return playGunAnimation(event, type, "tac:aim:");
        }
        if (entity.onGround() && entity.isSprinting()) {
            return playGunAnimation(event, type, "tac:run:");
        }
        return playGunAnimation(event, type, "tac:hold:");
    }

    /** 开枪 / 换弹 / 近战。 */
    public static PlayState handleGunAction(AnimationEvent<? extends LivingAnimatable<?>> event, ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) {
            return PlayState.STOP;
        }
        Optional<CommonGunIndex> indexOptional = TimelessAPI.getCommonGunIndex(gun.getGunId(stack));
        if (indexOptional.isEmpty()) {
            return PlayState.STOP;
        }
        String type = indexOptional.get().getType();
        LivingAnimatable<?> animatable = event.getAnimatable();
        LivingEntity entity = animatable.getEntity();
        IGunOperator operator = IGunOperator.fromLivingEntity(entity);
        if (operator == null) {
            return PlayState.STOP;
        }
        long shootCoolDown = operator.getSynShootCoolDown();
        if (entity instanceof LocalPlayer localPlayer) {
            IClientPlayerGunOperator clientOperator = IClientPlayerGunOperator.fromLocalPlayer(localPlayer);
            if (clientOperator != null) {
                shootCoolDown = Math.max(shootCoolDown, clientOperator.getClientShootCoolDown());
            }
        }
        if (animatable.isExtraRenderFlag()) {
            playLoopAnimation(event, "empty");
        }
        animatable.setExtraRenderFlag(false);
        if (operator.getSynReloadState().getCountDown() > 0) {
            return playGunAnimationWithMode(event, type, "tac:reload:", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
        }
        if (operator.getSynMeleeCoolDown() > 0) {
            return playGunAnimationWithMode(event, type, "tac:melee:", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
        }
        if (shootCoolDown > 0) {
            float aimingProgress = operator.getSynAimingProgress();
            if (!entity.isSwimming() && entity.getPose() == Pose.SWIMMING && Math.abs(event.getLimbSwingAmount()) <= 0.05d) {
                return playGunAnimationWithMode(event, type, "tac:climbing:fire:", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
            }
            if (aimingProgress > 0.0f) {
                return playGunAnimationWithMode(event, type, "tac:aim:fire:", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
            }
            return playGunAnimationWithMode(event, type, "tac:hold:fire:", ILoopType.EDefaultLoopTypes.PLAY_ONCE);
        }
        return PlayState.CONTINUE;
    }

    @NotNull
    private static PlayState playGunAnimation(AnimationEvent<?> event, String type, String prefix) {
        return playGunAnimationWithMode(event, type, prefix, ILoopType.EDefaultLoopTypes.LOOP);
    }

    @NotNull
    private static PlayState playGunAnimationWithMode(AnimationEvent<?> event, String type, String prefix, ILoopType loopType) {
        String matched = "";
        if (event.getAnimatable() instanceof LivingAnimatable<?> animatable) {
            ConditionTAC tac = null;
            try {
                tac = animatable.getModelConfig() == null ? null : animatable.getModelConfig().getTAC();
            } catch (RuntimeException ignored) {
                tac = null;
            }
            if (tac != null) {
                matched = tac.doTest(animatable.getEntity().getMainHandItem(), prefix);
                if (StringUtils.isNoneBlank(matched)) {
                    return setAnimationAndContinue(event, matched, loopType);
                }
            }
        }
        if (DEBUG) {
            String chosen2 = isGunType(type, GunTabType.PISTOL) ? prefix + "pistol"
                    : isGunType(type, GunTabType.RPG) ? prefix + "rpg" : prefix + "rifle";
            System.out.println("[YSM-TACZ] gunType=" + type + " prefix=" + prefix
                    + " 专属动作=" + (matched == null || matched.isEmpty() ? "无" : matched)
                    + " -> " + chosen2);
        }
        if (isGunType(type, GunTabType.PISTOL)) {
            return setAnimationAndContinue(event, prefix + "pistol", loopType);
        }
        if (isGunType(type, GunTabType.RPG)) {
            return setAnimationAndContinue(event, prefix + "rpg", loopType);
        }
        return setAnimationAndContinue(event, prefix + "rifle", loopType);
    }

    @NotNull
    private static PlayState playLoopAnimation(AnimationEvent<?> event, String animation) {
        return setAnimationAndContinue(event, animation, ILoopType.EDefaultLoopTypes.LOOP);
    }

    @NotNull
    private static PlayState setAnimationAndContinue(AnimationEvent<?> event, String animation, ILoopType loopType) {
        event.getController().setAnimation(animation, loopType);
        return PlayState.CONTINUE;
    }

    private static boolean isGunType(String type, GunTabType gunTabType) {
        return type != null && type.equals(gunTabType.name().toLowerCase(Locale.ENGLISH));
    }
}
