        - uid: aexqk11rdophca
          title: SYSTEM_HA_REDUN_GROUP_NODE_LEFT STS
          condition: B
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
                expr: "clamp_min(\r\n  (\r\n    sum by (host, queue, vmrHostAlias, vpn) (increase(sol_event_system_ha_redun_group_node_left_total[1d]))\r\n    unless\r\n    sum by (host, queue, vmrHostAlias, vpn) (increase(sol_event_system_ha_redun_group_node_joined_total[1d])) > 0\r\n  ),\r\n  0\r\n)"
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
                conditions:
                    - evaluator:
                        params:
                            - 0
                            - 0
                        type: gt
                      operator:
                        type: and
                      query:
                        params: []
                      reducer:
                        params: []
                        type: avg
                      type: query
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
          execErrState: KeepLast
          annotations:
            __dashboardUid__: arHPdgYmk
            __panelId__: "112"
            description: 'HA node left without JOINED on {{ $labels.host }}. Current Unmatched: {{ $values.A }}.'
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
          isPaused: true
          notification_settings:
            receiver: "myID"
            group_by:
                - grafana_folder
                - alertname
                - host
            repeat_interval: 30m
