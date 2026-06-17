# rootful containers/pods/images from the old deployment
sudo podman ps -a
sudo podman rm -af
sudo podman pod rm -af

# was there a rootful SYSTEM unit? (vs the rootless user unit we want)
sudo systemctl list-unit-files | grep -i solace
sudo systemctl disable --now solace 2>/dev/null || echo "no system unit"
ls -l /etc/systemd/system/*solace* 2>/dev/null || echo "no system unit file"

# rootful storage (owned by root, solace can't rm it)
sudo ls -ld /opt/apps/solace/storage-group 2>/dev/null
sudo rm -rf /opt/apps/solace/storage-group
