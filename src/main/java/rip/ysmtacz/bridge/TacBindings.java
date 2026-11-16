package rip.ysmtacz.bridge;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** TaCZ 的 molang 变量（tac_*）。 */
public final class TacBindings {

    private TacBindings() {
    }

    public static void register(CtrlBinding binding) {
        binding.livingEntityVar("tac_hold_gun", ctx -> IGun.mainhandHoldGun(ctx.entity()));
        binding.livingEntityVar("tac_gun_type", ctx -> gunType(ctx.entity().getMainHandItem()));
        binding.livingEntityVar("tac_gun_id", ctx -> gunId(ctx.entity().getMainHandItem()));
        binding.livingEntityVar("tac_is_fire", ctx -> {
            IGunOperator operator = IGunOperator.fromLivingEntity(ctx.entity());
            return operator != null && operator.getSynShootCoolDown() > 0;
        });
        binding.livingEntityVar("tac_is_aim", ctx -> {
            IGunOperator operator = IGunOperator.fromLivingEntity(ctx.entity());
            return operator != null && operator.getSynAimingProgress() > 0.0f;
        });
        binding.livingEntityVar("tac_is_reload", ctx -> {
            IGunOperator operator = IGunOperator.fromLivingEntity(ctx.entity());
            return operator != null && operator.getSynReloadState().getCountDown() > 0;
        });
        binding.livingEntityVar("tac_is_melee", ctx -> {
            IGunOperator operator = IGunOperator.fromLivingEntity(ctx.entity());
            return operator != null && operator.getSynMeleeCoolDown() > 0;
        });
        binding.livingEntityVar("tac_is_draw", ctx -> {
            IGunOperator operator = IGunOperator.fromLivingEntity(ctx.entity());
            return operator != null && operator.getSynDrawCoolDown() > 0;
        });
        binding.livingEntityVar("tac_fire_mode", ctx -> {
            FireMode fireMode = IGun.getMainHandFireMode(ctx.entity());
            return fireMode != null && fireMode != FireMode.UNKNOWN ? fireMode.name() : StringPool.EMPTY;
        });
    }

    static String gunType(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) {
            return StringPool.EMPTY;
        }
        Optional<CommonGunIndex> index = TimelessAPI.getCommonGunIndex(gun.getGunId(stack));
        return index.map(CommonGunIndex::getType).orElse(StringPool.EMPTY);
    }

    static String gunId(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        return gun == null ? StringPool.EMPTY : gun.getGunId(stack).toString();
    }

    /** 开火/换弹/近战时给 capability 打标记，让动画状态机立刻切动作。 */
    static void markGunAction(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        PlayerCapability.get(entity).ifPresent(cap -> cap.setExtraRenderFlag(true));
    }
}
