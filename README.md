# Nsubtle — Not That Subtle World

一个面向 **Minecraft 1.21.10 / NeoForge 21.10.64** 的小型玩法模组。名字里的 “Subtle” 是反话：
这里塞进去的机制基本都不太讲道理——护甲会被打穿、骨粉催树会爆炸、拿桶水就能在陆地上捅三叉戟。

> English: A small NeoForge 1.21.10 mod built around deliberately unfair combat and interaction mechanics:
> the armor-breaking **Broken Defense** debuff, the **Tough** enchantment that resists it, bonemeal-triggered
> tree explosions, water-bucket trident tricks and instant wet farmland.

---

## 功能一览

| 机制 | 简介 | 主要实现 |
| --- | --- | --- |
| 破防（Broken Defense） | 受击时概率放大伤害、叠加层数；层数满后摧毁护甲 | [`NsubtleEffects`](src/main/java/com/nsubtle/effect/NsubtleEffects.java) |
| 破防药水 | 粗制的药水 + 爆裂粉末酿造，30 秒破防 I | [`PotionRecipeMixin`](src/main/java/com/nsubtle/mixin/PotionRecipeMixin.java) |
| 坚韧（Tough）附魔 | 4 级盔甲附魔，每级降低 2% 破防触发率 | [`EnchantmentList`](src/main/java/com/nsubtle/list/EnchantmentList.java) |
| 骨粉树爆 | 骨粉催熟的树真正长成时，在树苗位置炸一下 | [`TreeBoomMixin`](src/main/java/com/nsubtle/mixin/TreeBoomMixin.java) |
| 成就「催生？」 | 首次炸树获得，goal 稀有帧，图标为原版树苗 | [`tree_boom.json`](src/main/resources/data/nsubtle/advancement/tree_boom.json) |
| 水桶三叉戟 | 副手拿水桶时，陆地也能使用/释放三叉戟 | [`WaterBucketMixin`](src/main/java/com/nsubtle/mixin/WaterBucketMixin.java) |
| 锄 + 水桶 | 主手锄、副手水桶右键泥土类方块 → 湿润耕地 | [`ModEvents`](src/main/java/com/nsubtle/events/ModEvents.java) |
| 食物 | 「羹」随机六选三效果、「金头」快速食用 + 多重增益 | [`FoodList`](src/main/java/com/nsubtle/list/FoodList.java) |

具体数值、公式与配方见 **[docs/MECHANICS.md](docs/MECHANICS.md)**。

---

## 环境要求

| 依赖 | 版本 |
| --- | --- |
| JDK | 21 |
| Minecraft | 1.21.10 |
| NeoForge | 21.10.64 |
| ModDevGradle | 2.0.141 |
| Gradle | 9.7.1（wrapper 指向腾讯云镜像） |
| Parchment | 2025.10.12 |

---

## 快速开始

### IntelliJ IDEA（推荐）

1. 用 IDEA 打开项目根目录 `Nsubtle`，等待 Gradle 同步完成。
2. 在右上角运行配置中选择：
   - `client` —— 启动带模组的客户端
   - `server` —— 启动服务端（`--nogui`）
   - `gameTestServer` —— 运行 GameTest
   - `clientData` / `serverData` —— 生成数据包资源

### 命令行

仓库同时提供 Gradle wrapper：

```powershell
# 启动客户端
.\gradlew runClient

# 仅编译
.\gradlew compileJava

# 构建 jar（输出到 build/libs）
.\gradlew build
```

> 首次运行 wrapper 会从腾讯云镜像下载 Gradle 9.7.1（已缓存的机器会直接复用）。
>
> 注意：Gradle 需要写入 `%USERPROFILE%\.gradle`（native 库缓存）。在受限沙箱里运行 CLI 构建会报
> `Could not initialize native services`，此时请改用 IDEA 构建，或放开该目录的写权限。

---

## 数据生成

本项目把 datagen 拆成客户端/服务端两条流水线，产物直接提交到 `src/generated`：

| 任务 | 输出目录 | 内容 |
| --- | --- | --- |
| `runClientData` | `src/generated/resources/client` | 模型、方块状态、`en_us` / `zh_cn` 语言文件 |
| `runServerData` | `src/generated/resources/server` | 配方、战利品表、附魔定义、标签、进度 |

```powershell
.\gradlew runClientData
.\gradlew runServerData
```

**不要手改 `src/generated` 下的文件**，它们会在下次 datagen 时被覆盖。要改内容请修改
`src/main/java/com/nsubtle/datagen/` 下对应的 provider。

---

## 目录结构

```
src/main/java/com/nsubtle/
├─ Nsubtle.java              入口：挂载注册与事件
├─ DataGen.java              datagen 事件，注册所有 provider
├─ events/
│  ├─ EventsLoader.java      所有 DeferredRegister / 事件监听器的统一挂载点
│  ├─ RegistryEvents.java    物品、方块、效果、药水、附魔效果组件
│  └─ ModEvents.java         创造页填充、锄+水桶交互
├─ effect/
│  ├─ NsubtleEffect.java     效果基类
│  └─ NsubtleEffects.java    破防效果与受击结算（核心玩法）
├─ list/                     注册项的属性/数值表
│  ├─ BlockList.java
│  ├─ EnchantmentList.java
│  └─ FoodList.java
├─ datagen/                  模型、语言、配方、战利品、附魔与标签的 provider
├─ mixin/                    PotionRecipe / TreeBoom / WaterBucket
└─ util/
   └─ TreeBoomHelper.java    骨粉催熟上下文与爆炸逻辑

src/main/resources/          手写资源：纹理、mixin 配置
src/main/templates/          neoforge.mods.toml 模板（占位符由 Gradle 展开）
src/generated/resources/     datagen 产物（client / server 分目录）
```

---

## 开发约定

- **注册集中**：所有 `DeferredRegister` 定义在 `RegistryEvents`，由 `EventsLoader` 统一 `register` 到 mod 总线。
- **数值分离**：物品/方块/食物的具体属性放在 `list` 包，注册类只做注册。
- **客户端/服务端分离**：客户端资源与服务端数据分别生成，避免把客户端模型塞进服务端包。
- **Mixin 逻辑外置**：需要跨类共享的状态（例如“正在被骨粉催熟”）放普通工具类，不要放 Mixin 类——
  注入后的代码运行在目标类里，访问 Mixin 类的私有成员会在运行时失败。
- **编码统一 UTF-8**（`build.gradle` 已配置）。

---

## 已知限制 / 后续计划

- **祭坛框架（Altar Frame）尚未实现玩法**：目前只有方块、掉落物、配方、贴图，没有 BlockEntity 或交互逻辑。
- **破防数值偏激进**：10 层时伤害 ×2.1；耐久损耗已改为受击概率触发（低频高额），仍需实测平衡。
- **锄 + 水桶不消耗水、不损耗锄**：属于刻意保留的“取巧”机制，是否调整待定。
- **缺少自动化测试与 CI**：目前只有 GameTest 运行配置，没有实际用例。
- **`mods.toml` 的作者/主页/更新检查仍未填写**。

---

## 许可

本项目使用 **MIT** 许可（见 `gradle.properties` 中的 `mod_license`）。

项目骨架来自 [NeoForge MDK](https://github.com/NeoForged/MDK)，模板部分遵循
`TEMPLATE_LICENSE.txt` 中的 MIT 许可。
