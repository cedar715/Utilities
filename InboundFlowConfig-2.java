podman logs solace 2>&1 | tail -40
journalctl --user -u solace.service --no-pager -n 30
