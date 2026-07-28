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
