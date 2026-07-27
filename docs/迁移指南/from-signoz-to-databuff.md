# 从 SigNoz 迁移到 DataBuff

保留 **OpenTelemetry SDK / Collector** 上报方式，将 OTLP 目标从 SigNoz 改为 DataBuff Ingest（gRPC **4317** / HTTP **4318**）。不换探针、不改业务代码。能力差异见 [DataBuff vs SigNoz](../业界对比/vs-signoz.md)。

## 迁移思路

```
迁移前：  App / Collector  ──OTLP──▶  SigNoz OTel Collector（常见 :4317 / :4318）
迁移后：  App / Collector  ──OTLP──▶  DataBuff Ingest
```

**核心：把 `OTEL_EXPORTER_OTLP_ENDPOINT`（或 Collector exporter）从 SigNoz 地址改为 DataBuff Ingest。**

推荐 **金丝雀 → 验收 → 分批扩**；服务少可一次性改全量地址并重启。

**前置条件**

- [DataBuff 已部署](../快速入门/docker安装部署.md)
- Ingest OTLP 可达（Docker **4317** / **4318**，K8s 常见 **31417** / **31418**）
- 记录当前 SigNoz Collector 地址（如 `signoz-otel-collector:4317`），便于回滚

协议细节见 [OpenTelemetry OTLP 接入](../opentelemetry-otlp-ingestion.md)。

## 操作步骤

### 1. 确认 Ingest 地址

| 部署 | OTLP gRPC | OTLP HTTP |
|------|-----------|-----------|
| Docker | `<ingest-host>:4317` | `http://<ingest-host>:4318` |
| Kubernetes | `<node-ip>:31417` 等 | `http://<node-ip>:31418` 等 |

### 2. 修改上报地址

**应用直连 Collector / SigNoz**

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT="http://<databuff-ingest-host>:4318"
export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
export OTEL_SERVICE_NAME="my-service"
```

将原指向 `signoz-otel-collector:4317`（或 Helm 中对应 Service）的地址改为 DataBuff Ingest。

**自建 OTel Collector**（原 exporter 写 SigNoz / ClickHouse 时，改为 DataBuff）：

```yaml
exporters:
  otlp:
    endpoint: "<databuff-ingest-host>:4317"
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      exporters: [otlp]
    metrics:
      receivers: [otlp]
      exporters: [otlp]
    logs:
      receivers: [otlp]
      exporters: [otlp]
```

K8s / 容器：更新 Deployment 环境变量或 ConfigMap 中的 OTLP 地址，滚动重启 Pod。

### 3. 分批切流

1. 先改 1–2 个非核心服务  
2. 在 DataBuff Web 完成验收（见下）  
3. 按业务域逐步扩批  
4. 稳定后可下线 SigNoz Query / ClickHouse 写入  

## 验收与回滚

| 检查项 | 预期 |
|--------|------|
| 服务 | 应用性能页出现对应服务名 |
| Trace | 新请求可查完整链路 |
| 指标 / 日志 | SDK 已上报则有对应数据 |
| 告警 | 在 DataBuff **重新配置**（SigNoz 告警规则不自动迁移） |

**回滚**：OTLP 地址改回 SigNoz Collector 并重启。变更单保留改前 / 改后两个值。

**不自动迁移**：SigNoz 历史数据、告警与自定义大盘；需在 DataBuff 侧按需重建。

## 延伸阅读

- [DataBuff vs SigNoz](../业界对比/vs-signoz.md)
- [OpenTelemetry OTLP 接入](../opentelemetry-otlp-ingestion.md)
- [Docker 安装部署](../快速入门/docker安装部署.md)
