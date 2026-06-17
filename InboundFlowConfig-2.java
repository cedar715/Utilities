# VM1 (primary) — start
systemctl --user start solace.service

# VM2 (backup) — start (within a few seconds of VM1)
systemctl --user start solace.service

# wait ~60s for primary+backup to find each other and establish the mate link
# THEN VM3 (monitor)
systemctl --user start solace.service
