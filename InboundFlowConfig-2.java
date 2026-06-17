# VM1 (primary) — start
systemctl --user start solace.service

# VM2 (backup) — start (within a few seconds of VM1)
systemctl --user start solace.service

# wait ~60s for primary+backup to find each other and establish the mate link
# THEN VM3 (monitor)
systemctl --user start solace.service

  ---
  systemctl --user status solace.service --no-pager | grep -E 'Active:|Main PID:'
podman ps --format '{{.Names}} {{.Status}}'          # solace = Up, not Created/restarting
podman logs solace 2>&1 | grep -iE 'platform type|pre-startup'   # [ OK ] lines
