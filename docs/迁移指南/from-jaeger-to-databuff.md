# 从 Jaeger 迁移到 DataBuff

保留 **OpenTelemetry SDK / Collector** 上报方式，将 OTLP 目标从 Jaeger 改为 DataBuff Ingest（gRPC **4317** / HTTP **4318**）。不换探针、不改业务代码。能力差异见 [DataBuff vs Jaeger](../业界对比/vs-jaeger.md)。

## 迁移思路

```
迁移前：  App / Collector  ──OTLP──▶  Jaeger（:4317 / :4318 或 Collector）
迁移后：  App / Collector  ──OTLP──▶  DataBuff Ingest
```

**核心：把 `OTEL_EXPORTER_OTLP_ENDPOINT`（或 Collector `exporters.otlp.endpoint`）改为 DataBuff Ingest 地址。**

推荐 **金丝雀 → 验收 → 分批扩**；服务少可一次性改全量地址并重启。

**前置条件**

- [DataBuff 已部署](../快速入门/docker安装部署.md)
- Ingest OTLP 可达（Docker **4317** / **4318**，K8s 常见 **31417** / **31418**）
- 记录当前 Jaeger OTLP / Collector 地址，便于回滚
- 应用已走 **OTLP**（OpenTelemetry SDK 或经 OTel Collector 转发）

> 仍使用 Jaeger Agent（UDP Thrift）等遗留上报时，需先经 OTel Collector `jaeger` receiver 转为 OTLP，再指向 DataBuff；或升级为 OTLP SDK。

协议细节见 [OpenTelemetry OTLP 接入](../opentelemetry-otlp-ingestion.md)。

## 操作步骤

### 1. 确认 Ingest 地址

| 部署 | OTLP gRPC | OTLP HTTP |
|------|-----------|-----------|
| Docker | `<ingest-host>:4317` | `http://<ingest-host>:4318` |
| Kubernetes | `<node-ip>:31417` 等 | `http://<node-ip>:31418` 等 |

### 2. 修改上报地址

**应用 SDK（环境变量）**

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT="http://<databuff-ingest-host>:4318"
export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
export OTEL_SERVICE_NAME="my-service"
```

gRPC 示例：

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT="http://<databuff-ingest-host>:4317"
export OTEL_EXPORTER_OTLP_PROTOCOL="grpc"
```

**OpenTelemetry Collector**（将原指向 Jaeger 的 exporter 改为 DataBuff）：

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
```

K8s / 容器：将原 `jaeger-collector:4317`、`jaeger:4318` 等改为 DataBuff Ingest，滚动重启。

### 3. 分批切流

1. 先改 1–2 个非核心服务或单个 Collector pipeline  
2. 在 DataBuff Web 完成验收（见下）  
3. 按业务域逐步扩批  
4. 稳定后可下线 Jaeger Collector / Query  

## 验收与回滚

| 检查项 | 预期 |
|--------|------|
| 服务 | 应用性能页出现 `OTEL_SERVICE_NAME` |
| Trace | 新请求可查完整瀑布图 |
| 指标 / 日志 | 若 SDK 已上报则有对应数据 |
| 告警 | 在 DataBuff **重新配置**（Jaeger 无内置告警，Grafana 规则不自动迁移） |

**回滚**：`OTEL_EXPORTER_OTLP_ENDPOINT`（或 Collector exporter）改回 Jaeger 地址并重启。变更单保留改前 / 改后两个值。

**不自动迁移**：Jaeger ES / Cassandra 中的历史 Trace；需在 DataBuff 侧按需重建告警与大盘。

## 延伸阅读

- [DataBuff vs Jaeger](../业界对比/vs-jaeger.md)
- [OpenTelemetry OTLP 接入](../opentelemetry-otlp-ingestion.md)
- [Docker 安装部署](../快速入门/docker安装部署.md)
