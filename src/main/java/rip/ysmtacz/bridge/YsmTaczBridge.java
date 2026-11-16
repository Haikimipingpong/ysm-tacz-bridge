package rip.ysmtacz.bridge;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** 入口：装了 TaCZ 才注册枪械事件。 */
public final class YsmTaczBridge implements ClientModInitializer {

    public static boolean taczLoaded() {
        return FabricLoader.getInstance().isModLoaded("tacz");
    }

    @Override
    public void onInitializeClient() {
        if (taczLoaded()) {
            TacEvents.register();
        }
    }
}
