# Dual — 双人弓箭对战游戏

一款用 Java + [Processing 4](https://processing.org/) 编写的本地双人（或人机）弓箭对战小游戏。两名角色在 640×640 的竞技场内互相射箭，用短弓快速骚扰、用长弓蓄力致命一击，先击杀对手者获胜。

## 游戏界面

![初始界面](picture/pic1)

<!-- 如有 GIF 截图可取消下一行注释 -->
<!-- ![游戏界面](picture/pic2.gif) -->

## 游戏玩法

### 操控（玩家一，键盘）

| 按键 | 功能 |
| ------ | ------ |
| ↑ ↓ ← → | 移动角色 / 蓄力长弓时调整瞄准方向 |
| `Z` | 发射**短弓箭**（自动瞄准，即时射出） |
| `X` | **蓄力**长弓；松开后射出**长弓箭**（需蓄力约 0.5 秒） |
| `P` | 暂停 / 继续 |
| 鼠标点击 | 显示 / 隐藏操作说明窗口 |

### 两种武器

- **短弓**：即时射出，自动指向对手，速度较慢，命中后击飞对手（不致命）。
- **长弓**：蓄力后射出高速分段箭矢，命中可直接击杀对手。两箭相撞均会碎裂。

### 游戏流程

1. 启动后进入**演示模式**（双方均为 AI 对战）。
2. 按 `Z` 键开始正式对局（玩家控制白色角色，AI 控制黑色角色）。
3. 任意一方被击杀后显示胜负结果，片刻后自动返回演示模式。

## 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | **21**（推荐 Oracle JDK 21） |
| Gradle | 无需手动安装，使用内置 Wrapper |

> **关于 JDK 路径**：`gradle.properties` 中的 `org.gradle.java.installations.paths` 指向本机 JDK 21 安装路径。
> 换台机器时，请将该路径改为实际路径，或删除该行并将 `JAVA_HOME` 环境变量指向 JDK 21。

## 运行方式

```cmd
# 直接运行（开发调试）
./gradlew run

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
├── App.java                    # Processing 入口，键盘事件处理
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
├── particle/                   # 粒子系统（对象池复用）
├── playerEngine/               # 玩家行为引擎（人类 / AI）
│   └── *PlayerPlan.java        # AI 决策计划（移动、点射、蓄力击杀）
├── pool/                       # 泛型对象池（ObjectPool, Poolable）
└── state/                      # 状态机
    ├── GameSystemState.java    # 游戏系统状态基类
    ├── StartGameState.java     # 开始/倒计时状态
    ├── PlayGameState.java      # 对战状态（碰撞检测、胜负判定）
    ├── GameResultState.java    # 结果展示状态
    └── *PlayerActorState.java  # 玩家状态（移动、拉弓、受伤）
```

## 技术栈

- **Processing 4.5.0**：渲染与窗口管理
- **Guava 32**：工具库
- **JUnit 5**：单元测试
- **jpackage + WiX 3**：Windows 安装包打包
