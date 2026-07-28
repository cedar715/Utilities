# 1. Counter resets = phantom increases. Any nonzero value here is your smoking gun.
resets(sol_event_system_ha_redun_group_node_left_total[1d])

# 2. Real event count vs. what increase() reported. Divergence = extrapolation/reset artifact.
changes(sol_event_system_ha_redun_group_node_left_total[1d])
increase(sol_event_system_ha_redun_group_node_left_total[1d])

# 3. Series that can NEVER be suppressed by your `unless`.
count by (host, queue, vmrHostAlias, vpn) (sol_event_system_ha_redun_group_node_left_total)
unless
count by (host, queue, vmrHostAlias, vpn) (sol_event_system_ha_redun_group_node_joined_total)

# 4. Scrape gaps (staleness can also fake a reset)
up{job=~".*solace.*"}
---
sum by (host, vmrHostAlias) (sol_event_system_ha_redun_group_node_left_total)
-
sum by (host, vmrHostAlias) (sol_event_system_ha_redun_group_node_joined_total)

  sol_event_system_ha_redun_group_node_left_total
                             sol_event_system_ha_redun_group_node_joined_total

  resets(sol_event_system_ha_redun_group_node_left_total[7d])
