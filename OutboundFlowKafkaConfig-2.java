# Kafka Client Topic Dashboard — design & adaptation guide

**v1 scope:** single-topic deep dive, broker-side metrics only, Grafana.
**Deliverable:** `kafka-client-topic-dashboard.json` (uid `kafka-client-topic`).

---

## 1. The design principle

A client dashboard is not the platform dashboard with fewer panels. Clients open a
dashboard for exactly three reasons:

1. **"Is my consumer keeping up?"** — the daily glance.
2. **"Something is wrong — is it me or is it Kafka?"** — the 2am question.
3. **"Am I going to outgrow something?"** — retention and storage, monthly.

Every panel in this dashboard serves one of those. Broker JVM heap, controller
elections, ISR churn internals, and per-node CPU are deliberately absent — they
make a client feel informed without making them able to act.

Question 2 is the one with the business case. If the dashboard lets an app team
self-serve the answer to "is the platform degraded or is it my pods", your team
stops being the first line of triage for every application incident. That is why
the **"Is it me, or is it the platform?"** row sits second, above all the detail.

---

## 2. Import it

1. Grafana → Dashboards → New → Import → upload the JSON.
2. Pick your Prometheus data source when prompted.
3. Work through §3 below — a handful of metric/label names will need adjusting
   for your exporter config.

The dashboard has no hardcoded data source UID; it uses a `datasource` template
variable, so it is portable across environments.

---

## 3. Assumptions to verify before it works

### 3.1 Metric names

Built against `danielqsj/kafka_exporter` and the common lowercase JMX exporter
naming. Check each against your Prometheus and find-replace in the JSON if needed.

| Purpose | Metric assumed | Source |
|---|---|---|
| Partition count | `kafka_topic_partitions` | kafka_exporter |
| Log end offset | `kafka_topic_partition_current_offset` | kafka_exporter |
| Log start offset | `kafka_topic_partition_oldest_offset` | kafka_exporter |
| Replica count | `kafka_topic_partition_replicas` | kafka_exporter |
| ISR count | `kafka_topic_partition_in_sync_replica` | kafka_exporter |
| Under-replicated flag | `kafka_topic_partition_under_replicated_partition` | kafka_exporter |
| Leader broker | `kafka_topic_partition_leader` | kafka_exporter |
| Preferred leader flag | `kafka_topic_partition_leader_is_preferred` | kafka_exporter |
| Consumer lag | `kafka_consumergroup_lag` | kafka_exporter |
| Group committed offset | `kafka_consumergroup_current_offset` | kafka_exporter |
| Group member count | `kafka_consumergroup_members` | kafka_exporter |
| Broker count | `kafka_brokers` | kafka_exporter |
| Messages in | `kafka_server_brokertopicmetrics_messagesin_total` | JMX |
| Bytes in / out / rejected | `kafka_server_brokertopicmetrics_bytes{in,out,rejected}_total` | JMX |
| Failed produce / fetch | `kafka_server_brokertopicmetrics_failed{produce,fetch}requests_total` | JMX |
| Log size | `kafka_log_log_size` | JMX |
| Cluster under-replicated | `kafka_server_replicamanager_underreplicatedpartitions` | JMX |
| Offline partitions | `kafka_controller_kafkacontroller_offlinepartitionscount` | JMX |
| Disk | `node_filesystem_avail_bytes` / `node_filesystem_size_bytes` | node_exporter |
| Endpoint probe | `probe_success` | blackbox |
| Cert expiry | `probe_ssl_earliest_cert_expiry` | blackbox |

If your JMX exporter config emits `kafka_server_BrokerTopicMetrics_MessagesInPerSec`
style names instead (older configs), the mapping is mechanical — the semantics are
identical, only the case and the `_total` suffix differ.

### 3.2 Labels

- **`cluster`** — assumed present on all Kafka metrics. Every query uses
  `cluster=~"$cluster"` (regex, not equality) so that if the label does *not*
  exist, `.*` still matches and nothing breaks. Zero-cost insurance.
