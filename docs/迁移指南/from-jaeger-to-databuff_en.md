# From Jaeger to DataBuff

Keep your **OpenTelemetry SDK / Collector** setup; retarget OTLP from Jaeger to DataBuff Ingest (gRPC **4317** / HTTP **4318**). No probe swap, no application code changes. For capability differences, see [DataBuff vs Jaeger](../业界对比/vs-jaeger_en.md).

## Migration model

```
Before:  App / Collector  ──OTLP──▶  Jaeger (:4317 / :4318 or Collector)
After:   App / Collector  ──OTLP──▶  DataBuff Ingest
```

**Core action: point `OTEL_EXPORTER_OTLP_ENDPOINT` (or Collector `exporters.otlp.endpoint`) at DataBuff Ingest.**

Roll out **canary → verify → expand**; small fleets can update all endpoints in one window.

**Prerequisites**

- [DataBuff deployed](../快速入门/docker安装部署_en.md)
- Ingest OTLP reachable (Docker **4317** / **4318**, K8s often **31417** / **31418**)
- Note the current Jaeger OTLP / Collector address for rollback
- Apps already send **OTLP** (OpenTelemetry SDK or via OTel Collector)

> Legacy Jaeger Agent (UDP Thrift) traffic must go through an OTel Collector `jaeger` receiver and export OTLP to DataBuff, or upgrade to an OTLP SDK.

Protocol details: [OpenTelemetry OTLP ingestion](../opentelemetry-otlp-ingestion_en.md).

## Steps

### 1. Ingest endpoint

| Deployment | OTLP gRPC | OTLP HTTP |
|------------|-----------|-----------|
| Docker | `<ingest-host>:4317` | `http://<ingest-host>:4318` |
| Kubernetes | `<node-ip>:31417` etc. | `http://<node-ip>:31418` etc. |

### 2. Update export target

**Application SDK (env vars)**

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT="http://<databuff-ingest-host>:4318"
export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
export OTEL_SERVICE_NAME="my-service"
```

gRPC example:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT="http://<databuff-ingest-host>:4317"
export OTEL_EXPORTER_OTLP_PROTOCOL="grpc"
```

**OpenTelemetry Collector** (replace the Jaeger-bound exporter with DataBuff):

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

K8s / containers: replace `jaeger-collector:4317`, `jaeger:4318`, etc. with DataBuff Ingest and roll pods.

### 3. Rollout

1. Change one or two non-critical services or a single Collector pipeline  
2. Verify in DataBuff Web (below)  
3. Expand by domain  
4. Decommission Jaeger Collector / Query when stable  

## Acceptance and rollback

| Check | Expected |
|-------|----------|
| Services | Listed under Application Performance |
| Traces | New requests show full waterfall views |
| Metrics / logs | Visible if the SDK exports them |
| Alerting | **Re-create** rules in DataBuff (Jaeger has no built-in alerts; Grafana rules do not migrate) |

**Rollback:** set `OTEL_EXPORTER_OTLP_ENDPOINT` (or Collector exporter) back to Jaeger and restart. Keep before/after values in the change ticket.

**Not auto-migrated:** historical traces in Jaeger ES / Cassandra; rebuild alerts and dashboards in DataBuff as needed.

## See also

- [DataBuff vs Jaeger](../业界对比/vs-jaeger_en.md)
- [OpenTelemetry OTLP ingestion](../opentelemetry-otlp-ingestion_en.md)
- [Docker Installation](../快速入门/docker安装部署_en.md)
