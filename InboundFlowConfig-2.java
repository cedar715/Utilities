# re-add the file-context equivalence rule (harmless if already present) and relabel RECURSIVELY
sudo semanage fcontext -a -e /var/lib/containers /opt/apps/solace/podman/graphroot 2>/dev/null
sudo restorecon -R -v /opt/apps/solace/podman/graphroot

# verify it's now container_var_lib_t
sudo ls -laZd /opt/apps/solace/podman/graphroot


  # back as solace on VM3
systemctl --user start solace.service
sleep 20
podman ps --format '{{.Names}} {{.Status}}'           # solace = Up now
podman logs solace 2>&1 | grep -iE 'platform type|pre-startup'   # [ OK ] lines


  sudo semanage fcontext -l | grep '/opt/apps/solace/podman/graphroot'
