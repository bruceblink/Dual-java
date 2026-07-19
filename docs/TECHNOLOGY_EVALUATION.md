# Dual 整体技术选型评估

## 结论

推荐采用 **Godot 4 + GDScript 客户端** 与 **Go 1.26 权威服务端**。它们放在两个独立仓库中，通过版本化的 WSS 二进制协议协作。当前 `Dual` 仓库保留为玩法演示，不迁移、不删除，也不作为正式版本的代码基线。

Kotlin 是可行的第二选择，但 Kotlin 只是语言，不是 Android 游戏发布方案。若采用 Kotlin，仍需引入 libGDX 处理渲染、输入、资源和多平台发布；这条路线适合明确偏好代码优先、JVM 工具链的团队，不是当前项目的最短风险路径。

## 评估范围

目标是以当前桌面双人弓箭对战 Demo 验证过的玩法为参考，建立可发布 Android 版本的正式在线游戏。评估以以下标准排序：

1. Windows、macOS、Linux 和 Android 的交付能力。
2. 2D 实时玩法、触摸输入、粒子和界面开发效率。
3. 互联网联机的稳定性、可恢复性和防作弊边界。
4. 独立构建、测试、部署和后续维护成本。
5. 从现有 Processing 客户端迁移的风险。

不把“尽量复用 Java 代码”作为目标。现有渲染和输入层无法直接用于 Android，而现有 Relay 也不具备权威判定能力。Demo 的职责是提供可玩的规则参考和回归比较，不是约束新工程的实现。

## 现状与问题

当前 Demo 仓库把客户端和 Java 服务端放在同一个 Gradle 多模块构建中。客户端协议是四个裸 TCP 消息：输入、开始、开始确认、断线。服务端只把两个连接按到达顺序配成一组，再逐字节转发输入。

这带来四个实质问题：

- 客户端、服务端必须一起构建和发版，服务端不能独立部署、回滚或扩缩容。
- “第一个等第二个”的隐式配对没有房间码，多个玩家同时进入会发生错误配对。
- Relay 不判定游戏状态。当前客户端含有帧相关行为和本地随机数，仅共享一个随机种子不足以保证两端状态收敛。
- 原始 TCP 在移动网络上需要自行处理 TLS、代理、连通性和诊断；使用 WSS 走 443 端口会更适合桌面与 Android 客户端。

因此，正式服务端不应只是从 Java 翻译到另一种语言，而应以全新的协议升级为服务端权威的比赛服务。Demo 的 Java Relay 保留为演示的一部分。

## 客户端候选方案

| 方案 | 平台与玩法匹配 | 迁移成本 | 长期风险 | 结论 |
| --- | --- | --- | --- | --- |
| Godot 4 + GDScript | 强。原生 2D 工作流、输入、场景、粒子和 Android 导出完整。 | 中。需要重写客户端。 | 低到中。需要学习 Godot 的节点与资源模型。 | 推荐。 |
| Kotlin + libGDX | 强。Kotlin 可配合 libGDX 交付桌面和 Android。 | 中到高。需要重写 Processing 渲染和平台层。 | 中。代码优先但需要自行组织 UI、资源和编辑流程。 | 备选。 |
| Kotlin Multiplatform + Compose | 中。适合大厅、设置和工具界面。 | 高。Compose 不是这类实时战斗渲染的主引擎。 | 中到高。还需要补充游戏引擎。 | 不作为主客户端。 |
| Unity + C# | 强。平台覆盖和工具都成熟。 | 高。对当前小型 2D 项目过重。 | 中。引擎与商业策略依赖更强。 | 不优先。 |
| Rust + Bevy | 中。性能优良，但 Android 与工具链的迁移验证成本高。 | 很高。玩法与工具全部重写。 | 高。生态变化与团队学习成本更高。 | 不优先。 |
| TypeScript + Phaser + Capacitor | 中。Web 分发方便。 | 高。原生游戏手感、离线能力和发布链更绕。 | 中。网络只能走 Web 协议。 | 不优先。 |

