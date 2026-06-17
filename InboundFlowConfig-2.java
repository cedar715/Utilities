nodetype: monitoring

  systemctl --user restart solace.service
sleep 30
podman ps --format '{{.Names}} {{.Status}}'
ss -tlnp 2>/dev/null | grep -E '8300|8301|8302'   # consul should now be listening
