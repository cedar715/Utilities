podman logs solace 2>&1 | tail -40
journalctl --user -u solace.service --no-pager -n 30


  grep -nE 'activestandbyrole|matelink|configsync|nodetype|routername|redundancy' /home/solace/solace/solace-compose.yml
  # on each node
md5sum /home/solace/secrets/solace-preshared-key.conf 2>/dev/null || podman unshare md5sum /home/solace/secrets/solace-preshared-key.conf
