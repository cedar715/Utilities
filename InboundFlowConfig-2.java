podman unshare ls -lan /home/solace/secrets

  podman unshare cat /home/solace/secrets/username_admin_password
podman unshare cat /home/solace/secrets/solace-preshared-key.conf 2>/dev/null || echo "no PSK file yet"
