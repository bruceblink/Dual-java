# Dual — 双人弓箭对战游戏

一款用 Java + [Processing 4](https://processing.org/) 编写的**本地 / 联机**双人（或人机）弓箭对战小游戏。游戏采用 1280×720 的 16:9 确定性竞技场，默认完整等比输出到 1920×1080；两名角色用短弓快速骚扰、用长弓蓄力致命一击，先击杀对手者获胜。支持通过 Relay 服务器进行互联网联机对战。

> `Dual` 会继续使用 Java + Processing 开发，当前 Java 客户端与 Java Relay 都属于本游戏。Godot 项目 `dual-game-client` 是玩法和产品定位均独立的另一款游戏，不是 `Dual` 的正式版、迁移版或替代工程。玩法方向见 [游戏设计](docs/GAME_DESIGN.md)，操作与规则落地见 [玩法开发指导](docs/GAMEPLAY_IMPLEMENTATION_GUIDE.md)，实施顺序见 [设计开发计划](docs/DEVELOPMENT_PLAN.md)，项目边界见 [技术路线说明](docs/TECHNOLOGY_EVALUATION.md)。

## 游戏界面

![游戏界面](picture/pic2.gif)

## 游戏玩法

### 操控（玩家一）

| 按键      | 功能                                              |
| --------- | ------------------------------------------------- |
| `WASD` / ↑ ↓ ← → | 移动角色；方向键同时保留为兼容操作             |
| 鼠标移动   | 敌人位于自动锁定范围外时手动瞄准长弓                  |
| 鼠标左键 / `Z` | 每次按下发射一支**短弓箭**（自动瞄准，即时射出）  |
| 鼠标右键 / `X` | **蓄力**长弓；松开后射出（需蓄力约 0.5 秒）       |
| `N`       | 打开**联机大厅**（开房 / 加入房间）                   |
| `P`       | 暂停 / 继续                                        |
| 演示画面鼠标点击 | 显示 / 隐藏操作说明窗口                         |

### 两种武器

- **短弓**：即时射出，自动指向对手，速度较慢，命中后击飞对手（不致命）。
- **长弓**：敌人进入 520 像素范围后自动锁定；范围外可用鼠标手动瞄准。蓄力后射出高速分段箭矢，命中可直接击杀对手。两箭相撞均会碎裂。

### 游戏流程

1. 启动后进入**演示模式**（双方均为 AI 对战）。
2. 按 `Z` 键开始正式人机比赛（玩家控制白色角色，AI 控制黑色角色）。
3. 比赛采用先胜三回合；每回合结束后按 `X` 开始下一回合，比分和当前回合会保留。
4. 达到三回合胜利后，按 `X` 立即再战，或按 `Z` 返回演示模式。
5. 按 `N` 键打开联机大厅，选择 **Host**（开房等待）或 **Join**（输入对方 IP 加入）。

## 联机对战

联机输入、共享随机种子、回合结果帧和再战请求已经接入客户端与 Relay。回合结果帧包含回合序号、双方比分和比赛完成标记；客户端只接受更大的回合序号，重复或乱序旧帧不会覆盖最新结果。结算画面的 `X` 会发送对应回合的再战请求，双方确认后才进入下一回合或新比赛；等待超过 5 秒会明确提示，可在释放后重试。延迟试玩仍在 R7 后续工作中。

### 快速开始（局域网 / 互联网）

**方式一：直连（同一局域网）**

1. 一方按 `N → H` 开房，记下显示的本机 IP。
2. 另一方按 `N → J`，输入对方 IP（端口默认 `7777`）后按 Enter。
3. 连接建立后自动进入联机对战。

**方式二：通过 Relay 服务器（跨网络）**

1. 在公网机器上启动 Relay 服务：
   ```bash
   java -jar server/build/libs/dual-server-1.0-all.jar [port]
   # 默认端口 7777
   ```
2. 双方均按 `N → J`，输入 **Relay 服务器的公网 IP / 域名** 与端口，依次连接即可配对。

### 构建 Relay 服务器

```cmd
./gradlew :server:fatJar
# 输出：server/build/libs/dual-server-1.0-all.jar
```

> Relay 服务器无图形界面，不依赖 Processing，可部署到任意 JDK 21+ 环境。

## 环境要求

| 依赖    | 版本                              |
| ------- | --------------------------------- |
| JDK     | **21+**（编译目标为 Java 21）     |
| Gradle  | 无需手动安装，使用内置 Wrapper    |

> **关于 JDK 路径**：项目不绑定本机 JDK 绝对路径，使用当前运行 Gradle 的 JDK 编译 Java 21 目标。
> 请确保 `JAVA_HOME` 或命令行上的 `java` 来自 JDK 21 或更高版本。

默认窗口和游戏区域分辨率为 1920×1080，并支持自由缩放。竞技场保持 16:9 比例；只有窗口比例不同时才加入留白，留白不接收瞄准或攻击输入。

## 运行方式

```cmd
# 直接运行游戏（开发调试）
./gradlew run

# 构建 Relay 服务器 fat JAR
./gradlew :server:fatJar

# 启动 Relay 服务器
java -jar server/build/libs/dual-server-1.0-all.jar [port]

# 打包为 Windows 安装包（.exe，需要 WiX 3.x）
./gradlew packageApp
```

安装包输出到 `build/dist/` 目录。

## 构建说明

```cmd
# 编译并运行测试
./gradlew build

# 仅运行测试
./gradlew test

# 清理构建产物
./gradlew clean
```

## 项目结构

```txt
src/main/java/com/likanug/dual/
├── App.java                    # Processing 入口，键盘事件 / 联机大厅 UI
├── GameConstants.java          # 所有游戏数值常量
├── actor/                      # 角色与箭矢实体
│   ├── ActorGroup.java         # 一方阵营（玩家 + 箭矢列表）
│   ├── actor/
│   │   └── player/             # 玩家角色（PlayerActor, NullPlayerActor）
│   └── arrow/                  # 箭矢（短弓箭、长弓箭各组件）
├── game/
│   ├── GameSystem.java         # 游戏主循环、屏幕震动、粒子生成
│   └── GameBackground.java     # 背景线条渲染
├── inputDevice/                # 输入设备抽象（KeyInput, InputDevice）
├── network/                    # 联机网络层
│   ├── NetworkMessage.java     # 协议编解码
│   ├── GameNetwork.java        # P2P 网络基类（发送 / 接收输入）
│   ├── NetworkServer.java      # Host 模式（监听连接）
│   └── NetworkClient.java      # Join 模式（连接到 Host）
├── particle/                   # 粒子系统（对象池复用）
├── playerEngine/               # 玩家行为引擎（人类 / AI / 网络）
│   ├── NetworkPlayerEngine.java# 联机远端玩家驱动
│   └── *PlayerPlan.java        # AI 决策计划（移动、点射、蓄力击杀）
├── pool/                       # 泛型对象池（ObjectPool, Poolable）
└── state/                      # 状态机
    ├── GameSystemState.java    # 游戏系统状态基类
    ├── StartGameState.java     # 开始/倒计时状态
    ├── PlayGameState.java      # 对战状态（碰撞检测、胜负判定）
    ├── GameResultState.java    # 结果展示状态
    └── *PlayerActorState.java  # 玩家状态（移动、拉弓、受伤）

server/src/main/java/com/likanug/dual/server/
├── ServerApp.java              # Relay 服务器入口
├── RelayServer.java            # TCP accept 循环，多房间管理
├── RelayRoom.java              # 单个房间：握手 + 双向消息转发
└── NetworkProtocol.java        # 服务端协议常量
```

## 技术栈

- **Processing 4.5.0**：渲染与窗口管理
- **JUnit 5**：单元测试
- **jpackage + WiX 3**：Windows 安装包打包
