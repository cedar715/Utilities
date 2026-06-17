Common base + per-role env — write /home/solace/solace/solace-compose.yml (or wherever your unit's -f points) on each node:
yamlservices:
  solace:
    image: artifactory.global.example.com/gv-images-products/oss/solace-pubsub-enterprise:10.25.0.217
    container_name: solace
    network_mode: host
    uts: host
    user: "0:0"
    shm_size: 1g
    ulimits:
      core: -1
      nofile:
        soft: 2448
        hard: 1048576
    volumes:
      - /opt/apps/solace/storage-group:/var/lib/solace:Z
      - /home/solace/secrets:/run/secrets:Z
    environment:
      username_admin_globalaccesslevel: admin
      username_admin_passwordfilepath: /run/secrets/username_admin_password
      system_scaling_maxconnectioncount: "100"
      redundancy_enable: "yes"
      redundancy_authentication_presharedkey_keyfilepath: /run/secrets/solace-preshared-key.conf
      redundancy_group_node_cncdcdev1primary_connectvia: cdc-dev1-a.51080.cn.app.example.com
      redundancy_group_node_cncdcdev1primary_nodetype: message_routing
      redundancy_group_node_cncdcdev1backup_connectvia: cdc-dev1-b.51080.cn.app.example.com
      redundancy_group_node_cncdcdev1backup_nodetype: message_routing
      redundancy_group_node_cncdcdev1monitor_connectvia: cdc-dev1-c.51080.cn.app.example.com
      redundancy_group_node_cncdcdev1monitor_nodetype: monitoring
Then append the per-role lines to environment::
VM1 (primary):
yaml      routername: cncdcdev1primary
      redundancy_activestandbyrole: primary
      redundancy_matelink_connectvia: cdc-dev1-b.51080.cn.app.example.com
      configsync_enable: "yes"
VM2 (backup):
yaml      routername: cncdcdev1backup
      redundancy_activestandbyrole: backup
      redundancy_matelink_connectvia: cdc-dev1-a.51080.cn.app.example.com
      configsync_enable: "yes"
VM3 (monitor):
yaml      routername: cncdcdev1monitor
