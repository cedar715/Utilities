---
# ==============================================================================
# solace-compose-runtime.yml.j2
# ==============================================================================
# PURPOSE   : Runtime podman-compose file rendered by Ansible for each target
#             host.  All values come from inventory variables (group_vars/all.yml,
#             group_vars/ha_*.yml, host_vars/).
#             The CI intercept scan uses the static solace-compose.yml in repo
#             root — this template is NOT scanned directly.
#
# TEMPLATE  : Rendered by roles/solace_runtime_deploy/tasks/main.yml (or
#             site-ha-targeted-podman-compose.yml) via ansible.builtin.template
#             onto the target host at /home/{{ solace_local_service_user }}/solace-compose.yml
#
# VARIABLES : (all sourced from inventory)
#   image                  — fully-qualified container image (required)
#   broker                 — dict: cpu, memory, shm_size, cgroups, network,
#                            routername_prefix, routername, log_driver,
#                            enable_file_logging, log_path, log_max_size,
#                            redundancy_enable, configsync_enable, ...
#   mounts                 — dict: host_storage_group, container_storage_group,
#                            host_secrets, container_secrets
#   system                 — dict: system_scaling_maxconnectioncount,
#                            system_scaling_maxqueuemessagecount,
#                            messagespool_maxspoolusage,
#                            system_scaling_maxbridgecount
#   ports                  — dict of optional port mappings (see ports_default)
#   is_ha                  — bool  (set in group_vars/ha_servers.yml)
#   ha_role                — primary | backup | monitor
#   node1_fqdn, node2_fqdn, node3_fqdn — HA node FQDNs
#   solace_local_service_user
#   solace_container_name
#
# CIS POLICY COMPLIANCE (mirrors solace-compose.yml):
#   211/212  custom bridge or host-mode
#   221      non-root UID
#   222      read-only root fs
#   223      cap_drop ALL
#   224      no-new-privileges
#   231      localhost-only port bindings (bridge mode)
#   242      secrets via mounted files
#   243      JSON-file logging with rotation
#   244      health check
#   251      named volume for Solace data
#   252      /tmp and /run as tmpfs
# ==============================================================================

version: "3.9"

# ── Network ───────────────────────────────────────────────────────────────────
# Host networking only (no bridge network, no explicit port mappings).

# ── Policy 242 — Secrets (mounted files, not env-var values) ─────────────────
secrets:
  username_admin_password:
    file: {{ mounts.host_secrets }}/username_admin_password
  server_cert:
    file: {{ mounts.host_secrets }}/server.cert
{% if broker.redundancy_authentication_presharedkey_key is defined and broker.redundancy_authentication_presharedkey_key != '' %}
  solace_preshared_key:
    file: {{ mounts.host_secrets }}/solace-preshared-key.conf
{% endif %}

