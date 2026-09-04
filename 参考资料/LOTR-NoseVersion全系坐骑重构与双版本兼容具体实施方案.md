# LOTR-NoseVersion 全系坐骑重构与双版本兼容具体实施方案

> **方案版本**：v1.1.0（存储与双版本UI深度增强版）  
> **编制日期**：2026-09-04  
> **实施目标**：
> 1. 构建环境升级至现代化体系（对标 `LegacyModdingMC/UniMixins` `example-fg-g6`，基于 Gradle 6.9.1 + GTNH ForgeGradle 1.2.11，**严格不引入 UniMixins 与任何 Mixin 依赖**）；
> 2. 彻底打破硬编码限制，设计并落地全系坐骑通用适配器架构（`IMountAdapter`），全覆盖魔戒传承版与重制版内全部 18+ 种可骑乘生物；
> 3. **构建工业级高可靠数据存储引擎**：引入 `.dat.tmp` 写入、`.dat.bak` 双重容灾自动回退、损坏隔离保护（拒绝静默清空）与内存快照并发隔离；
> 4. **双版本 UI 像素级深度兼容**：采用纯原版 `GuiButton` 零依赖解耦，实现动态双排网格重排算法（完美对齐传承版 7 按钮与重制版 8 按钮），`HorseGui` 支持各生物专属属性面板与战象（Mumakil）自适应缩放；
> 5. 彻底根治静态 Chunk 内存泄漏、15 米异常收回坐骑 Bug、LivingUpdateEvent 全局性能消耗以及 GUI 预览实体泄漏；
> 6. 研发创造模式专用全系生物自动化检测口令（`/lotrmount test`）。

---

