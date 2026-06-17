sudo semanage fcontext -a -e /var/lib/containers /opt/apps/solace/podman/graphroot
sudo restorecon -R -v /opt/apps/solace/podman/graphroot
# verify — should now be container_var_lib_t, not unlabeled_t
sudo ls -laZd /opt/apps/solace/podman/graphroot