- **`team`** — used **only** in the topic dropdown query, not in panel queries.
  This is deliberate: you only have to attach ownership metadata to the
  `kafka_topic_partitions` series, not to every metric in the stack.
- **`topic`, `partition`, `consumergroup`** — standard exporter labels.

### 3.3 Two placeholders you must edit

Both are in the platform-health row and are marked in the panel descriptions:

- **Min broker disk free** — the mountpoint regex
  `/var/lib/kafka.*|/data.*|/kafka.*`. Set it to your actual log dirs, or the
  panel will report the OS root filesystem and lie to you.
- **Bootstrap endpoints / TLS cert expiry** — the matchers
  `job=~"blackbox.*", instance=~".*kafka.*"`. Point these at your real blackbox
  targets.

---

## 4. The variable model

```
datasource → cluster → team → topic → consumergroup
```

Chained, in that order. Two choices worth explaining:

**Team gates the topic list.** On a cluster with thousands of topics, a client
scrolling an unfiltered dropdown is a bad first impression and a support ticket.
Internal topics are excluded at the query level with `topic!~"__.*|_confluent.*|_schemas"`
— note that Grafana's variable regex filter uses RE2, which has no negative
lookahead, so the exclusion has to live in the PromQL selector, not the regex field.

**Consumer group is multi-select and chained off topic.** This is the parameter
people forget. Lag is meaningless without it — a topic with three consumer groups
has three different answers to "am I behind", and one healthy group will mask a
dead one in any summed view.

---

## 5. Panel reference

### Row 1 — At a glance

| Panel | Why it exists |
|---|---|
| **Seconds behind** | The headline number. Message-count lag is not comparable across topics; time is. `lag ÷ produce rate`. |
| Total lag (messages) | The number people ask for, kept because they ask for it. |
| Produce rate / Consume rate | Side by side, the delta tells you whether lag is growing. |
| Under-replicated partitions | Scoped to *this* topic — the fastest "not my fault" signal. |
| **Since last message produced** | Silence detection. Traffic going to zero is the most common real incident and the one that no time-series chart shows well, because a flat line at zero looks like a flat line. |

### Row 2 — Is it me, or is it the platform?

Brokers online · cluster under-replicated · offline partitions · min broker disk ·
endpoint reachability · TLS cert expiry, plus a text panel stating the decision
rule explicitly. All green + growing lag = client-side. Anything red = platform-side.

### Row 3 — Producer side

Messages/sec (two independent derivations, offsets and JMX, so a scrape gap is
visible), bytes in/out/rejected, average message size, fan-out ratio, failed
requests.

**Average message size** catches schema bloat before it becomes a
`message.max.bytes` incident. **Fan-out ratio** (bytes out ÷ bytes in) tells a
client how many consumers are actually reading — a drop toward zero means someone
stopped; a jump means someone started replaying from the beginning.

### Row 4 — Consumer side

Lag by group, seconds-behind by group, produce-vs-consume on one axis, group member
count, and a per-partition lag table.

**Produce vs consume on a shared axis** is the most diagnostic chart here. Note it
is a shared axis, not a dual axis — both series are messages/sec. Dual-axis charts
are the single most common way to make two unrelated series look correlated, and
there are none in this dashboard.

**Member count** turns "lag spiked at 3am, why?" into a five-second answer. A dip to
zero is dead pods; repeated sawtoothing is a rebalance loop, usually
`max.poll.interval.ms` being exceeded by slow processing.

**Lag by partition** as a sorted table, not a chart. A single stuck partition
vanishes completely in a summed lag line.

### Row 5 — Partition balance

Message rate and data volume per partition, as bar gauges. Clients never think
about this and it is the root cause of a large share of "my consumer is slow"
tickets: a skewed partition key creates one hot partition, which saturates one
consumer thread while the rest idle. It presents exactly like a capacity problem,
and adding consumers does not help. Two cheap panels, high hit rate.

### Row 6 — Storage & retention

Topic size (sum across brokers, so it includes replicas — divide by RF for logical
size), **retention actually achieved**, and projected daily growth.

