# on VM3 (monitor)
systemctl --user start solace.service
systemctl --user status solace.service --no-pager | grep Active:
podman ps --format '{{.Names}} {{.Status}}'
podman logs solace 2>&1 | tail -20
  
