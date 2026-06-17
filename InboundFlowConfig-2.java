# on EACH of VM1, VM2, VM3 — as solace
systemctl --user stop solace.service 2>/dev/null
podman unshare rm -rf /opt/apps/solace/storage-group /home/solace/solace/storage-group 2>/dev/null
mkdir -p /opt/apps/solace/storage-group
podman unshare chown 0:0 /opt/apps/solace/storage-group     # 0:0 = host solace 5011
podman unshare ls -lan /opt/apps/solace/storage-group        # total 0, owner 0 0
