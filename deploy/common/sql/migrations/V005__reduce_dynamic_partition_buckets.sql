-- 0.1.4 -> next: reduce dynamic-partition telemetry tables from BUCKETS 16 to 3.
-- Fresh installs already use BUCKETS 3 via databuff.sql (and stamp schema_version=5).
--
-- Only metadata: changes default / dynamic_partition bucket count for newly created
-- partitions. Existing partitions keep their original bucket count until retention
-- drops them (no data rewrite / tablet rebuild).
--
-- Use both statements so table default and dynamic_partition.buckets stay aligned
-- (if both are set, Doris prefers dynamic_partition.buckets for auto-created partitions).
-- ALTER to the same value is idempotent, so a retry after partial apply is safe.

USE databuff;

ALTER TABLE trace_dc_span
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`trace_id`) BUCKETS 3;
ALTER TABLE trace_dc_span
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE log_dc_record
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`trace_id`) BUCKETS 3;
ALTER TABLE log_dc_record
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_jvm
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_jvm
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_config
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_config
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_cpu
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_cpu
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_db
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_db
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_db_connection_pool
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_db_connection_pool
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_db_connection_pool_get
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_db_connection_pool_get
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_exception
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_exception
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_flow
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_flow
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_health_status
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_health_status
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_http
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_http
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_http_connection_pool
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_http_connection_pool
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_http_connection_pool_get
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_http_connection_pool_get
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_instance
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_instance
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_io
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_io
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_mem
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_mem
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_mq
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_mq
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_net
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_net
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_object_pool
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_object_pool
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_object_pool_get
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_object_pool_get
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_redis
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_redis
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_rpc
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_rpc
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_remote
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_remote
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_tcp
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_tcp
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_thread_pool
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_thread_pool
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_thread_pool_cost
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_thread_pool_cost
SET ("dynamic_partition.buckets" = "3");

ALTER TABLE metric_service_trace
MODIFY DISTRIBUTION DISTRIBUTED BY HASH(`service_id`) BUCKETS 3;
ALTER TABLE metric_service_trace
SET ("dynamic_partition.buckets" = "3");
