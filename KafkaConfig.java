max by (host, vmrHostAlias) (
  max_over_time(
    (timestamp(
      ((sol_event_system_ha_redun_group_node_left_total
        - sol_event_system_ha_redun_group_node_left_total offset 10m) > 0)
      or
      (sol_event_system_ha_redun_group_node_left_total
       unless (sol_event_system_ha_redun_group_node_left_total offset 10m))
    ))[1d:5m]
  )
)

  max by (host, vmrHostAlias) (
  max_over_time(
    (timestamp(
      ((sol_event_system_ha_redun_group_node_joined_total
        - sol_event_system_ha_redun_group_node_joined_total offset 10m) > 0)
      or
      (sol_event_system_ha_redun_group_node_joined_total
       unless (sol_event_system_ha_redun_group_node_joined_total offset 10m))
    ))[1d:5m]
  )
)
or
max by (host, vmrHostAlias) (sol_event_system_ha_redun_group_node_left_total) * 0
