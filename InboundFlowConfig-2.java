cat /home/solace/.config/containers/storage.conf | grep -iE 'graphroot|runroot|driver'
# expect: graphroot = /opt/apps/solace/podman/graphroot, driver = overlay