## 目录
1. [方案设计原则与总体架构](#1-方案设计原则与总体架构)
2. [模块一：高可靠数据存储系统深度优化方案 (Storage Engine)](#2-模块一高可靠数据存储系统深度优化方案-storage-engine)
3. [模块二：双版本 UI 像素级自适应兼容方案 (Dual-Version UI)](#3-模块二双版本-ui-像素级自适应兼容方案-dual-version-ui)
4. [模块三：全系坐骑通用适配器架构设计 (IMountAdapter)](#4-模块三全系坐骑通用适配器架构设计-imountadapter)
5. [模块四：构建环境现代化升级方案（纯净 Forge）](#5-模块四构建环境现代化升级方案纯净-forge)
6. [模块五：架构级隐患与性能缺陷根治方案](#6-模块五架构级隐患与性能缺陷根治方案)
7. [模块六：创造模式自动化测试口令开发规范 (/lotrmount test)](#7-模块六创造模式自动化测试口令开发规范-lotrmount-test)
8. [模块七：玩家个人载具存储上限管理方案 (/lotrmount limit)](#模块七玩家个人载具存储上限管理方案-lotrmount-limit)
9. [模块八：区块卸载生命周期与离线/超距收回保障机制](#模块八区块卸载生命周期与离线超距收回保障机制-chunk-lifecycle--mount-safety)
10. [模块九：带箱载具防刷物品与并发安全体系](#模块九带箱载具防刷物品与并发安全体系-anti-item-duplication-architecture)
11. [模块十：召唤冷却时间自定义与创造模式免 CD 机制](#模块十召唤冷却时间自定义与创造模式免-cd-机制-summon-cooldown-management)
12. [模块十一：玩家操作服务端审计日志体系与动态开关 (/lotrmount log)](#模块十一玩家操作服务端审计日志体系与动态开关-operation-audit-logging)
13. [分阶段实施路线图与验收标准](#13-分阶段实施路线图与验收标准)

---

## 1. 方案设计原则与总体架构

### 1.1 总体原则
1. **数据安全绝对优先**：任何意外崩溃、断电、文件损坏均不可导致玩家数据被静默清空；必须具备自愈与多层容灾能力。
2. **像素级无缝原生体验**：UI 必须自适应识别传承版（7 原生按钮）与重制版（8 原生按钮，含披风），在布局、悬浮、按键和交互上与原版魔戒浑然一体。
3. **面向抽象与接口编程**：废除所有面向特定具体类的向下造型，统一面向 `LOTRNPCMount` 接口与 `IMountAdapter` 适配器。
4. **纯净 Forge，不依赖 Mixin**：严格遵循用户要求，不使用 UniMixins，不引入 Mixin 编译插件与运行时 Tweaker。

### 1.2 系统分层架构图

```
┌───────────────────────────────────────────────────────────────┐
│                       客户端与交互层                          │
│   CallableHorseGUIHandler  ──>  HorseButton (extends GuiButton)│
│       (动态双排网格重排算法: 适配 v36 的 7 键 与 Rework 的 8 键)     │
│                                      │                        │
│                                 HorseGui                      │
│             (生物专属多态属性展示 + 猛犸象自适应缩放渲染)            │
├───────────────────────────────────────────────────────────────┤
│                       指令与管理层                            │
│   CommandAddHorse     CommandCallHorse     CommandTestMount   │
│       (/addhorse)        (/callhorse)      (/lotrmount test)  │
├───────────────────────────────────────────────────────────────┤
│                    坐骑通用适配器层 (核心)                    │
│                     MountAdapterRegistry                      │
│                               │                               │
│       ┌───────────────┬───────┴───────┬───────────────┐       │
│       ▼               ▼               ▼               ▼       │
│ HorseMountAdapter WargMountAdapter SpiderMountAdapter Generic │
│  (马/鹿/猪/象)       (全阵营座狼)       (全阵营蜘蛛)    (兜底)│
├───────────────────────────────────────────────────────────────┤
│                 高可靠数据存储引擎 (Storage Engine)           │
│   [内存快照隔离] ──> [.dat.tmp 写入] ──> [.dat.bak 备份轮转]  │
│          │                                     │              │
│   [损坏隔离封存] <── [读失败自动回退] <────────┘              │
│                CallableHorseLevelData (独立 .dat)             │
├───────────────────────────────────────────────────────────────┤
│                       环境与底层支撑                          │
│   CallableHorseEventHandler (安全坐标追踪, 无 Chunk 强引用)  │
│   GTNH ForgeGradle 1.2.11 + Gradle 6.9.1 构建体系 (纯净Forge) │
└───────────────────────────────────────────────────────────────┘
```

---

## 2. 模块一：高可靠数据存储系统深度优化方案 (Storage Engine)

针对前序审计中发现的“读异常静默清空覆写删档”这一重大风险，本方案对存储体系进行全链路工业级重构：

### 2.1 三级容灾备份与自动回退体系 (Failover Architecture)
```
          [写操作流]                                       [读操作流]
    内存 PlayerHorseData                             调用 loadData(playerUUID)
             │                                                │
             ▼                                                ▼
     生成深拷贝 NBT 快照                               尝试读取 player.dat
             │                                                │
             ▼                                                ├─ 成功 ──> 校验版本并反序列化完成
   写入 player.dat.tmp 临时文件                               │
             │                                                └─ 失败 (文件损坏/EOF/ZipException)
             ▼                                                │
   现有 player.dat 备份为 player.dat.bak                      ▼
             │                                       尝试读取 player.dat.bak (自动回退自愈)
             ▼                                                │
   原子替换: .tmp 提交为正式 .dat                             ├─ 成功 ──> 日志告警, 数据成功自愈拯救!
             │                                                │
          写入完成                                            └─ 失败 (双重严重损坏)
                                                              │
                                                              ▼
                                                     将损坏文件隔离至 .dat.corrupt.<ts>
                                                     **严禁覆盖写入空数据! 保护第一现场**
```

### 2.2 核心安全读写代码规范 (`CallableHorseLevelData`)
1. **原子替换与文件备份 (`saveNBTToFileSafe`)**：
   ```java
   public static void saveNBTToFileSafe(File targetFile, NBTTagCompound nbt) throws IOException {
       File parent = targetFile.getParentFile();
       if (parent != null && !parent.exists()) parent.mkdirs();

       File tempFile = new File(targetFile.getAbsolutePath() + ".tmp");
       File backupFile = new File(targetFile.getAbsolutePath() + ".bak");

       // 1. 写入临时文件
       try (FileOutputStream fos = new FileOutputStream(tempFile)) {
           CompressedStreamTools.writeCompressed(nbt, fos);
       }

       // 2. 将现有正本安全复制为备份
       if (targetFile.exists()) {
           try {
               Files.copy(targetFile.toPath(), backupFile.toPath(),
                       StandardCopyOption.REPLACE_EXISTING);
           } catch (Throwable ignored) {}
       }

       // 3. 原子提交新文件 (带跨文件系统重试降级)
       try {
           Files.move(tempFile.toPath(), targetFile.toPath(),
                   StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
       } catch (AtomicMoveNotSupportedException e) {
           Files.move(tempFile.toPath(), targetFile.toPath(),
                   StandardCopyOption.REPLACE_EXISTING);
       }
   }
   ```
2. **容灾回退与损坏隔离读取 (`loadDataSafe`)**：
   ```java
   public static PlayerHorseData loadDataSafe(UUID player) {
       File primary = getCallableHorsePlayerDat(player);
       File backup = new File(primary.getAbsolutePath() + ".bak");

       // 尝试读取正本
       if (primary.exists()) {
           try {
               NBTTagCompound nbt = loadNBTFromFile(primary);
               PlayerHorseData data = new PlayerHorseData(player);
               data.load(nbt);
               return data;
           } catch (Throwable t) {
               FMLLog.severe("[召之马来] 玩家 %s 的正本数据损坏，尝试从备份自愈: %s", player, t.getMessage());
           }
       }

       // 正本损坏或不存在，自动尝试备份
       if (backup.exists()) {
           try {
               NBTTagCompound nbt = loadNBTFromFile(backup);
               PlayerHorseData data = new PlayerHorseData(player);
               data.load(nbt);
               FMLLog.warning("[召之马来] 成功从备份拯救玩家 %s 的坐骑数据！", player);
               // 立即同步回正本
               saveNBTToFileSafe(primary, nbt);
               return data;
           } catch (Throwable t) {
               FMLLog.severe("[召之马来] 玩家 %s 的备份数据亦损坏: %s", player, t.getMessage());
           }
       }

       // 若存在损坏文件，进行隔离封存，严禁静默覆盖！
       if (primary.exists()) {
           File corrupt = new File(primary.getParentFile(), primary.getName() + ".corrupt." + System.currentTimeMillis());
           primary.renameTo(corrupt);
           FMLLog.severe("[召之马来] 警告！已将无法读取的文件隔离保存至: %s", corrupt.getName());
       }

       return new PlayerHorseData(player);
   }
   ```

### 2.3 内存快照隔离（Snapshot Isolation）
在执行保存时，严禁让后台或定期保存线程直接遍历可能会被主线程修改的 `vehicles` 列表。
- 在 `data.save(nbt)` 内部对 `vehicles` 施加严格同步锁（`synchronized(vehicles)`），并在 1 毫秒内将所有槽位深拷贝为 `NBTTagCompound` 内存快照；
- 随后将完全独立的 NBT 快照传递给文件 IO 任务执行磁盘压缩写入，实现读写物理隔离，彻底根绝 `ConcurrentModificationException`。

---

## 3. 模块二：双版本 UI 像素级自适应兼容方案 (Dual-Version UI)

### 3.1 解决按钮包名断层：纯原版 `GuiButton` 零依赖方案
- **实现原理**：
  - `HorseButton` 直接继承 `net.minecraft.client.gui.GuiButton`（原版所有环境均稳定存在）；
  - 自包含绘制：重写 `drawButton`，内部绑定 `horseAndSelectHorse.png`，在 32x32 区域绘制魔戒圆形金属框底座与马头图标；
  - 悬浮自渲染：当鼠标划过按钮时，调用原版自带的悬浮提示渲染，绘制 `"LOTR-召唤载具"` 金色标题；
  - 彻底规避对 `lotr.client.gui.LOTRGuiButtonMenu`（v36）或 `lotr.client.gui.button.LOTRGuiButtonMenu`（Rework）的直接继承。

### 3.2 动态双排网格自适应重排算法 (Dynamic Menu Grid Re-alignment)
针对传承版（7 个按钮）与重制版（8 个按钮）的排版差异，在 `CallableHorseGUIHandler.postInitGui` 中引入魔戒原生的双排网格重排算法：
```java
@SubscribeEvent
public void postInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
    if (!(event.gui instanceof LOTRGuiMenu)) return;

    LOTRGuiMenu menu = (LOTRGuiMenu) event.gui;
    List buttons = event.buttonList;

    // 1. 避免重复添加
    for (Object b : buttons) {
        if (b instanceof HorseButton) return;
    }

    // 2. 构造我们的载具按钮
    HorseButton horseBtn = new HorseButton(FALLBACK_BUTTON_ID, 0, 0, "LOTR-召唤载具");
    buttons.add(horseBtn);

    // 3. 收集所有属于菜单网格的按钮 (原生按钮 + 我们的载具按钮)
    List<GuiButton> gridButtons = new ArrayList<>();
    for (Object b : buttons) {
        if (b instanceof GuiButton) {
            GuiButton gb = (GuiButton) b;
            // 过滤出 32x32 的菜单主按钮
            if (gb.width == 32 && gb.height == 32) {
                gridButtons.add(gb);
            }
        }
    }

    // 4. 应用魔戒原生双排网格公式，重新全量居中排版！
    int midX = menu.width / 2;
    int midY = menu.height / 2;
    int buttonGap = 10;
    int buttonSize = 32;

    int numButtons = gridButtons.size(); // v36: 7+1=8; Rework: 8+1=9
    int numTopRowButtons = (numButtons - 1) / 2 + 1; // v36: 4; Rework: 5
    int numBtmRowButtons = numButtons - numTopRowButtons; // v36: 4; Rework: 4

    int topRowLeft = midX - (numTopRowButtons * buttonSize + (numTopRowButtons - 1) * buttonGap) / 2;
    int btmRowLeft = midX - (numBtmRowButtons * buttonSize + (numBtmRowButtons - 1) * buttonGap) / 2;

    for (int i = 0; i < numButtons; i++) {
        GuiButton btn = gridButtons.get(i);
        if (i < numTopRowButtons) {
            btn.xPosition = topRowLeft + i * (buttonSize + buttonGap);
            btn.yPosition = midY - buttonGap / 2 - buttonSize;
        } else {
            btn.xPosition = btmRowLeft + (i - numTopRowButtons) * (buttonSize + buttonGap);
            btn.yPosition = midY + buttonGap / 2;
        }
    }
}
```
**此算法的威力**：
- 在 **传承版 v36**：7 原生 + 1 载具 = 8 按钮，自动排为 **上 4 下 4**，完美居中！
- 在 **重制版 Reworked**：8 原生 + 1 载具 = 9 按钮，自动排为 **上 5 下 4**，完美居中！
- 彻底消除重叠、错位与版本差异！

### 3.3 `HorseGui` 内部界面深度增强
1. **多态属性面板自适应**：
   在 `HorseGui.drawEntity` 中，废除固定打印马匹属性的做法，改为通过适配器动态展示：
   - **马类/斑马/骆驼/鹿**：显示 `生命上限`、`移动速度`、`跳跃能力`、`马铠品级`、`变种代码`；
   - **座狼系**：显示 `生命上限`、`移动速度`、`座狼种类`（安格玛/魔多等）、`座狼铠甲`；
   - **蜘蛛系**：显示 `生命上限`、`移动速度`、`毒液类型`（迟缓/剧毒/无）、`蜘蛛体型`；
   - **猛犸象系（重制版）**：显示 `巨兽生命`、`践踏威力`、`战象鞍具`。
2. **巨兽（战象 Mumakil）模型渲染自适应缩放**：
   在 `GuiInventory.func_147046_a` 渲染模型前：
   ```java
   float bounds = Math.max(previewEntity.height, previewEntity.width);
   // 基于适配器动态调整基准比例与 Y 偏移
   float scaleMult = adapter.getRenderScaleMultiplier(previewEntity);
   int renderScale = (int) (Math.max(10, Math.min(60, (int) (70.0F / Math.max(bounds, 0.1F)))) * scaleMult);
   int renderY = modelY + (int) adapter.getRenderYOffset(previewEntity);
   ```
   当渲染战象时，自动缩小至 0.35x 并向下平移，象头与长牙绝不遮挡顶部标题与侧边属性。
3. **安全删除 `ReworkVer/` 重复代码目录**：
   通过上述自适应机制，`HorseGui` 单一类即可完美在双版本运行，直接删除 `src/.../client/gui/ReworkVer/`。

---

## 4. 模块三：全系坐骑通用适配器架构设计 (IMountAdapter)

### 4.1 `IMountAdapter` 接口规范
```java
public interface IMountAdapter {
    boolean matches(Entity entity);
    String getCategory();
    boolean isSaddled(Entity entity);
    void setSaddled(Entity entity, boolean saddled);
    ItemStack getArmor(Entity entity);
    void setArmor(Entity entity, ItemStack armor);
    boolean isTamed(Entity entity);
    void setTamed(Entity entity, EntityPlayer player);
    IInventory getInventory(Entity entity);
    double getJumpStrength(Entity entity);
    int getVariant(Entity entity);
    int getMountType(Entity entity);
    float getRenderScaleMultiplier(Entity entity);
    float getRenderYOffset(Entity entity);
    void clearAllEquipment(Entity entity);
    List<String> getDisplayStats(SingleVehicle vehicle);
}
```

### 4.2 具体适配器实现矩阵
1. `HorseMountAdapter`：覆盖战马、矮种马、近哈拉德骆驼、大角鹿、野猪、犀牛、长颈鹿、斑马，以及重制版猛犸象、战羊。
2. `WargMountAdapter`：动态反射覆盖全阵营座狼与投弹座狼，安全管理专属座狼铠与炸药背包。
3. `SpiderMountAdapter`：覆盖幽暗密林、魔多及乌图诺冰蜘蛛，管理蜘蛛鞍具与毒液属性。
4. `GenericMountAdapter`：面向纯 `LOTRNPCMount` 接口的通用安全兜底。

---

## 5. 模块四：构建环境现代化升级方案（纯净 Forge）

- **工具**：升级至 `Gradle 6.9.1`。
- **插件**：采用 `com.github.GTNewHorizons:ForgeGradle:1.2.11`。
- **严格排除**：不使用 UniMixins，不引入 Mixin 依赖与 Tweaker。
- **配置**：引入 `gradle/wrapper/gradle-wrapper.properties`，标准 `gradlew` 与 `gradlew.bat`。

---

## 6. 模块五：架构级隐患与性能缺陷根治方案

1. **彻底消除 Chunk 内存泄漏**：
   从 `CallableHorseEventHandler` 彻底移除 `vehicleAndChunk` 强引用，采用不可变轻量坐标结构 `ChunkLocation(dimension, chunkX, chunkZ)`。
2. **移除 15 米自动收回机制**：
   删除 `mount.getDistanceToEntity(owner) > 15.0F`，仅在主动召回、登出（可配置）、真正区块卸载或死亡时触发收回。
3. **优化全局 `LivingUpdateEvent` 性能**：
   首行执行 `if (!VehicleEntityAndOwner.containsKey(event.entity.getUniqueID())) return;`，毫秒级快速跳过全服普通生物。
4. **GUI 实体预览生命周期销毁**：
   在 `HorseGui.onGuiClosed` 与索引切换时，确保对废弃预览实体显式调用 `setDead()` 并置空引用。

---

## 7. 模块六：创造模式自动化测试口令开发规范 (/lotrmount test)

- **命令名称**：`/lotrmount test`（别名 `/lotrhorse test`）
- **权限**：仅限创造模式玩家（`player.capabilities.isCreativeMode`）或 OP。
- **子命令矩阵**：
  1. `info`：扫描 `LOTREntities` 并列出当前运行环境所有受支持的可骑乘生物与适配器。
  2. `dryrun`：在内存中对全系生物执行无损虚拟化测试（实例化 -> 状态捕获 -> NBT 序列化/反序列化 -> 属性校验），输出完整兼容性体检报表。
  3. `fill`：自动向玩家载具槽注入全品类坐骑样本（战马、座狼、巨蛛、大角鹿、野猪，重制版加填猛犸象与战羊），供即时预览与召唤测试。
  4. `clear`：一键清空玩家测试卡槽。

---


---

## 模块七：玩家个人载具存储上限管理方案 (/lotrmount limit)

### 7.1 功能设计与权限要求
- **默认上限**：每个玩家默认存储上限为 **3** 个（与全局默认配置对齐）。
- **权限规范**：仅限**创造模式玩家**（`player.capabilities.isCreativeMode`）或 OP 权限/服务端控制台可使用修改指令；普通玩家仅可查询自己的上限。

### 7.2 指令语法与交互规范
```text
/lotrmount limit [数量]                  - (创造模式) 设置自己当前载具存储上限 (1-100)
/lotrmount limit <玩家名> <数量>         - (创造模式/OP) 设置目标玩家的载具存储上限 (1-100)
/lotrmount limit <玩家名> reset          - (创造模式/OP) 重置目标玩家上限为全局默认值 (3)
/lotrmount limit [玩家名]                - 查询目标玩家 (或自己) 的存储上限与已用槽位数
```

### 7.3 数据持久化与网络同步机制
1. **数据模型扩展 (`PlayerHorseData`)**：
   - 增加 `customMaxSlots` 字段（默认 `-1` 表示继承全局默认 `3`）；
   - `getMaxSlots()` 算法：若 `customMaxSlots > 0` 则优先返回该值（受 `1-100` 硬边界保护），否则返回 `CallableHorseConfig.getConfiguredSlotCount()`；
   - 在 `writeData` 中存储为 `CustomMaxSlots` NBT 标签，在 `load` 中读取。
2. **安全保存与即时生效**：
   - 执行修改指令后，自动调用 `playerData.markDirty()` 并立即调用安全保存方法；
   - 若目标玩家在线，即时向其客户端推送最新载具数据包，`HorseGui` 实时展示最新扩展后的载具卡槽。

## 模块八：区块卸载生命周期与离线/超距收回保障机制 (Chunk Lifecycle & Mount Safety)

### 8.1 为什么彻底移除“超过 15 米自动强制收回”？
早期版本之所以设置“超过 15 米强制自动收回”，是因为当时模组缺乏完善的事件监听与断链恢复机制，作者担心玩家离开后区块卸载导致生物“走失”或变成未托管的野怪。
然而，15 米自动收回带来了毁灭性的沉浸感破坏：
- 玩家下马打怪，移动超过 15 格坐骑突然凭空消失；
- 玩家进屋开箱子或搜刮遗迹，出门发现坐骑没了；
- 坐骑无法像真实马匹一样停留在原地待命。

### 8.2 区块卸载时生物的完整处理链路（四重防护与零丢失保障）

本重构方案通过构建**四重生命周期防护网**，确保在彻底移除 15 米限制后，即使区块卸载、服务器断电或跨维度旅行，生物也绝不会丢失或无法收回：

#### 第一重防护：`ChunkEvent.Unload` 预卸载精准捕获（实时入库）
1. 当玩家远离坐骑（例如走远、传送、魔戒快速旅行）导致所在区块准备卸载时，Forge 会在区块从内存抹除并存盘前触发 `ChunkEvent.Unload` 事件。
2. 模组的 `CallableHorseEventHandler.onChunkUnload` 监听器会在毫秒级捕获该事件，并定位该区块内的所有已召唤坐骑。
3. 立即调用 `vehicle.captureEntityState(mount)` 将坐骑的当前血量、鞍具、马铠、魔戒背包/箱子物品、自定义名字与属性完整快照至玩家的虚拟卡槽（`SingleVehicle`）。
4. 对实体调用 `mount.setDead()`，阻止该实体以非托管形式存盘进区块文件。
5. 槽位状态原子化更新为 `isUsing = false`，并通过 `CallableHorsePacketHandler` 同步给客户端。
6. **表现**：玩家完全无感。打开界面时，该槽位已安稳返回虚拟马厩，按钮显示为【召之马来】，随时可召出。

#### 第二重防护：`HorseInfo.recallVehicleByIndex` 界面断链自愈兜底
1. 如果玩家在极端边界时机（如正在跨维度传送中）在客户端 GUI 点击【收回】：
2. 即使此时实体对象已因区块卸载从内存脱钩（`ent == null` 或 `ent.isDead`），模组检测到槽位处于 `isUsing == true` 激活态，依然会执行强制自愈重置：
   - 将 `isUsing` 置为 `false`；
   - 清理所有相关 UUID 跟踪映射；
   - 触发数据盘写入与客户端网络同步。
3. **保障**：彻底杜绝“由于实体在未加载区块导致界面按钮卡死在【收回】且无法操作”的死锁 Bug。

#### 第三重防护：`spawnSpecificVehicleByIndex` 跨区块瞬移与跨维度重构
1. **同世界未卸载**：若玩家距离坐骑较远（例如 40 格，区块仍然保持加载），玩家再次点击【召之马来】时，模组检测到同一世界实体仍然存活，直接将其**无损瞬移**（`SingleVehicle.placeNearPlayer`）至玩家身边，并提示 `[召之马来]载具已飞奔至您的身边！`，无需重新生成实体。
2. **跨世界/已卸载**：若坐骑在另一维度或区块已卸载，模组安全清理旧引用后，基于卡槽内完整保存的 NBT 快照在玩家身边生成全新实体，属性、血量、装备与背包 100% 还原。

#### 第四重防护：`EntityJoinWorldEvent` 孤儿实体清理与防复制机制
1. 若服务器遭遇断电或强退等非正常关机，导致个别实体被意外保存在了区块 NBT 中：
2. 当未来该区块被重新加载时，实体尝试进入世界（`EntityJoinWorldEvent`）；
3. 模组检测其 `LOTR_CallableHorse_Owner` 标签，比对玩家数据。若该实体不属于当前活跃卡槽，立即取消生成并 `setDead()` 销毁。
4. **保障**：彻底根除旧世界残留的“幽灵坐骑”，杜绝利用区块加载刷坐骑/刷物品的漏洞。

#### 第五重可选兜底：服务端超距自动收回配置 (`autoRecallDistance`)
- 在配置文件 `lotrcallablehorse.cfg` 中提供了 `autoRecallDistance` 配置项（默认 `0` 为禁用）。
- 若大型高负载服务器需要激进的实体回收策略，服主可将其设置为指定格数（如 `128` 或 `256`），当玩家离开超过该距离且仍在同一世界时，模组将自动平滑收回坐骑至卡槽。

---

## 模块九：带箱载具防刷物品与并发安全体系 (Anti-Item-Duplication Architecture)

魔戒模组中存在多种可装配箱子或自带背包的坐骑（如近哈拉德双箱骆驼、夏尔矮种马、全阵营座狼背包、巨蛛背包以及原版骡子/驴）。在卡槽虚拟化管理下，若缺乏针对性防御，极易引发严重刷物品漏洞。本模组建立了六道针对带箱载具的防刷铁律：

### 9.1 开箱并发收回防御（GUI Container Concurrent Recall）
- **漏洞威胁**：玩家打开坐骑箱子界面不关闭，利用按键/指令/队友协助点击【收回】。卡槽完成 NBT 保存后，玩家在未关闭的窗口中利用快速点击或连点器将箱内物品拖入自身背包，造成物品翻倍。
- **防御机制**：
  1. **强制关闭容器 (`closeOpenContainers`)**：在实体被销毁前，模组全服扫描当前世界正在与该坐骑交互（骑乘、或 `openContainer` 含有该载具背包槽位）的所有玩家，立即调用 `player.closeScreen()` 发送关闭窗口封包，并重置其容器为背包容器。
  2. **内存物理清空 (`clearAllEquipment`)**：在快照生成后，立即调用适配器的 `clearAllEquipment(activeEntity)` 将实体内存中所有背包槽位强制置为 `null`。即使客户端通过外挂在关闭前一微秒强发 `C0EPacketClickWindow`，服务端面对的也是空槽位，绝对无法提取任何物品。

### 9.2 登记源载具防刷防御 (`/addhorse` & `detachSourceEntity`)
- **漏洞威胁**：玩家在打开野生/已驯服带箱载具背包的同时执行 `/addhorse` 登记。
- **防御机制**：在 `SingleVehicle.detachSourceEntity()` 中，销毁原实体前同样严密执行 `closeOpenContainers(source)` 与 `clearAllEquipment(source)`，双重清除交互窗口并抹平背包物品，彻底根绝从原实体身上复制物品的可能。

### 9.3 坐骑意外死亡防刷防御 (`onMountDrops` & `finalizeDeadMount`)
- **漏洞威胁**：带箱坐骑死亡时，原版逻辑会向世界喷洒掉落物（钻石、鞍具等）；若模组在死亡时捕获了背包快照，玩家捡起地面掉落物后再重新召唤坐骑，即可获得双份物品。
- **防御机制**：
  - 在 `LivingDropsEvent`（最低优先级兜底）中，模组无条件调用 `event.drops.clear()` 拦截并抹除所有掉落物实体。
  - 在 `finalizeDeadMount` 中清空实体内存装备与背包。
  - **结果**：坐骑死亡地面**零掉落**，物品唯一安全地存留在玩家卡槽中，重新召唤时完整带出，杜绝死马刷物。

### 9.4 区块异常残留孤儿载具防刷 (`EntityJoinWorldEvent`)
- **漏洞威胁**：服务器异常断电或强退导致带箱载具未触发卸载而保存在区块文件中。未来该区块重载时生成带箱载具，而玩家此前已在基地重新召唤了带有相同物品的载具，导致两套满载物资同时存在。
- **防御机制**：
  - `EntityJoinWorldEvent` 监听所有带有 `LOTR_CallableHorse_Owner` 标签的生物入界。
  - 若该实体不属于当前活跃卡槽，立即调用 `clearMountEquipment(entity)` 抹除背包，并调用 `setDead()` 和 `event.setCanceled(true)` 取消生成，从源头上抹杀任何区块复制实体。

### 9.5 连续连点与并发召唤幂等性保障 (`spawnSpecificVehicleByIndex`)
- **漏洞威胁**：玩家利用高频宏指令或双发数据包试图同时召唤两只同一卡槽的载具。
- **防御机制**：
  - 召唤方法首行执行活体幂等校验：若该卡槽在当前世界已存在活体实体，绝对禁止重复实例化，而是直接调用 `placeNearPlayer` 将已有实体平移至玩家身边。
  - 所有网络封包全部通过 `CallableHorseServerTasks.enqueue` 排队进入服务端主线程串行执行，杜绝多线程竞态。

---

## 模块十：召唤冷却时间自定义与创造模式免 CD 机制 (Summon Cooldown Management)

为了兼顾服务器生存的平衡性以及管理员/测试人员的调试便捷性，模组对坐骑召唤冷却时间（Summon Cooldown）提供了全方位的自定义与权限适配：

### 10.1 动态管理机制与指令体系
1. **指令语法**：
   - `/lotrmount cooldown [秒数]`（别名 `/lotrmount cd [秒数]`）
   - **普通玩家执行**：无参数查询当前全局召唤冷却时间；
   - **管理员/创造模式执行**：带参数直接调整全局冷却时间（范围：`0 - 3600` 秒，`0` 秒代表彻底禁用冷却）。
2. **免重启动态生效与自动存盘**：
   - 指令执行后，调用 `CallableHorseConfig.setSummonCooldown(seconds)`，不仅在当前服务器内存中立即生效，而且**自动同步写入 `config/lotrcallablehorse.cfg` 磁盘文件**，服务器重启后依然保留。

### 10.2 配置文件静态调整
服主亦可在停机时或通过配置文件直接编辑 `config/lotrcallablehorse.cfg`：
```text
general {
    # Server-side summon cooldown in seconds (0-3600).
    I:summonCooldownSeconds=60
}
```

### 10.3 创造模式专属免 CD 特权
- 在 `HorseInfo.spawnSpecificVehicleByIndex` 中，模组自动检测执行召唤的玩家是否处于创造模式（`player.capabilities.isCreativeMode`）；
- 若处于创造模式，**无条件无视任何冷却倒计时**，秒点秒召，极大提升管理员建服、巡查与测试全系坐骑的流畅度。

---

## 模块十一：玩家操作服务端审计日志体系与动态开关 (/lotrmount log)

为了便于大型多人 RPG/战争服务器的服主和管理员排查载具异常、追溯交易纠纷、防范刷取物资以及防止熊孩子利用坐骑进行非法位移，本模组建立了全生命周期的服务端操作审计上报体系。

### 11.1 审计日志标准与格式规范
所有审计日志均统一输出至服务端控制台及日志文件（`logs/fml-server-latest.log` / `logs/latest.log`），以最高辨识度的中文标签作为前缀：
```text
[召之马来-操作审计] <详细上下文>
```
服主可通过文本搜索工具或服务器日志管理组件（如 `grep "召之马来-操作审计"`）瞬间检索出任意时段或特定玩家的操作轨迹。

### 11.2 全生命周期事件覆盖
1. **载具登记（Registration）**：
   - 触发时机：玩家执行 `/addhorse` 录入坐骑。
   - 记录要素：玩家名、玩家 UUID、载具自定义名称、底层实体类型（如 `LOTREntityGondorHorse` / `LOTREntityWarg`）、原实体 UUID、录入后占用的卡槽索引与上限（如 `1/3`）、当前世界维度与高精度坐标（`[维度 0, X:128.5, Y:64.0, Z:-300.2]`）。
2. **载具召唤与拉回（Summon & Teleport）**：
   - 触发时机：通过 GUI 点击召唤或按键召唤。
   - 记录要素：玩家名、UUID、栏位编号、载具名称、生成的实体类型、新实体 UUID、当前世界维度与坐标。若坐骑已在当前世界存活执行的是拉回/传送，明确标识为“召唤/拉回已存在的载具”，杜绝双生实体疑云。
3. **载具召回与重置（Recall & Reset）**：
   - 触发时机：通过 GUI 召回、跨维度/下线自动保护、或区块卸载清理。
   - 记录要素：玩家 UUID、栏位编号、载具名称、实体 UUID、**当前骑乘者名称（无骑乘者或具体的玩家/NPC名称）**、世界维度与坐标。这对于排查“在PVP中强行收回坐骑导致队友坠落”或“利用坐骑卡人”具有决定性证据作用。
4. **载具销毁/释放（Release & Deletion）**：
   - 触发时机：玩家在 GUI 确认销毁或通过网络包释放。
   - 记录要素：玩家名、UUID、栏位编号、载具名称、实体 UUID。
5. **管理员管理指令变更（Admin Changes）**：
   - 个人上限调整：记录操作管理员、目标玩家名、目标 UUID、调整后的槽位数值；
   - 冷却时间调整：记录操作管理员、调整后的全局秒数；
   - 日志开关调整：记录操作管理员、开关状态变更。

### 11.3 动态指令开关与配置文件持久化
1. **指令控制**：
   - `/lotrmount log`：查询当前操作审计上报状态（ON / OFF）；
   - `/lotrmount log <on|off|status>`：动态开启或关闭操作日志上报；
   - 权限要求：仅限管理员（OP）或创造模式玩家可修改，普通玩家仅可查询；
   - 即时生效：内存开关与配置文件双向同步，无需重启服务端。
2. **配置文件项**（`config/lotrcallablehorse.cfg`）：
```text
general {
    # 是否将玩家的所有【召之马来】关键操作（登记载具、召唤载具、召回载具、销毁载具）详细上报至服务端控制台与运行日志 (Server Log)。
    # 默认值: true。开启后便于管理员随时通过检索日志审计玩家的载具行为与防刷记录。
    # 游戏内管理命令: 管理员或创造模式玩家可在游戏内使用 /lotrmount log <on|off> 随时动态开启/关闭上报并自动存盘；使用 /lotrmount log 查询当前状态。
    B:logPlayerOperations=true
}
```

---

## 13. 分阶段实施路线图与验收标准

### 阶段一：构建环境现代化升级
- [x] 部署 Gradle 6.9.1 Wrapper；
- [x] 重构 `build.gradle` 至 GTNH ForgeGradle 1.2.11（纯净 Forge，无 UniMixins）；
- [x] 验证命令行构建与依赖拉取。

### 阶段二：高可靠数据存储引擎落地
- [x] 在 `CallableHorseLevelData` 中实现 `.dat.tmp` 写入与 `.dat.bak` 双重备份；
- [x] 实现读取损坏自动回退自愈与 `.corrupt.<ts>` 隔离机制（杜绝静默清空）；
- [x] 落地同步锁内存快照隔离（防并发竞态）。

### 阶段三：全系坐骑通用适配器架构实现
- [x] 编写 `IMountAdapter` 及其全套适配器（马、狼、蜘蛛、通用）；
- [x] 重构 `SingleVehicle`，彻底移除具体类强转；
- [x] 完善各品类坐骑专属属性提取与装备管理。

### 阶段四：双版本 UI 像素级兼容与性能缺陷根治
- [x] 改造 `HorseButton` 为原生 `GuiButton` 零依赖方案；
- [x] 在 `CallableHorseGUIHandler` 中落地动态双排网格重排算法（完美适配 v36 的 7 键与 Rework 的 8 键）；
- [x] 完善 `HorseGui` 多态属性面板与猛犸象自适应缩放；
- [x] 修复属性面板与换页按钮重叠遮挡 Bug；
- [x] 安全删除 `ReworkVer/` 冗余代码目录；
- [x] 根治 `Chunk` 强引用与 15 米自动收回。

### 阶段五：创造模式测试命令与全链路验证
- [x] 开发并注册 `CommandTestMount`（`/lotrmount test`、`/lotrmount limit`、`/lotrmount cooldown`、`/lotrmount log`）；
- [x] 分别在传承版与重制版环境下运行完整编译构建；
- [x] 执行 `/lotrmount test dryrun` 与 `/lotrmount test fill` 验收全流程；
- [x] 混合服务端（Crucible/Thermos）网络包即时消费与离线 UUID 兼容性保障。
