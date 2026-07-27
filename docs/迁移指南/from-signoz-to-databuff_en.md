# From SigNoz to DataBuff

Keep your **OpenTelemetry SDK / Collector** setup; retarget OTLP from SigNoz to DataBuff Ingest (gRPC **4317** / HTTP **4318**). No probe swap, no application code changes. For capability differences, see [DataBuff vs SigNoz](../业界对比/vs-signoz_en.md).

## Migration model

```
Before:  App / Collector  ──OTLP──▶  SigNoz OTel Collector (often :4317 / :4318)
After:   App / Collector  ──OTLP──▶  DataBuff Ingest
```

**Core action: change `OTEL_EXPORTER_OTLP_ENDPOINT` (or Collector exporter) from SigNoz to DataBuff Ingest.**

Roll out **canary → verify → expand**; small fleets can update all endpoints in one window.

**Prerequisites**

- [DataBuff deployed](../快速入门/docker安装部署_en.md)
- Ingest OTLP reachable (Docker **4317** / **4318**, K8s often **31417** / **31418**)
- Note the current SigNoz Collector address (e.g. `signoz-otel-collector:4317`) for rollback

Protocol details: [OpenTelemetry OTLP ingestion](../opentelemetry-otlp-ingestion_en.md).

## Steps

### 1. Ingest endpoint

| Deployment | OTLP gRPC | OTLP HTTP |
|------------|-----------|-----------|
| Docker | `<ingest-host>:4317` | `http://<ingest-host>:4318` |
| Kubernetes | `<node-ip>:31417` etc. | `http://<node-ip>:31418` etc. |

### 2. Update export target

**App → SigNoz Collector directly**

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT="http://<databuff-ingest-host>:4318"
export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
export OTEL_SERVICE_NAME="my-service"
```

Replace addresses that pointed at `signoz-otel-collector:4317` (or the Helm Service) with DataBuff Ingest.

**Self-managed OTel Collector** (when the exporter previously wrote to SigNoz / ClickHouse):

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

K8s / containers: update OTLP env vars or ConfigMap entries and roll pods.

### 3. Rollout

1. Change one or two non-critical services  
2. Verify in DataBuff Web (below)  
3. Expand by domain  
4. Decommission SigNoz Query / ClickHouse writes when stable  

## Acceptance and rollback

| Check | Expected |
|-------|----------|
| Services | Listed under Application Performance |
| Traces | New requests show full traces |
| Metrics / logs | Visible if the SDK exports them |
| Alerting | **Re-create** rules in DataBuff (SigNoz alert rules do not migrate) |

**Rollback:** point OTLP back to the SigNoz Collector and restart. Keep before/after values in the change ticket.

**Not auto-migrated:** SigNoz historical data, alerts, and custom dashboards—rebuild in DataBuff as needed.

## See also

- [DataBuff vs SigNoz](../业界对比/vs-signoz_en.md)
- [OpenTelemetry OTLP ingestion](../opentelemetry-otlp-ingestion_en.md)
- [Docker Installation](../快速入门/docker安装部署_en.md)
