# inventories/nonp/cn/cdc-dev1/group_vars/ha_servers.yml
# Values shared by ALL THREE HA nodes. The redundancy GROUP topology is
# identical on every node — each broker must know all three members.

# ---- networking ----
# Multi-host HA: use host networking. Rootless bridge would NAT inter-broker
# source IPs (pasta/slirp4netns) and confuse consul/redundancy. Host net avoids it.
solace_network_mode: host       # template -> network_mode: host + uts: host; drops ports:

# ---- redundancy group (same on all three) ----
redundancy_enable: "yes"

# Router names derived from the prefix
router_primary: "{{ routername_prefix }}primary"   # cncdcdev1primary
router_backup:  "{{ routername_prefix }}backup"    # cncdcdev1backup
router_monitor: "{{ routername_prefix }}monitor"   # cncdcdev1monitor

# Full group topology. The compose template loops this into:
#   redundancy_group_node_<name>_connectvia / _nodetype
redundancy_group:
  - name: "{{ router_primary }}"
    nodetype: message_routing
    connectvia: "{{ node1_fqdn }}"
  - name: "{{ router_backup }}"
    nodetype: message_routing
    connectvia: "{{ node2_fqdn }}"
  - name: "{{ router_monitor }}"
    nodetype: monitoring
    connectvia: "{{ node3_fqdn }}"

# Pre-shared key: >=44 chars, Base64, IDENTICAL on all three. VAULT THIS.
# generate once: openssl rand -base64 33
redundancy_presharedkey: "{{ vault_redundancy_presharedkey }}"

# ---- inter-node firewall (open between the three VMs) ----
#   8300/8301/8302 = config-sync/consul, 8741 = mate-link, 55555 = SMF
solace_ha_ports: [8300, 8301, 8302, 8741, 55555]