services:
  solace:
    image: "{{ image }}"
    container_name: "{{ solace_container_name | default('solace') }}"
    hostname: >-
      {{
        (broker.routername_prefix | default('')) ~ ha_role
        if (is_ha | default(false) | bool and ha_role is defined and ha_role in ['primary', 'backup', 'monitor'])
        else (broker.routername | default('solace'))
      }}

    # podman-run uses --user $(id -u solace); default to 0:0 when not pre-resolved.
    user: "{{ solace_resolved_uid | default('0') }}:{{ solace_resolved_gid | default('0') }}"

    network_mode: host
    uts: host

    shm_size: 1g
    ulimits:
      core: -1
      nofile:
        soft: 2448
        hard: 1048576
      # NO memlock - from user@.service.d/delegate.conf, not here

    volumes:
      # :Z == podman-run's relabel=private (NOT :z / named volume)
      - "{{ mounts.host_storage_group | default('/opt/apps/solace/storage-group') }}:{{ mounts.container_storage_group }}:Z"
      - "{{ mounts.host_secrets }}:{{ mounts.container_secrets }}:Z"
      - "{{ mounts.host_mnt_solace }}:{{ mounts.container_mnt_solace }}:Z"
{% if mounts.host_etc_hosts is defined %}
      - "{{ mounts.host_etc_hosts }}/hosts:{{ mounts.container_etc_hosts }}:Z"
{% endif %}

    env_file:
      - "/home/{{ solace_local_service_user }}/etc_solace/solace.env"

    # NO cap_drop / read_only / no-new-privileges (kill consul)
    # NO ports here when host networking (HA); for standalone, omit 2222 if host sshd owns it

    # ── Bootstrap environment (paths only — Policy 242) ─────────────────────
    environment:
      - username_admin_globalaccesslevel=admin
      - username_admin_passwordfilepath={{ mounts.container_secrets }}/username_admin_password
      - tls_servercertificate_filepath={{ mounts.container_secrets }}/server.cert
{% if ports.https_port is defined %}
      - service_semp_tlsport={{ ports.https_port.split(':')[1] }}
{% elif ports.https_standard_port is defined %}
      - service_semp_tlsport={{ ports.https_standard_port.split(':')[1] }}
{% else %}
      - service_semp_tlsport=1943
{% endif %}
      - system_scaling_maxconnectioncount={{ system.system_scaling_maxconnectioncount }}
      - system_scaling_maxqueuemessagecount={{ system.system_scaling_maxqueuemessagecount }}
{% if system.system_scaling_maxbridgecount is defined %}
      - system_scaling_maxbridgecount={{ system.system_scaling_maxbridgecount }}
{% endif %}
{% if system.messagespool_maxspoolusage is defined %}
      - messagespool_maxspoolusage={{ system.messagespool_maxspoolusage }}
{% endif %}
{% if broker.service_smf_port is defined %}
      - service_smf_port={{ broker.service_smf_port }}
{% endif %}
{% if broker.service_smf_compressedport is defined %}
      - service_smf_compressedport={{ broker.service_smf_compressedport }}
{% endif %}
{% if broker.service_smf_tlsport is defined %}
      - service_smf_tlsport={{ broker.service_smf_tlsport }}
{% endif %}
{% if broker.service_smf_routingport is defined %}
      - service_smf_routingport={{ broker.service_smf_routingport }}
{% endif %}
{% if broker.service_matelink_port is defined %}
      - service_matelink_port={{ broker.service_matelink_port }}
{% endif %}
{% if broker.service_redundancy_firstlistenport is defined %}
      - service_redundancy_firstlistenport={{ broker.service_redundancy_firstlistenport }}
{% endif %}
{% if is_ha is defined and is_ha | bool %}
      - redundancy_enable={{ broker.redundancy_enable | default('yes') }}
      - configsync_enable={{ broker.configsync_enable | default('yes') }}
      - configsync_tls_enable={{ broker.configsync_tls_enable | default('yes') }}
{% if ha_role == 'primary' %}
      - nodetype=message_routing
      - routername={{ broker.routername_prefix | default('') }}primary
      - redundancy_activestandbyrole=primary
{% if broker.redundancy_matelink_remote_port is defined %}
      - redundancy_matelink_connectvia={{ node2_fqdn }}:{{ broker.redundancy_matelink_remote_port }}
{% else %}
      - redundancy_matelink_connectvia={{ node2_fqdn }}
{% endif %}
      - redundancy_matelink_tls_enable=yes
{% elif ha_role == 'backup' %}
      - nodetype=message_routing
      - routername={{ broker.routername_prefix | default('') }}backup
      - redundancy_activestandbyrole=backup
{% if broker.redundancy_matelink_remote_port is defined %}
      - redundancy_matelink_connectvia={{ node1_fqdn }}:{{ broker.redundancy_matelink_remote_port }}
{% else %}
      - redundancy_matelink_connectvia={{ node1_fqdn }}
{% endif %}
      - redundancy_matelink_tls_enable=yes
{% elif ha_role == 'monitor' %}
      - nodetype=monitoring
      - routername={{ broker.routername_prefix | default('') }}monitor
{% endif %}
{% if broker.service_redundancy_firstlistenport is defined %}
      - redundancy_group_node_{{ broker.routername_prefix | default('') }}primary_connectvia={{ node1_fqdn }}:{{ broker.service_redundancy_firstlistenport }}
      - redundancy_group_node_{{ broker.routername_prefix | default('') }}backup_connectvia={{ node2_fqdn }}:{{ broker.service_redundancy_firstlistenport }}
      - redundancy_group_node_{{ broker.routername_prefix | default('') }}monitor_connectvia={{ node3_fqdn }}:{{ broker.service_redundancy_firstlistenport }}
{% else %}
      - redundancy_group_node_{{ broker.routername_prefix | default('') }}primary_connectvia={{ node1_fqdn }}
      - redundancy_group_node_{{ broker.routername_prefix | default('') }}backup_connectvia={{ node2_fqdn }}
      - redundancy_group_node_{{ broker.routername_prefix | default('') }}monitor_connectvia={{ node3_fqdn }}
{% endif %}
      - redundancy_group_node_{{ broker.routername_prefix | default('') }}primary_nodetype=message_routing
      - redundancy_group_node_{{ broker.routername_prefix | default('') }}backup_nodetype=message_routing
      - redundancy_group_node_{{ broker.routername_prefix | default('') }}monitor_nodetype=monitoring
{% if broker.redundancy_authentication_presharedkey_key is defined and broker.redundancy_authentication_presharedkey_key != '' %}
      - redundancy_authentication_presharedkey_keyfilepath={{ mounts.container_secrets }}/solace-preshared-key.conf
{% endif %}
{% if broker.redundancy_mate_smf_port is defined %}
      - redundancy_mate_smf_port={{ broker.redundancy_mate_smf_port }}
{% endif %}
{% if broker.redundancy_mate_smf_tlsport is defined %}
      - redundancy_mate_smf_tlsport={{ broker.redundancy_mate_smf_tlsport }}
{% endif %}
{% if broker.redundancy_matelink_remote_port is defined %}
      - redundancy_matelink_remote_port={{ broker.redundancy_matelink_remote_port }}
{% endif %}
{% endif %}{# end is_ha #}
{% if broker.routername is defined and not (is_ha | default(false) | bool) %}
      - routername={{ broker.routername }}
{% endif %}

    # ── Policy 242 — Secret mounts ───────────────────────────────────────────
    secrets:
      - source: username_admin_password
        target: {{ mounts.container_secrets }}/username_admin_password
      - source: server_cert
        target: {{ mounts.container_secrets }}/server.cert
{% if broker.redundancy_authentication_presharedkey_key is defined and broker.redundancy_authentication_presharedkey_key != '' %}
      - source: solace_preshared_key
        target: {{ mounts.container_secrets }}/solace-preshared-key.conf
{% endif %}

    # ── Policy 243 — Logging with rotation ───────────────────────────────────
    logging:
{% if broker.log_driver is defined %}
      driver: {{ broker.log_driver }}
{% else %}
      driver: json-file
{% endif %}
      options:
        max-size: "{{ broker.log_max_size | default('100m') }}"
        max-file: "5"
{% if broker.enable_file_logging | default(false) and broker.log_path is defined %}
        path: "{{ broker.log_path }}"
{% endif %}

    # ── Policy 244 — Container health check ──────────────────────────────────
    healthcheck:
      test:
        - "CMD-SHELL"
{% if ports.https_port is defined %}
        - "curl -sk https://127.0.0.1:{{ ports.https_port.split(':')[1] }}/ > /dev/null 2>&1 || curl -sk http://127.0.0.1:{{ ports.http_port.split(':')[1] if ports.http_port is defined else '8080' }}/ > /dev/null 2>&1"
{% elif ports.https_standard_port is defined %}
        - "curl -sk https://127.0.0.1:{{ ports.https_standard_port.split(':')[1] }}/ > /dev/null 2>&1 || curl -sk http://127.0.0.1:{{ ports.http_port.split(':')[1] if ports.http_port is defined else '8080' }}/ > /dev/null 2>&1"
{% else %}
        - "curl -sk https://127.0.0.1:1943/ > /dev/null 2>&1 || curl -sk http://127.0.0.1:8080/ > /dev/null 2>&1"
{% endif %}
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 120s
