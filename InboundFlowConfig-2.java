routername: cncdcdev1primary
      redundancy_enable: "yes"
      redundancy_authentication_presharedkey_key: "<PSK>"
      redundancy_activestandbyrole: primary
      redundancy_matelink_connectvia: cdc-dev1-b.51080.cn.app.example.com
      configsync_enable: "yes"
      redundancy_group_node_cncdcdev1primary_nodetype: message_routing
      redundancy_group_node_cncdcdev1primary_connectvia: cdc-dev1-a.51080.cn.app.example.com
      redundancy_group_node_cncdcdev1backup_nodetype: message_routing
      redundancy_group_node_cncdcdev1backup_connectvia: cdc-dev1-b.51080.cn.app.example.com
      redundancy_group_node_cncdcdev1monitor_nodetype: monitoring
      redundancy_group_node_cncdcdev1monitor_connectvia: cdc-dev1-c.51080.cn.app.example.com

  routername: cncdcdev1backup
      redundancy_activestandbyrole: backup
      redundancy_matelink_connectvia: cdc-dev1-a.51080.cn.app.example.com
      # configsync_enable: "yes"  (keep, same as primary)
      # the 6 redundancy_group_node_* lines are IDENTICAL to primary

  routername: cncdcdev1monitor
      redundancy_enable: "yes"
      redundancy_authentication_presharedkey_key: "<PSK>"
      redundancy_group_node_cncdcdev1primary_nodetype: message_routing
      redundancy_group_node_cncdcdev1primary_connectvia: cdc-dev1-a.51080.cn.app.example.com
      redundancy_group_node_cncdcdev1backup_nodetype: message_routing
      redundancy_group_node_cncdcdev1backup_connectvia: cdc-dev1-b.51080.cn.app.example.com
      redundancy_group_node_cncdcdev1monitor_nodetype: monitoring
      redundancy_group_node_cncdcdev1monitor_connectvia: cdc-dev1-c.51080.cn.app.example.com