Retention-achieved is worth calling out: it is computed from real offsets and real
traffic, so it reflects size-based eviction. It is usually well short of the
`retention.ms` in the topic config that nobody re-checks, and it is the number that
matters when a client asks "can I replay yesterday?"

### Rows 7–8 — Replication detail, and a read-me-once methodology panel

Both collapsed by default.

---

## 6. Known limits — broker-side only

Everything here is measured at the broker. That answers "is my topic healthy" and
"is my consumer keeping up". It cannot answer "why is my producer slow". The broker
cannot see:

- producer retry rate, batch size, buffer exhaustion, client-observed latency
- consumer poll duration, processing time, `records-lag-max` from the client's view
- rebalance counts and durations (member-count dips are a proxy, not the truth)
- end-to-end produce→consume latency

Adding client-side scraping (JMX exporter sidecar, or Micrometer on the apps) is
the single biggest upgrade available to this dashboard, and it is the difference
between a dashboard that diagnoses platform problems and one that diagnoses
application problems.

### Sampling caveat

`kafka_exporter` polls the cluster on each scrape, so lag is a **snapshot**. At 30s
scrape intervals and high throughput, lag charts look spiky — that is sampling, not
your consumer. Never alert on a single sample.

---

## 7. Performance: recording rules

Per-partition, per-group lag series multiply fast. On a large cluster, pre-aggregate
before this dashboard becomes the reason your Prometheus is slow:

```yaml
groups:
  - name: kafka-client-dashboard
    interval: 30s
    rules:
      - record: kafka:consumergroup_lag:sum
        expr: sum by (cluster, consumergroup, topic) (kafka_consumergroup_lag)

      - record: kafka:topic_produce_rate:sum
        expr: sum by (cluster, topic) (rate(kafka_topic_partition_current_offset[5m]))

      - record: kafka:consumergroup_consume_rate:sum
        expr: sum by (cluster, consumergroup, topic) (rate(kafka_consumergroup_current_offset[5m]))

      - record: kafka:consumergroup_seconds_behind
        expr: |
          kafka:consumergroup_lag:sum
            / on (cluster, topic) group_left()
          clamp_min(kafka:topic_produce_rate:sum, 1)

      - record: kafka:topic_size_bytes:sum
        expr: sum by (cluster, topic) (kafka_log_log_size)
```

Then swap the summary panels over to the recorded series. Keep the raw per-partition
queries only in the drilldown panels, which are viewed rarely.

---

## 8. Suggested client-facing alerts

Alert on the same numbers the dashboard leads with, so an alert always maps to a
panel. Thresholds are starting points — tune per topic.

| Alert | Condition | Rationale |
|---|---|---|
| Consumer falling behind | `kafka:consumergroup_seconds_behind > 300` for 10m | Time, not message count |
| Consumer stopped | consume rate `== 0` for 10m while produce rate `> 0` | Catches the dead-pod case that lag alone reaches slowly |
| Topic silent | no produce for 30m on a topic that normally has traffic | The incident nobody alerts on |
| Rejected messages | `rate(bytesrejected) > 0` for 5m | Always a producer bug |
| Replay window shrinking | retention-achieved `< 4h` | Warn before the replay budget is gone, not after |

Use `for:` on every one — see the sampling caveat.

---

## 9. Roadmap

**v1.1 — cheap wins on the same data**
- Team-level overview dashboard: one row per owned topic, health grid, drilldown
  link into this dashboard with `$topic` pre-set. This is what people will actually
  bookmark; the topic view is where they land from it.
- Dashboard links to the platform dashboard and to your topic-request runbook.

**v1.2 — requires client-side metrics**
- Producer panel: retry rate, batch size, record-send latency, buffer exhaustion.
- Consumer panel: poll interval, processing time, rebalance rate and duration.
- End-to-end latency, if apps stamp produce time into headers.

**v1.3 — requires extra collectors**
- Offset commit freshness and lag *evaluation status* (Burrow, or
  `kafka-lag-exporter` for a broker-independent time-lag measure).
- Quota usage per client id, if you enforce quotas — clients cannot currently see
  when they are being throttled, and throttling looks exactly like a slow consumer.
