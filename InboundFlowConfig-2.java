sudo cat /etc/systemd/system/solace-compose.service

     sudo systemctl stop solace-compose.service
sudo systemctl disable solace-compose.service
sudo rm -f /etc/systemd/system/solace-compose.service
sudo systemctl daemon-reload

# kill the lingering conmon + remove the old container/storage
sudo pkill -f 'conmon.*solace_solace_1' 2>/dev/null
# the container is in solace's rootless store at /opt/apps — clean as solace:
# (su to solace, then:) podman rm -af ; podman unshare rm -rf /opt/apps/solace/storage-group
