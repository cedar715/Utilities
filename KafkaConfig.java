max by (host, vmrHostAlias, vpn, queue) (
  max_over_time(
    (timestamp(
      ((sol_event_vpn_ad_msg_spool_quota_exceed_total
        - sol_event_vpn_ad_msg_spool_quota_exceed_total offset 10m) > 0)
      or
      (sol_event_vpn_ad_msg_spool_quota_exceed_total
       unless (sol_event_vpn_ad_msg_spool_quota_exceed_total offset 10m))
    ))[12h:1m]
  )
)
or
(
  max by (host, vmrHostAlias, vpn, queue) (sol_event_vpn_ad_msg_spool_quota_exceed_total)
  or
  max by (host, vmrHostAlias, vpn, queue) (sol_event_vpn_ad_msg_spool_high_clear_total)
) * 0

  ---

  max by (host, vmrHostAlias, vpn, queue) (
  max_over_time(
    (timestamp(
      ((sol_event_vpn_ad_msg_spool_high_clear_total
        - sol_event_vpn_ad_msg_spool_high_clear_total offset 10m) > 0)
      or
      (sol_event_vpn_ad_msg_spool_high_clear_total
       unless (sol_event_vpn_ad_msg_spool_high_clear_total offset 10m))
    ))[12h:1m]
  )
)
or
(
  max by (host, vmrHostAlias, vpn, queue) (sol_event_vpn_ad_msg_spool_quota_exceed_total)
  or
  max by (host, vmrHostAlias, vpn, queue) (sol_event_vpn_ad_msg_spool_high_clear_total)
) * 0
