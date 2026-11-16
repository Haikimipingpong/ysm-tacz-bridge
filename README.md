# YSM x TaCZ Bridge

> ## ⚠️ 维护状态：**不维护 / 极少维护**
> 本仓库是那份[移植版](https://github.com/Haikimipingpong/openysm-1.21.11-fabric)的配套件，
> 同样是给 **macOS 上的 Minecraft 1.21.11 + Fabric 玩家**用的：让 YSM 这个替代品能适配 TaCZ。
> **作者不打算持续维护**——不保证跟进 TaCZ / YSM 的新版本，不保证修 bug、不保证回 issue。
> 能用就用；不能用就 fork 走。

> ## ⚠️ 这是配套件：**单独加载不会有任何效果**
> 本模组只给**配套仓库里的那份 YSM**（[OpenYSM 1.21.11 / Fabric 移植版](https://github.com/Haikimipingpong/openysm-1.21.11-fabric)）
> 做 TaCZ 适配。没有那份 YSM，它没有可打补丁的对象，**装进去等于没装**。
> 不要把它单独丢进一个没有 YSM 的整合包，然后来问为什么没效果。

> ## 🔗 配套仓库：**[OpenYSM 1.21.11 / Fabric 移植版 →](https://github.com/Haikimipingpong/openysm-1.21.11-fabric)**
> 先装那个（1.21.11 + Fabric 的 YSM 替代品），再装本模组，TaCZ 联动才完整。

中文 | [English](#english)

给**配套仓库里的那份 YSM**补上 TaCZ（Timeless & Classics Zero）联动的桥接模组。
它不改 YSM 的 jar，而是用 mixin 把 TaCZ 兼容层缺的东西接上；**脱离 YSM 单独存在时不做任何事**。

## 先说明几件事

- **本项目是非官方的**，与 YSM / OpenYSM 的作者、与 TaCZ 的作者都没有隶属关系。
- **定位**：给 **macOS** 上跑 **Minecraft 1.21.11 + Fabric** 的玩家，把 YSM 这个替代品接上 TaCZ
  （持枪 / 瞄准 / 开火 / 换弹 / 近战动作与 `tac_*` 变量）。**不维护 / 极少维护**，只做自用与存档。
- **上游 OpenYSM 只发布过 Minecraft 1.20.1 / Forge**，从未发布 Fabric 版本，更没有 Fabric 版的 TaCZ 联动；
  本项目做的是把上游 Forge 侧那套逻辑搬到 Fabric 上，让 Fabric 构建也能用。
- 下文的 `openysm-fabric-2.6.5.9` 一律指**社区流传的 Fabric 构建**，它**不是**上游发布的版本，
  我们也不知道它是谁做的；这里不使用「官方版」这种说法。
- 本项目以 **MIT** 发布（见 [LICENSE](LICENSE)）。其中动画状态机逻辑**派生自 OpenYSM 上游
  Forge 实现**（`TacAnimHandler` 等，MIT，版权归 OpenYSM）；mixin、事件、molang 绑定是本项目新增的。

## 为什么需要它

YSM 这一系的 TaCZ 兼容层都有缺口，本模组就是补这些缺口：

- 公开发布的 `openysm-fabric-2.6.5.9`（社区构建，并非上游发布）里，兼容层基本是空壳；
- 配套的移植版有基础实现（持枪 / 瞄准状态能跑），但 `handleItemSound` 会立刻把 `isSelf` 复位，
  **额外枪包**（延迟提交渲染的那类）的枪口火光 / 抛壳就画不出来。

2.6.5.9 那份的情况长这样：

```java
rip.ysm.compat.gun.tacz.fabric.TacCompatImpl
  isLoaded() -> false                    // 恒 false
  handleTaczAnimState(...) -> null
  handleGunHoldAnimState(...) -> null
  handleGunActionAnimState(...) -> null
  getGunTexture(stack) -> null           // 1.1.0 修
  handleGunSound(...) / handleItemSound(...) // 空方法，1.2.0 修
  registerControllerFunctions(...)       // 只注册 9 个恒为默认值的 tac_* 变量
```

结果就是：抱着枪也永远播走路动画，模型的 `tac:*` 动画全是死的。本模组把这些方法换成真实现。

## 装上之后

- `tac_hold_gun` / `tac_gun_type` / `tac_gun_id` / `tac_is_fire` / `tac_is_aim` / `tac_is_reload` /
  `tac_is_melee` / `tac_is_draw` / `tac_fire_mode` 这 9 个 molang 变量返回真实值。
- 持枪走路 / 瞄准 / 跑动 / 游泳 / 爬行 → `tac:hold:rifle`、`tac:aim:rifle`、`tac:run:rifle` …
- 开火 / 换弹 / 近战 → `tac:hold:fire:rifle`、`tac:reload:rifle`、`tac:melee:rifle`（一次性播放）。
- 模型专属动作 `tac:hold$tacz:ak47` 之类也会生效（1.1.0 起）。

**注意**：这些都依赖模型自身带 `tac:*` 动画。默认模型有；部分第三方模型没有，
表现就和没装一样。

## 依赖

- Minecraft **1.21.11** / Fabric Loader ≥ 0.19 / Java 21 / Fabric API
- YSM 的 Fabric 构建：**配套的移植版**（推荐），或公开发布的 `2.6.5.9` 构建（要求 ≥ 2.6.5）
- TaCZ Refabricated 1.21.11

## 构建

```bash
export GRADLE_USER_HOME=$PWD/.gradle-home
gradle build
# 产物：build/libs/ysm-tacz-bridge-1.3.0-fabric.jar
```

`libs/` 里放两个**仅编译期**用的 jar（不打进产物）：

| 依赖 | 用途 |
|---|---|
| `openysm-2.6.5.9.jar` | mixin 目标的类型解析 |
| `TACZ-Refabricated-1.21.11.jar` | TaCZ API |

## 版本说明（每个版本修的是什么）

### 1.1.0：模型专属持枪动作

YSM 的专属动作命名是 `前缀$枪ID`，例如 `tac:hold$tacz:minigun`。
匹配由 `ConditionTAC` 完成，而它依赖 `TacCompat.getGunTexture(stack)` 取枪 ID
——这个方法在原 jar 里同样是空实现（返回 `null`），所以专属动作永远命中不了，
所有模型都只能落到通用的 `tac:hold:rifle` / `tac:hold:pistol`。

1.1.0 补上了 `getGunTexture`：

- 模型有 `tac:hold$tacz:ak47` → 用模型自己的动作
- 模型没有 → 回落 `tac:hold:rifle`（旧行为）

排查开关：`-DysmTaczBridge.debug=true`，日志会打印 `getGunTexture item=... id=...`
和 `gunType=... 专属动作=... -> ...`。

### 1.2.0：恢复枪口火光 / 抛壳

那个构建还有两个空实现：`TacCompatImpl.handleGunSound(LivingEntity, ItemStack)` 与
`TacCompatImpl.handleItemSound(ItemStack)`。

TaCZ 的 `MuzzleFlashRender.extract()` 里判断是：

```java
if (!IrisCompat.isRenderShadow() && isSelf) {
    long time = System.currentTimeMillis() - shootTimeStamp;
    if (time >= 0 && time <= 50L) { /* 画火光 */ }
}
```

`isSelf` 只有在「渲染本机玩家手持的枪」时才该为 true，否则第三人称下会被当成「别人的枪」而不画。
OpenYSM 的 `CustomPlayerItemInHandLayer` 会在渲染主手物品前后分别调用这两个方法，
原实现是空的 → `isSelf` 永远是 false → **模型持枪开火没有火光**。

1.2.0 补上：

```java
handleGunSound: if (entity == Minecraft.getInstance().player) { MuzzleFlashRender.isSelf = true; ShellRender.isSelf = true; }
handleItemSound: MuzzleFlashRender.isSelf = false; ShellRender.isSelf = false;
```

排查开关：`-DysmTaczBridge.debug=true` 会打印 `handleGunSound entity=... self=true`。

### 1.3.0：额外枪包没有火光

TaCZ 自己是在原版 `ItemInHandLayer.submitArmWithItem` 的 HEAD 里置 `isSelf = true`、
在 `submit` 的 TAIL 里复位（`com.tacz.guns.mixin.client.ItemInHandLayerMixin`）。
YSM 绕开了原版那一层，所以只能用 `handleGunSound` 复刻。

但 1.2.0 在 `handleItemSound` 里立刻复位，对「同步渲染」的默认枪包没问题；
额外枪包有些把枪模型 / 火光放进**延迟提交**（collector 或 mesh 路径），等真正画的时候
`isSelf` 已经被清成 false → 火光永远不画。

1.3.0 改成：

- `handleGunSound`：本机玩家 → 置 `true`
- `handleItemSound`：**不再复位**（no-op）
- `GunFireEvent`：本机玩家开火时也立刻置 `true`

安全性：`MuzzleFlashRender.extract()` 除了 `isSelf` 还有
`System.currentTimeMillis() - shootTimeStamp <= 50ms` 的时间窗，
所以标记常亮也只会在开火后 50ms 内真正画出来，不会一直亮。

## English

A mixin bridge that adds **TaCZ** support to the **Fabric** build of YSM / OpenYSM — without
modifying the YSM jar. **Companion only:** it patches the YSM from the
[companion repository](https://github.com/Haikimipingpong/openysm-1.21.11-fabric); loaded on its
own it does nothing at all. That 2.6.5.9 Fabric build (not published upstream) ships an empty TaCZ compatibility layer
(`isLoaded()` returns false, all handlers return null), so every model falls back to walking
animations and the `tac:*` molang variables stay at their defaults.

Unofficial. Upstream OpenYSM ships **Minecraft 1.20.1 / Forge only**; this project ports the
upstream Forge-side logic to Fabric.

**Who this is for / maintenance:** the companion piece to that port — for **macOS players on
Minecraft 1.21.11 + Fabric** who want YSM with TaCZ. **Unmaintained / barely maintained**: no
tracking of new TaCZ or YSM versions, no support. Fork it if you need it to last.

MIT licensed — the animation-state logic is derived from
upstream OpenYSM (MIT, © OpenYSM); the mixin, events and molang bindings are new.

**Companion repository:** [OpenYSM 1.21.11 / Fabric port](https://github.com/Haikimipingpong/openysm-1.21.11-fabric) —
install that first, then this bridge: on its own it patches nothing and does nothing.

---

## 后记：这玩意儿到底是怎么来的

> **老子要用，老子发现它用不了，老子要让它能用，老子让它能用了。**

过程就是这样：从一个 **R-18 整合包**里把它挖出来（一座能跑、但多看一眼掉头发的屎山），
把整合包里所有 R-18 模组全剔了留着自己用，结果发现 **它不支持 TaCZ**——于是有了这个。

能用就行，别问架构。
