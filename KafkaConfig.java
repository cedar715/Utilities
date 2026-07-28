max by (host, vmrHostAlias, vpn, clientUsername) (
  max_over_time(
    (timestamp(
      ((sol_event_vpn_client_username_connections_exceeded_total
        - sol_event_vpn_client_username_connections_exceeded_total offset 10m) > 0)
      or
      (sol_event_vpn_client_username_connections_exceeded_total
       unless (sol_event_vpn_client_username_connections_exceeded_total offset 10m))
    ))[12h:1m]
  )
)
or
(
  max by (host, vmrHostAlias, vpn, clientUsername) (sol_event_vpn_client_username_connections_exceeded_total)
  or
  max by (host, vmrHostAlias, vpn, clientUsername) (sol_event_vpn_client_username_connections_high_clear_total)
) * 0

  ----

  max by (host, vmrHostAlias, vpn, clientUsername) (
  max_over_time(
    (timestamp(
      ((sol_event_vpn_client_username_connections_high_clear_total
        - sol_event_vpn_client_username_connections_high_clear_total offset 10m) > 0)
      or
      (sol_event_vpn_client_username_connections_high_clear_total
       unless (sol_event_vpn_client_username_connections_high_clear_total offset 10m))
    ))[12h:1m]
  )
)
or
(
  max by (host, vmrHostAlias, vpn, clientUsername) (sol_event_vpn_client_username_connections_exceeded_total)
  or
  max by (host, vmrHostAlias, vpn, clientUsername) (sol_event_vpn_client_username_connections_high_clear_total)
) * 0
