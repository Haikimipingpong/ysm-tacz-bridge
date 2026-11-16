package rip.ysmtacz.bridge;

import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import net.minecraft.client.Minecraft;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;

/** TaCZ Fabric 事件：开火 / 近战 / 换弹时打标记。 */
final class TacEvents {

    private TacEvents() {
    }

    static void register() {
        GunFireEvent.CALLBACK.register(event -> {
            TacBindings.markGunAction(event.getShooter());
            // 本机玩家开火：立刻置位，避免火光在延迟提交时错过 isSelf 窗口
            if (event.getShooter() == Minecraft.getInstance().player) {
                MuzzleFlashRender.isSelf = true;
                ShellRender.isSelf = true;
            }
        });
        GunMeleeEvent.CALLBACK.register(event -> TacBindings.markGunAction(event.getShooter()));
        GunReloadEvent.CALLBACK.register(event -> TacBindings.markGunAction(event.getEntity()));
    }
}
