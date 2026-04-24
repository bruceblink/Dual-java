# 开发文档（联机 NIO / Relay Reactor）

## 目标

将联机链路从阻塞式 Socket 迁移到 NIO，并完成服务端 Relay 的 Reactor 化，在保持协议字节与联机行为不变的前提下提高可维护性与并发能力。

## 开发约束（执行流程）

1. 在 `dev` 分支开发。
2. 每个功能点完成后先跑测试。
3. 按功能点拆分提交。
4. 再进入下一阶段开发。

## 当前进度

### 已完成

- 客户端网络层 NIO 化（`GameNetwork / NetworkClient / NetworkServer`）。
- 服务端 Relay 主循环迁移为 Selector Reactor（`OP_ACCEPT / OP_READ / OP_WRITE`）。
- `RelayRoom` 状态机迁移：
  - `WAITING_SECOND_PLAYER -> WAITING_ACKS -> RELAYING -> CLOSED`
- 握手时序修复（第二个连接 attach 后再启动握手）。
- 新增并通过测试：
  - `GameNetworkTest`
  - `RelayRoomTest`
  - `RelayServerIntegrationTest`（握手、输入转发、断连转发）
- 断连阶段稳定性修复：
  - 发送队列注册写事件后主动 `selector.wakeup()`
  - 事件循环中读取/写入前补充 `key.isValid()` 防止关闭时 `CancelledKeyException`

### 进行中

- 收尾优化与回归验证（聚焦 relay/disconnect 边界场景稳定性）。

## 下一阶段计划

1. 补充 relay/disconnect 边界集成用例（异常关闭、EOF 时序、半包场景）。
2. 完成服务端联调回归（双客户端 Host/Join + Relay 模式）。
3. 保持协议一致性审查：
   - `TYPE_INPUT + flags`
   - `TYPE_START + seed`
   - `TYPE_START_ACK`
   - `TYPE_DISCONNECT`
4. 功能点通过测试后按粒度提交。

## 验证命令

```bash
./gradlew :server:test --no-daemon
./gradlew test --no-daemon
```
