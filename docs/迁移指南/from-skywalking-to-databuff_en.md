# From SkyWalking to DataBuff

Keep your **SkyWalking Agent**; point ingest from OAP to DataBuff Ingest (gRPC **11800**). No probe swap, no application code changes. For capability differences, see [DataBuff vs SkyWalking](../业界对比/vs-skywalking_en.md).

## Migration model

```
Before:  Agent  ──gRPC:11800──▶  SkyWalking OAP
After:   Agent  ──gRPC:11800──▶  DataBuff Ingest
```

**Core action: update `agent.backend_service` to the DataBuff Ingest host; keep port 11800.**

Roll out **canary → verify → expand**; small fleets can update all backends in one window.

**Prerequisites**

- [DataBuff deployed](../快速入门/docker安装部署_en.md)
- Ingest SkyWalking gRPC reachable (Docker **11800**, K8s often **31180**)
- Note the current OAP address for rollback

Protocol details: [SkyWalking Ingestion](../使用手册/SkyWalking接入_en.md).

## Steps

### 1. Ingest endpoint

| Deployment | Point Agent to |
|------------|----------------|
| Docker | `<ingest-host>:11800` |
| Kubernetes | `<node-ip>:31180` or Ingress `11800` |

### 2. Update backend

`agent.config`:

```properties
agent.backend_service=<databuff-ingest-host>:11800
agent.service_name=my-service
```

Or JVM flag:

```bash
-Dskywalking.collector.backend_service=<databuff-ingest-host>:11800
```

K8s / containers: replace `oap:11800` with DataBuff Ingest and roll pods.

### 3. Rollout

1. Change one or two non-critical services and restart  
2. Verify in DataBuff Web (below)  
3. Expand by domain  
4. Decommission OAP/UI when stable  

## Acceptance and rollback

| Check | Expected |
|-------|----------|
| Services | Listed under Application Performance |
| Traces | New data; `data.source` = `SkyWalking` |
| JVM / logs | Visible if Agent reports them |
| Alerting | **Re-create** rules in DataBuff (OAP rules do not migrate) |

**Rollback:** set `backend_service` back to OAP and restart. Keep before/after values in the change ticket.

**Not auto-migrated:** historical OAP traces/metrics and alarm YAML—rebuild in DataBuff as needed.

## See also

- [DataBuff vs SkyWalking](../业界对比/vs-skywalking_en.md)
- [SkyWalking Ingestion](../使用手册/SkyWalking接入_en.md)
- [Docker Installation](../快速入门/docker安装部署_en.md)
