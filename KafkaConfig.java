# SYSTEM_HA_REDUN_GROUP_NODE_LEFT - v2
#
# Replaces the increase()/unless approach, which fired based on when old events
# aged out of a 24h window rather than when events actually occurred.
#
# How this works:
#   left_total - joined_total  = number of nodes currently out of the redundancy group,
#                                plus a fixed per-host offset from unmatched startup joins.
#   The median over 3 days     = that fixed offset (the "everything healthy" baseline).
#   Subtracting the two        = 0 when healthy, >0 during an actual outage.
#
# Median (quantile 0.5) is used rather than min_over_time because the difference
# excursions in both directions; min would latch onto a downward transient and
# hold a false baseline for 3 days.
#
# BEFORE DEPLOYING: route this to a test contact point and shadow-run for a week
# against Kibana before pointing it at the production receiver.

- uid: aexqk11rdophca-v2
  title: SYSTEM_HA_REDUN_GROUP_NODE_LEFT STS
  condition: B
  # Pending period: condition must hold 5 continuous minutes before firing.
  # Observed real outages last 20-30 min, so this is safely inside them.
  for: 5m
  data:
    - refId: A
      relativeTimeRange:
        from: 600
        to: 0
      datasourceUid: c2d2d69e-138c-462b-9f5b-84e530148fae
      model:
        datasource:
          type: prometheus
          uid: c2d2d69e-138c-462b-9f5b-84e530148fae
        editorMode: code
        expr: |-
          (
            sum by (host, vmrHostAlias) (sol_event_system_ha_redun_group_node_left_total)
            -
            sum by (host, vmrHostAlias) (sol_event_system_ha_redun_group_node_joined_total)
          )
          -
          quantile_over_time(0.5,
            (
              sum by (host, vmrHostAlias) (sol_event_system_ha_redun_group_node_left_total)
              -
              sum by (host, vmrHostAlias) (sol_event_system_ha_redun_group_node_joined_total)
            )[3d:5m]
          )
        instant: true
        intervalMs: 1000
        legendFormat: __auto
        maxDataPoints: 43200
        range: false
        refId: A

    - refId: B
      relativeTimeRange:
        from: 600
        to: 0
      datasourceUid: __expr__
      model:
        datasource:
          name: Expression
          type: __expr__
          uid: __expr__
        expression: $A > 0
        intervalMs: 1000
        maxDataPoints: 43200
        refId: B
        type: math

  dashboardUid: arHPdgYmk
  panelId: 112

  noDataState: OK
  # Error (not KeepLast) so Prometheus query failures are visible instead of
  # silently freezing the alert in its previous state.
  execErrState: Error

  annotations:
    __dashboardUid__: arHPdgYmk
    __panelId__: "112"
    description: >-
      HA node left the redundancy group on {{ $labels.host }}
      ({{ $labels.vmrHostAlias }}). Nodes currently out: {{ $values.A.Value }}.
    summary: "Documentation: https://docs.solace.com/Solace-PubSub-Event-Reference/event_ref_boiler.html#SYSTEM_HA_REDUN_GROUP_NODE_LEFT \n\nKibana: https://kibana.example.com/app/discover#/?_g=(filters:!(),refreshInterval:(pause:!t,value:120000),time:(from:now-1d,to:now))&_a=(columns:!(vmrHost,vmrHostAlias,syslogEvent,message),dataSource:(dataViewId:ef11adb1-12c0-4374-805c-1d8a8fd4dde1,type:dataView),filters:!(('$state':(store:appState),meta:(alias:!n,disabled:!f,field:appId,index:'logs-*',key:appId,negate:!f,params:(query:'myID'),type:phrase),query:(match_phrase:(appId:'myID'))),('$state':(store:appState),meta:(alias:!n,disabled:!f,field:syslogEvent,index:ef11adb1-12c0-4374-805c-1d8a8fd4dde1,key:syslogEvent,negate:!f,params:!(SYSTEM_HA_REDUN_GROUP_NODE_LEFT,SYSTEM_HA_REDUN_GROUP_NODE_JOINED),type:phrases,value:!(SYSTEM_HA_REDUN_GROUP_NODE_LEFT,SYSTEM_HA_REDUN_GROUP_NODE_JOINED)),query:(bool:(minimum_should_match:1,should:!((match_phrase:(syslogEvent:SYSTEM_HA_REDUN_GROUP_NODE_LEFT)),(match_phrase:(syslogEvent:SYSTEM_HA_REDUN_GROUP_NODE_JOINED))))))),grid:(columns:(solaceEventData.client:(width:436),syslogEvent:(width:275),vmrHost:(width:190),vmrHostAlias:(width:211))),interval:auto,query:(language:kuery,query:''),sort:!(!('@timestamp',desc)))"

  labels:
    CI: "myID"
    SupportGroup: ""
    alertmetric: Application
    alertparam: Usage
    alertresource: Application
    alerttype: Pod
    app_id: "myID"
    app_name: FOUNDATION SERVICES SOLACE BA
    auto_inc: ""
    cmsid: COP-GRAF-001
    itam-id: "myID"
    itam_id: "myID"
    name: SYSTEM_HA_REDUN_GROUP_NODE_LEFT STS
    severity: error
    source: grafana
    supportgroup: ""
    value: ""

  isPaused: false

  notification_settings:
    # CHANGE THIS to a test receiver for the shadow-run period.
    receiver: "myID"
    group_by:
      - grafana_folder
      - alertname
      - host
      - vmrHostAlias
    repeat_interval: 30m
