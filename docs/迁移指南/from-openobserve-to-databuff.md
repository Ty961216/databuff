# 从 OpenObserve 迁移到 DataBuff

保留 **OpenTelemetry SDK / Collector** 上报方式，将 OTLP 目标从 OpenObserve 改为 DataBuff Ingest（gRPC **4317** / HTTP **4318**）。不换探针、不改业务代码。能力差异见 [DataBuff vs OpenObserve](../业界对比/vs-openobserve.md)。

## 迁移思路

```
迁移前：  App / Collector  ──OTLP HTTP──▶  OpenObserve（常见 :5080/api/default）
迁移后：  App / Collector  ──OTLP──▶  DataBuff Ingest
```

**核心：把 `OTEL_EXPORTER_OTLP_ENDPOINT` 从 OpenObserve 的 `/api/<org>` 路径改为 DataBuff Ingest（`:4318` HTTP 或 `:4317` gRPC）。**

推荐 **金丝雀 → 验收 → 分批扩**；服务少可一次性改全量地址并重启。

**前置条件**

- [DataBuff 已部署](../快速入门/docker安装部署.md)
- Ingest OTLP 可达（Docker **4317** / **4318**）
- 记录当前 OpenObserve OTLP 地址（如 `http://openobserve:5080/api/default`），便于回滚

协议细节见 [OpenTelemetry OTLP 接入](../opentelemetry-otlp-ingestion.md)。

## 操作步骤

### 1. 确认 Ingest 地址

| 部署 | OTLP gRPC | OTLP HTTP |
|------|-----------|-----------|
| Docker | `<ingest-host>:4317` | `http://<ingest-host>:4318` |
| Kubernetes | `<node-ip>:31417` 等 | `http://<node-ip>:31418` 等 |

### 2. 修改上报地址

**应用 SDK**

```bash
# 改前（OpenObserve 示例）
# export OTEL_EXPORTER_OTLP_ENDPOINT="http://openobserve:5080/api/default"

export OTEL_EXPORTER_OTLP_ENDPOINT="http://<databuff-ingest-host>:4318"
export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
export OTEL_SERVICE_NAME="my-service"
```

若 OpenObserve 配置了 Basic Auth，去掉对应 exporter 认证头；DataBuff Ingest 默认无需该认证。

**OpenTelemetry Collector**：

```yaml
exporters:
  otlphttp:
    endpoint: "http://<databuff-ingest-host>:4318"

service:
  pipelines:
    traces:
      receivers: [otlp]
      exporters: [otlphttp]
    metrics:
      receivers: [otlp]
      exporters: [otlphttp]
    logs:
      receivers: [otlp]
      exporters: [otlphttp]
```

K8s / 容器：更新环境变量或 Collector ConfigMap，滚动重启。

### 3. 分批切流

1. 先改 1–2 个非核心服务  
2. 在 DataBuff Web 完成验收（见下）  
3. 按业务域逐步扩批  
4. 稳定后 APM 流量可不再写入 OpenObserve（日志 / 大盘若仍依赖 OpenObserve 可继续并行使用）  

## 验收与回滚

| 检查项 | 预期 |
|--------|------|
| 服务 | 应用性能页出现对应服务名 |
| Trace | 新请求可查完整链路 |
| 指标 | SDK 已上报则有黄金指标曲线 |
| 告警 | 在 DataBuff **重新配置**（OpenObserve 告警不自动迁移） |

**回滚**：`OTEL_EXPORTER_OTLP_ENDPOINT` 改回 OpenObserve 地址并重启。变更单保留改前 / 改后两个值。

**不自动迁移**：OpenObserve 中的历史 Trace / 指标、日志管道与大盘；日志若仍走 OpenObserve 可保持独立。

## 延伸阅读

- [DataBuff vs OpenObserve](../业界对比/vs-openobserve.md)
- [OpenTelemetry OTLP 接入](../opentelemetry-otlp-ingestion.md)
- [Docker 安装部署](../快速入门/docker安装部署.md)
