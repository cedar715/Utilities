services:
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
    environment:
      username_admin_globalaccesslevel: admin
      username_admin_password: <REAL-ADMIN-PASSWORD>
      system_scaling_maxconnectioncount: "100"