Godot 的 Android 导出和 WebSocket 客户端是引擎的标准能力；libGDX 也明确支持 Kotlin 使用和多平台部署。参考：[Godot Android 导出](https://docs.godotengine.org/en/stable/tutorials/export/exporting_for_android.html)、[Godot WebSocketPeer](https://docs.godotengine.org/en/stable/classes/class_websocketpeer.html)、[libGDX Kotlin 支持](https://libgdx.com/wiki/jvm-langs/using-libgdx-with-kotlin)、[libGDX 部署](https://libgdx.com/wiki/deployment/deploying-your-application)。

### 为什么不直接重写成 Kotlin

把 `.java` 改为 `.kt` 只会改善语言表达，并不会解决以下工作：Android 生命周期、触摸输入、画布缩放、音频、资源加载、桌面与 Android 打包、网络连接和视觉编辑。若选择 Kotlin，实际的组合应是 Kotlin + libGDX，而不是 Kotlin + Processing 或 Kotlin + Compose 直接替换游戏主循环。

libGDX 是合理备选：它适合代码优先的 2D 游戏，并可保留 Gradle/JVM 的开发习惯。但当前目标同时强调移动端发布、操作打磨和尝试最适合的组合；Godot 的场景编辑、2D 工具和 Android 路径能更快验证可玩性，因此优先级更高。

## 服务端候选方案

| 方案 | 适合度 | 结论 |
| --- | --- | --- |
| Go 1.26 + WSS + 每房间模拟循环 | 强。并发模型简单，单一静态二进制和 Docker 交付直接，适合双人房间服务。 | 推荐。 |
| Rust + Tokio | 强。性能与内存安全优秀。 | 可行但不优先；对小型服务的实现与维护成本高于 Go。 |
| Node.js + TypeScript | 中。WebSocket 与后台管理开发快。 | 可行但不优先；实时模拟需要更严格避免事件循环被业务阻塞。 |
| Elixir/Phoenix Channels | 中。实时连接与容错强。 | 可行但团队学习和部署成本与项目规模不匹配。 |
| Java/Kotlin 服务端 | 强。现有代码迁移最小。 | 排除，用户已明确后续不再使用 Java 服务端。 |

Go 的标准库覆盖网络、HTTP、测试和交叉编译等基础能力，项目本机已安装 Go 1.26.2。服务端使用 Go 的目的不是追求极限并发，而是用清晰的房间生命周期和低部署成本替代 Java Relay。参考：[Go 文档](https://go.dev/doc/)、[`net` 包](https://pkg.go.dev/net)。

## 推荐的目标架构

```text
Godot desktop client  -- WSS -->  Go edge / connection layer
Godot Android client  -- WSS -->  Go matchmaking and room service
                                      |
                                      v
                              one 60 Hz authoritative room loop
                                      |
                                      v
                            state snapshots and match events
```

### 术语

- **WSS**：运行在 TLS 加密连接上的 WebSocket。客户端通过 `wss://` 使用它；首版统一走 HTTPS 常用的 443 端口。
- **服务端权威**：只有服务端可以确认移动、命中、伤害、胜负和房间成员关系；客户端提交操作请求，不以本地计算结果覆盖服务端结果。
- **本地预测**：客户端先显示自己刚输入的移动，减少网络延迟带来的操作滞后；收到服务端状态后必须校正到服务端位置。
- **状态快照**：服务端在某个模拟帧发出的完整、可重建的比赛状态，用来让客户端渲染和纠正偏差。
- **固定 60 Hz 模拟**：每秒推进 60 次相同时间步长的规则更新；它与客户端实际渲染帧率无关。

### 客户端职责

- 收集键盘或触摸输入，显示本地预测和服务端状态。
- 渲染角色、箭矢、特效、菜单和断线状态。
- 不决定命中、伤害、胜负或房间成员关系。

### 服务端职责

- 创建显式房间码或快速匹配队列，保证两个玩家进入同一房间。
- 以固定 60 Hz 推进移动、箭矢、碰撞、伤害和胜负。
- 校验输入序号、输入频率、动作合法性和数值范围。
- 以较低频率发送状态快照，并在断线、超时、再战和结算时发送明确事件。

### 协议边界

使用 WSS 的二进制 WebSocket 帧，不共享 Java、Go 或 GDScript 源码。每条帧都有协议版本、消息类型、序号和严格定义的字段。

第一版至少包含：`HELLO`、`CREATE_ROOM`、`JOIN_ROOM`、`INPUT`、`MATCH_START`、`SNAPSHOT`、`MATCH_END`、`REPLAY_REQUEST`、`DISCONNECT` 和 `ERROR`。`INPUT` 要包含递增序号、客户端模拟帧、移动向量、瞄准值和动作位；触摸操作因此不再被限制为桌面方向键。

服务端仓库是协议规范的权威来源，规范以不可变 Git tag 发布。客户端固定声明支持的协议版本，并用黄金报文测试兼容性。当前只有两端，不建立第三个“共享代码仓库”，以免增加维护成本。正式协议从 `v1` 开始，不兼容 Demo 的裸 TCP 协议。

## 仓库与交付边界

| 仓库 | 内容 | 发布物 |
| --- | --- | --- |
| `Dual` | 冻结的 Java/Processing 玩法 Demo 与 Java Relay。 | Demo 桌面包。 |
| `dual-game-client` | Godot 工程、资源、客户端规则、桌面和 Android 导出配置。 | 桌面安装包、APK/AAB。 |
| `dual-game-server` | Go 模块、协议规范、房间模拟、部署与运维配置。 | Linux 静态二进制、Docker 镜像。 |

客户端与服务端独立版本、独立 CI、独立发布。服务端不依赖 Godot、JDK 或 Gradle；客户端构建不拉取服务器二进制。服务器的首个持久化需求只限于可选的房间/指标，不在首版引入账户、商城、排行榜或数据库。

## 先验证，再迁移

正式版本的选择需要三个受控原型确认，而不是直接重写 Demo：

1. **Godot 原型**：以受控的小范围证明同一玩法能在桌面和 Android 真机上稳定运行，且触摸操作可玩。
2. **Go 原型**：以受控的小范围证明两个客户端经 WSS 进入指定房间，服务器可以在延迟、断线和重复输入下给出正确状态。
3. **联调原型**：完成一局移动、射箭、击杀和再战。只有这一项通过后，才迁移完整表现层和发布流程。

若 Godot 原型未达到 Android 运行或触摸可玩性标准，立即停止该方向，改做 Kotlin + libGDX 原型；不在未经验证的假设上投入完整重写。无论结果如何，当前 Demo 保持独立可运行。
