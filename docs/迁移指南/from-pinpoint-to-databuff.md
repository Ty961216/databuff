# 从 Pinpoint 迁移到 DataBuff

Pinpoint 使用 **专有 Java Agent**，无法像 SkyWalking 那样只改上报地址。迁移需 **卸下 Pinpoint Agent、改用 OpenTelemetry Java Agent（或 SkyWalking Agent）**，将遥测发往 DataBuff Ingest。能力差异见 [DataBuff vs Pinpoint](../业界对比/vs-pinpoint.md)。

## 迁移思路

```
迁移前：  App + Pinpoint Agent  ──▶  Pinpoint Collector
迁移后：  App + OTel Java Agent  ──OTLP──▶  DataBuff Ingest（:4317 / :4318）
         或 App + SkyWalking Agent ──gRPC:11800──▶  DataBuff Ingest
```

**核心：换探针 + 指向 DataBuff。** 推荐 OTel Java Agent（多语言栈统一）；纯 Java 且希望最小改动时可选 SkyWalking Agent 接 DataBuff 11800。

推荐 **金丝雀 → 验收 → 分批扩**。

**前置条件**

- [DataBuff 已部署](../快速入门/docker安装部署.md)
- Ingest OTLP（**4317** / **4318**）或 SkyWalking gRPC（**11800**）可达
- 记录原 `pinpoint.applicationName`、Agent 启动参数，便于对照 `OTEL_SERVICE_NAME` 或 `agent.service_name`
- 在测试环境验证 Pinpoint Agent 已完全移除，避免双探针冲突

接入细节见 [OpenTelemetry OTLP 接入](../opentelemetry-otlp-ingestion.md)、[SkyWalking 接入](../使用手册/SkyWalking接入.md)。

## 操作步骤

### 1. 确认 Ingest 地址

| 方案 | Agent | 指向 |
|------|-------|------|
| OTel（推荐） | OpenTelemetry Java Agent | `<ingest-host>:4318`（HTTP）或 `:4317`（gRPC） |
| 备选 | SkyWalking Java Agent | `<ingest-host>:11800` |

### 2. 卸下 Pinpoint Agent

从 JVM 启动参数中移除 Pinpoint，例如：

```bash
# 删除类似配置
-javaagent:/path/to/pinpoint-bootstrap.jar
-Dpinpoint.applicationName=my-service
-Dpinpoint.agentId=my-instance
```

同时删除 `pinpoint.config` 或对应环境变量挂载。

### 3. 接入 DataBuff（OTel 示例）

```bash
java -javaagent:/path/to/opentelemetry-javaagent.jar \
  -Dotel.service.name=my-service \
  -Dotel.exporter.otlp.endpoint=http://<databuff-ingest-host>:4318 \
  -Dotel.exporter.otlp.protocol=http/protobuf \
  -jar my-app.jar
```

或环境变量：

```bash
export OTEL_SERVICE_NAME="my-service"
export OTEL_EXPORTER_OTLP_ENDPOINT="http://<databuff-ingest-host>:4318"
export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
```

**SkyWalking 备选**（仅 Java、希望协议与端口不变时）：

```properties
agent.backend_service=<databuff-ingest-host>:11800
agent.service_name=my-service
```

K8s：更新 Deployment 的 `JAVA_TOOL_OPTIONS` 或 Init 容器中的 Agent 镜像，滚动重启。

### 4. 分批切流

1. 先改 1–2 个非核心 Java 服务  
2. 在 DataBuff Web 完成验收（见下）  
3. 按业务域逐步扩批  
4. 稳定后可下线 Pinpoint Collector / Web  

## 验收与回滚

| 检查项 | 预期 |
|--------|------|
| 服务 | 应用性能页出现与 `OTEL_SERVICE_NAME` 一致的服务 |
| Trace | 新请求可查链路（粒度为 OTel 自动埋点，非 Pinpoint 方法级 Call Tree） |
| 实例 | 多副本时实例列表可见 |
| 告警 | 在 DataBuff **重新配置**（Pinpoint 告警不自动迁移） |

**回滚**：恢复 Pinpoint Agent 启动参数与 Collector 地址并重启。变更单保留改前 / 改后配置。

**不自动迁移**：Pinpoint 历史 Trace、Inspector 数据与告警规则；Pinpoint 方法级调用栈深度需接受 OTel 默认埋点粒度差异。

## 延伸阅读

- [DataBuff vs Pinpoint](../业界对比/vs-pinpoint.md)
- [OpenTelemetry OTLP 接入](../opentelemetry-otlp-ingestion.md)
- [SkyWalking 接入](../使用手册/SkyWalking接入.md)
- [Docker 安装部署](../快速入门/docker安装部署.md)
