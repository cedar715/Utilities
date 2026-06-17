# on a podman-run prod box
ls -lan /home/solace/solace/storage-group 2>/dev/null | head -3
# or wherever prod mounts /var/lib/solace from — check the compose/run mount source
