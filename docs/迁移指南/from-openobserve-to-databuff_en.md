# From OpenObserve to DataBuff

Keep your **OpenTelemetry SDK / Collector** setup; retarget OTLP from OpenObserve to DataBuff Ingest (gRPC **4317** / HTTP **4318**). No probe swap, no application code changes. For capability differences, see [DataBuff vs OpenObserve](../业界对比/vs-openobserve_en.md).

## Migration model

```
Before:  App / Collector  ──OTLP HTTP──▶  OpenObserve (often :5080/api/default)
After:   App / Collector  ──OTLP──▶  DataBuff Ingest
```

**Core action: change `OTEL_EXPORTER_OTLP_ENDPOINT` from OpenObserve’s `/api/<org>` path to DataBuff Ingest (`:4318` HTTP or `:4317` gRPC).**

Roll out **canary → verify → expand**; small fleets can update all endpoints in one window.

**Prerequisites**

- [DataBuff deployed](../快速入门/docker安装部署_en.md)
- Ingest OTLP reachable (Docker **4317** / **4318**)
- Note the current OpenObserve OTLP address (e.g. `http://openobserve:5080/api/default`) for rollback

Protocol details: [OpenTelemetry OTLP ingestion](../opentelemetry-otlp-ingestion_en.md).

## Steps

### 1. Ingest endpoint

| Deployment | OTLP gRPC | OTLP HTTP |
|------------|-----------|-----------|
| Docker | `<ingest-host>:4317` | `http://<ingest-host>:4318` |
| Kubernetes | `<node-ip>:31417` etc. | `http://<node-ip>:31418` etc. |

### 2. Update export target

**Application SDK**

```bash
# Before (OpenObserve example)
# export OTEL_EXPORTER_OTLP_ENDPOINT="http://openobserve:5080/api/default"

export OTEL_EXPORTER_OTLP_ENDPOINT="http://<databuff-ingest-host>:4318"
export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
export OTEL_SERVICE_NAME="my-service"
```

If OpenObserve used Basic Auth on the exporter, remove those headers; DataBuff Ingest does not require them by default.

**OpenTelemetry Collector**:

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

K8s / containers: update env vars or Collector ConfigMap and roll pods.

### 3. Rollout

1. Change one or two non-critical services  
2. Verify in DataBuff Web (below)  
3. Expand by domain  
4. Stop sending APM traffic to OpenObserve when stable (logs / dashboards can stay on OpenObserve if needed)  

## Acceptance and rollback

| Check | Expected |
|-------|----------|
| Services | Listed under Application Performance |
| Traces | New requests show full traces |
| Metrics | Golden-signal curves if the SDK exports metrics |
| Alerting | **Re-create** rules in DataBuff (OpenObserve alerts do not migrate) |

**Rollback:** set `OTEL_EXPORTER_OTLP_ENDPOINT` back to OpenObserve and restart. Keep before/after values in the change ticket.

**Not auto-migrated:** historical traces/metrics in OpenObserve, log pipelines, and dashboards—logs can remain on OpenObserve independently.

## See also

- [DataBuff vs OpenObserve](../业界对比/vs-openobserve_en.md)
- [OpenTelemetry OTLP ingestion](../opentelemetry-otlp-ingestion_en.md)
- [Docker Installation](../快速入门/docker安装部署_en.md)
