        - uid: ef3wvtjhxbh1cd
          title: VPN_AD_MSG_SPOOL_QUOTA_EXCEED
          condition: B
          data:
            - refId: A
              relativeTimeRange:
                from: 1800
                to: 0
              datasourceUid: c2d2d69e-138c-462b-9f5b-84e530148fae
              model:
                datasource:
                    type: prometheus
                    uid: c2d2d69e-138c-462b-9f5b-84e530148fae
                editorMode: code
                expr: "clamp_min(\r\n  (\r\n    sum by (host, queue, vmrHostAlias, vpn) (increase(sol_event_vpn_ad_msg_spool_quota_exceed_total[1d]))\r\n    unless\r\n    sum by (host, queue, vmrHostAlias, vpn) (increase(sol_event_vpn_ad_msg_spool_high_clear_total[1d])) > 0\r\n  ),\r\n  0\r\n)\r\n"
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
          dashboardUid: aegbeyarmdc00a
          panelId: 1
          noDataState: OK
          execErrState: KeepLast
          annotations:
            __dashboardUid__: aegbeyarmdc00a
            __panelId__: "1"
            description: |-
                Documentation: https://docs.solace.com/Admin-Ref/Solace-PubSub-Event-Reference/event_ref_boiler.html#VPN_AD_MSG_SPOOL_QUOTA_EXCEED

                Kibana: https://kibana.example.com/app/discover#/?_g=(filters:!(),refreshInterval:(pause:!t,value:60000),time:(from:now-1w,to:now))&_a=(columns:!(solaceEventData.queue,syslogEvent,vmrHostAlias,solaceEventData.vpn,message),dataSource:(dataViewId:'ef11adb1-12c0-4374-805c-1d8a8fd4dde1',type:dataView),filters:!(('$state':(store:appState),meta:(alias:!n,disabled:!f,index:'ef11adb1-12c0-4374-805c-1d8a8fd4dde1',key:syslogEvent,negate:!f,params:!(VPN_AD_MSG_SPOOL_QUOTA_EXCEED,VPN_AD_MSG_SPOOL_HIGH_CLEAR),type:phrases,value:!(VPN_AD_MSG_SPOOL_QUOTA_EXCEED,VPN_AD_MSG_SPOOL_HIGH_CLEAR)),query:(bool:(minimum_should_match:1,should:!((match_phrase:(syslogEvent:VPN_AD_MSG_SPOOL_QUOTA_EXCEED)),(match_phrase:(syslogEvent:VPN_AD_MSG_SPOOL_HIGH_CLEAR))))))),grid:(columns:(solaceEventData.queue:(width:284),solaceEventData.vpn:(width:170),syslogEvent:(width:260),vmrHostAlias:(width:154))),interval:auto,query:(language:kuery,query:'solaceEventData.vpn%20:%20%22{{ $labels.vpn }}%22%20and%20vmrHostAlias%20:%20%22{{ $labels.vmrHostAlias }}%22%20and%20solaceEventData.queue%20:%20%22{{ $labels.queue }}%22'),sort:!(!('@timestamp',desc)))
            summary: "Warning: The total size of messages in the VPN: {{ $labels.vpn }} / Queue: {{ $labels.queue }}  has exceeded its configured quota. Messages may be discarded until usage drops below the limit. Further, \nthe spooled messages if not consumed with-in 48hrs, would be purged.\n\nGrafana Dashboard:\nhttps://grafana.cop.awscloud.sc.net/d/solacequeuedb4clients/solace-clients-queue-dashboard?orgId=1&var-vpn={{ $labels.vpn }}&var-queue={{ $labels.queue }}"
          labels:
            CI: "myID"
            SupportGroup: ""
            alertmetric: Network Connectivity
            alertparam: Usage
            alertresource: Network Connectivity
            alerttype: Connectivity
            app_id: "myID"
            app_name: FOUNDATION SERVICES SOLACE BA
            auto_inc: ""
            client-itam-field: '{{ reReplaceAll `^q-([0-9]+)-.*` "$${1}" $$labels.queue }}'
            client-notification: "true"
            cmsid: COP-GRAF-001
            itam-id: "myID"
            itam_id: "myID"
            name: VPN_AD_MSG_SPOOL_QUOTA_EXCEED
            severity: critical
            source: grafana
            supportgroup: ""
            value: ""
          isPaused: true
