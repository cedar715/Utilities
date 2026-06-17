# 1. where are the mount vars + user DEFINED? (so compose reuses, not redefines)
grep -rn 'host_storage_group\|container_storage_group\|apps_root\|host_secrets\|container_secrets' \
  group_vars/ inventories/ roles/*/defaults/ roles/*/vars/ 2>/dev/null | grep -iv '#'
appadmintemp@cnlvatfss003[DEV][51080-solace-pubsubplus-podman-jgd] $ grep -rn 'host_storage_group\|apps_root\|/var/lib/solace' roles/solace_runtime_deploy/ group_vars/ inventories/ | grep -iv '#' | head
roles/solace_runtime_deploy/tasks/main.yml:34:    - "{{ mounts.host_storage_group }}"
inventories/srack/sg/dce-prd1/group_vars/all.yml:8:apps_root: /opt/apps
inventories/srack/sg/dce-prd1/group_vars/all.yml:10:spare_apps_root: /app
inventories/srack/sg/dce-prd1/group_vars/all.yml:14:podman_alternative_home: "{{ spare_apps_root }}/solace/podman"
inventories/srack/sg/dce-prd1/group_vars/all.yml:28:        mount_path: "{{ apps_root }}"
inventories/srack/sg/dce-prd1/group_vars/all.yml:117:  host_storage_group: "{{ apps_root }}/solace/storage-group"
inventories/srack/sg/dce-prd1/group_vars/all.yml:118:  container_storage_group: /var/lib/solace
inventories/srack/sg/dce-prd1/group_vars/all.yml:120:  host_secrets: /home/solace/secrets
inventories/srack/sg/dce-prd1/group_vars/all.yml:121:  container_secrets: /run/secrets
inventories/srack/sg/dce-prd1/group_vars/all.yml:373:  app_path: "{{ apps_root }}/solace/java"
inventories/srack/sg/dce-prd1/group_vars/all.yml:390:  log_dir: "{{ spare_apps_root }}/solace/logs/noformat-logs/{{ solace_container_name }}"
inventories/srack/sg/dce-prd1/group_vars/all.yml:391:  log_path: "{{ spare_apps_root }}/solace/logs/noformat-logs/{{ solace_container_name }}/{{ solace_container_name }}.log"
roles/solace_scripts_create_and_set_server_certs/defaults/main.yml:3:app_dir: "{{ apps_root }}"
roles/solace_scripts_generate_certs/defaults/main.yml:3:app_dir: "{{ apps_root }}"

# 2. how does podman-run set --user and the volume mounts in its env/args?
grep -rn 'PSP_USER_ID\|PSP_MOUNT\|--user\|--volume\|-v \|:/var/lib/solace\|mounts\.' \
  roles/solace_systemd/templates/ 2>/dev/null | grep -iv '#'
appadmintemp@cnlvatfss003[DEV][51080-solace-pubsubplus-podman-jgd] $ grep -rn 'PSP_USER_ID\|PSP_MOUNT\|--user\|--volume\|-v \|:/var/lib/solace\|mounts\.' \
  roles/solace_systemd/templates/ 2>/dev/null | grep -iv '#'
roles/solace_systemd/templates/solace.conf.j2:100:PSP_MOUNT=--mount=type=bind,source={{ mounts.host_storage_group }},destination={{ mounts.container_storage_group }},relabel=private,ro=false
roles/solace_systemd/templates/solace.conf.j2:101:PSP_MOUNT_SECRETS=--mount=type=bind,source={{ mounts.host_secrets }},destination={{ mounts.container_secrets }},relabel=private,ro=false
roles/solace_systemd/templates/solace.conf.j2:102:PSP_MOUNT_SOLACE=--mount=type=bind,source={{ mounts.host_mnt_solace }},destination={{ mounts.container_mnt_solace }},relabel=private,ro=false
roles/solace_systemd/templates/solace.conf.j2:103:{% if mounts.host_etc_hosts is defined %}
roles/solace_systemd/templates/solace.conf.j2:104:PSP_MOUNT_HOSTS=--mount=type=bind,source={{ mounts.host_etc_hosts }}/hosts,destination={{ mounts.container_etc_hosts }},relabel=private,ro=false
roles/solace_systemd/templates/solace.env.j2:10:tls_servercertificate_filepath={{ mounts.container_secrets }}/server.cert
roles/solace_systemd/templates/solace.env.j2:20:username_admin_passwordfilepath={{ mounts.container_secrets }}/username_admin_password
roles/solace_systemd/templates/solace.env.j2:104:redundancy_authentication_presharedkey_keyfilepath={{ mounts.container_secrets }}/solace-preshared-key.conf
roles/solace_systemd/templates/solace.service.j2:17:RequiresMountsFor={{ mounts.host_storage_group }}
roles/solace_systemd/templates/solace.service.j2:36:Environment=PSP_MOUNT="--env=process=podman"
roles/solace_systemd/templates/solace.service.j2:37:Environment=PSP_MOUNT_SECRETS="--env=process=podman"
roles/solace_systemd/templates/solace.service.j2:38:Environment=PSP_MOUNT_SOLACE="--env=process=podman"
roles/solace_systemd/templates/solace.service.j2:39:Environment=PSP_MOUNT_HOSTS="--env=process=podman"
roles/solace_systemd/templates/solace.service.j2:116:ExecStartPre=/bin/bash -c 'echo PSP_USER_ID=$(id -u {{ solace_local_service_user }}) > /home/{{ solace_local_service_user }}/etc_solace/solace-user.conf'
roles/solace_systemd/templates/solace.service.j2:117:ExecStart=/usr/bin/podman run --env-file=/home/{{ solace_local_service_user }}/etc_solace/solace.env --user ${PSP_USER_ID} --cidfile=%t/%N.cid \
roles/solace_systemd/templates/solace.service.j2:132:  ${PSP_MOUNT} \
roles/solace_systemd/templates/solace.service.j2:133:  ${PSP_MOUNT_SECRETS} \
roles/solace_systemd/templates/solace.service.j2:134:  ${PSP_MOUNT_SOLACE} \
roles/solace_systemd/templates/solace.service.j2:135:  ${PSP_MOUNT_HOSTS} \
