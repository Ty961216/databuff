# 从 SkyWalking 迁移到 DataBuff

保留 **SkyWalking Agent**，将上报地址从 OAP 改为 DataBuff Ingest（gRPC **11800**）。不换探针、不改业务代码。能力差异见 [DataBuff vs SkyWalking](../业界对比/vs-skywalking.md)。

## 迁移思路

```
迁移前：  Agent  ──gRPC:11800──▶  SkyWalking OAP
迁移后：  Agent  ──gRPC:11800──▶  DataBuff Ingest
```

**核心：修改 `agent.backend_service`，主机改为 DataBuff Ingest，端口保持 11800。**

推荐 **金丝雀 → 验收 → 分批扩**；服务少可一次性改全量地址并重启。

**前置条件**

- [DataBuff 已部署](../快速入门/docker安装部署.md)
- Ingest SkyWalking gRPC 可达（Docker `11800`，K8s 常见 `31180`）
- 记录当前 OAP 地址，便于回滚

协议细节见 [SkyWalking 接入](../使用手册/SkyWalking接入.md)。

## 操作步骤

### 1. 确认 Ingest 地址

| 部署 | Agent 指向 |
|------|------------|
| Docker | `<ingest-host>:11800` |
| Kubernetes | `<node-ip>:31180` 或 Ingress `11800` |

### 2. 修改上报地址

`agent.config`：

```properties
agent.backend_service=<databuff-ingest-host>:11800
agent.service_name=my-service
```

或 JVM 参数：

```bash
-Dskywalking.collector.backend_service=<databuff-ingest-host>:11800
```

K8s / 容器：将原 `oap:11800` 改为 DataBuff Ingest 地址，滚动重启 Pod。

### 3. 分批切流

1. 先改 1–2 个非核心服务并重启  
2. 在 DataBuff Web 完成验收（见下）  
3. 按业务域逐步扩批  
4. 稳定后可下线 OAP / UI  

## 验收与回滚

| 检查项 | 预期 |
|--------|------|
| 服务 | 应用性能页出现 `service_name` |
| Trace | 新请求可查；`data.source` = `SkyWalking` |
| JVM / Log | Agent 有上报则有数据 |
| 告警 | 在 DataBuff **重新配置**（OAP 规则不自动迁移） |

**回滚**：`backend_service` 改回 OAP 地址并重启应用。变更单保留改前 / 改后两个值。

**不自动迁移**：OAP 历史 Trace / 指标、告警 YAML；需在 DataBuff 侧按需重建。

## 延伸阅读

- [DataBuff vs SkyWalking](../业界对比/vs-skywalking.md)
- [SkyWalking 接入](../使用手册/SkyWalking接入.md)
- [Docker 安装部署](../快速入门/docker安装部署.md)
