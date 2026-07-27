# From Pinpoint to DataBuff

Pinpoint uses a **proprietary Java Agent**—you cannot retarget ingest like SkyWalking. Migration means **removing the Pinpoint Agent, switching to the OpenTelemetry Java Agent (or SkyWalking Agent)**, and sending telemetry to DataBuff Ingest. For capability differences, see [DataBuff vs Pinpoint](../业界对比/vs-pinpoint_en.md).

## Migration model

```
Before:  App + Pinpoint Agent  ──▶  Pinpoint Collector
After:   App + OTel Java Agent  ──OTLP──▶  DataBuff Ingest (:4317 / :4318)
     or App + SkyWalking Agent  ──gRPC:11800──▶  DataBuff Ingest
```

**Core action: swap the probe and point it at DataBuff.** Prefer the OTel Java Agent for a unified multi-language stack; SkyWalking Agent to DataBuff **11800** is an option for Java-only fleets with minimal protocol change.

Roll out **canary → verify → expand**.

**Prerequisites**

- [DataBuff deployed](../快速入门/docker安装部署_en.md)
- Ingest OTLP (**4317** / **4318**) or SkyWalking gRPC (**11800**) reachable
- Note the original `pinpoint.applicationName` and Agent flags for mapping to `OTEL_SERVICE_NAME` or `agent.service_name`
- Validate in staging that the Pinpoint Agent is fully removed to avoid dual-agent conflicts

Ingestion details: [OpenTelemetry OTLP ingestion](../opentelemetry-otlp-ingestion_en.md), [SkyWalking Ingestion](../使用手册/SkyWalking接入_en.md).

## Steps

### 1. Ingest endpoint

| Option | Agent | Point to |
|--------|-------|----------|
| OTel (recommended) | OpenTelemetry Java Agent | `<ingest-host>:4318` (HTTP) or `:4317` (gRPC) |
| Alternative | SkyWalking Java Agent | `<ingest-host>:11800` |

### 2. Remove Pinpoint Agent

Drop Pinpoint from JVM flags, for example:

```bash
# Remove lines like
-javaagent:/path/to/pinpoint-bootstrap.jar
-Dpinpoint.applicationName=my-service
-Dpinpoint.agentId=my-instance
```

Also remove `pinpoint.config` or related env/volume mounts.

### 3. Connect to DataBuff (OTel example)

```bash
java -javaagent:/path/to/opentelemetry-javaagent.jar \
  -Dotel.service.name=my-service \
  -Dotel.exporter.otlp.endpoint=http://<databuff-ingest-host>:4318 \
  -Dotel.exporter.otlp.protocol=http/protobuf \
  -jar my-app.jar
```

Or via environment variables:

```bash
export OTEL_SERVICE_NAME="my-service"
export OTEL_EXPORTER_OTLP_ENDPOINT="http://<databuff-ingest-host>:4318"
export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
```

**SkyWalking alternative** (Java-only, same port family as Pinpoint-style gRPC ingest):

```properties
agent.backend_service=<databuff-ingest-host>:11800
agent.service_name=my-service
```

K8s: update `JAVA_TOOL_OPTIONS` or the Agent init image in the Deployment and roll pods.

### 4. Rollout

1. Migrate one or two non-critical Java services  
2. Verify in DataBuff Web (below)  
3. Expand by domain  
4. Decommission Pinpoint Collector / Web when stable  

## Acceptance and rollback

| Check | Expected |
|-------|----------|
| Services | Listed under Application Performance with the chosen service name |
| Traces | New requests show traces (OTel auto-instrumentation—not Pinpoint method-level Call Tree) |
| Instances | Multiple replicas visible in the instance list |
| Alerting | **Re-create** rules in DataBuff (Pinpoint alerts do not migrate) |

**Rollback:** restore Pinpoint Agent flags and Collector address and restart. Keep before/after configs in the change ticket.

**Not auto-migrated:** Pinpoint historical traces, Inspector data, and alert rules; accept OTel default instrumentation depth vs Pinpoint method trees.

## See also

- [DataBuff vs Pinpoint](../业界对比/vs-pinpoint_en.md)
- [OpenTelemetry OTLP ingestion](../opentelemetry-otlp-ingestion_en.md)
- [SkyWalking Ingestion](../使用手册/SkyWalking接入_en.md)
- [Docker Installation](../快速入门/docker安装部署_en.md)
