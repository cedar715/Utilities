cat /home/solace/.config/containers/storage.conf | grep -iE 'graphroot|runroot|driver'
# expect: graphroot = /opt/apps/solace/podman/graphroot, driver = overlay


  # the storage-group candidates — see which exists on VM3
podman unshare ls -lan /opt/apps/solace/storage-group 2>/dev/null && echo "^ at /opt/apps"
podman unshare ls -lan /home/solace/solace/storage-group 2>/dev/null && echo "^ at ~/solace"
podman unshare ls -lan /app/solace/storage-group 2>/dev/null && echo "^ at /app"
