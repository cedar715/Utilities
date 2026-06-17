ls -lan /home/solace/secrets
ls -ld /home/solace/secrets

  # move the old one aside, make a fresh solace-owned dir
podman unshare rm -rf /home/solace/secrets 2>/dev/null
mkdir -p /home/solace/secrets
ls -ld /home/solace/secrets    # should now be solace:solace
echo -n '<REAL-ADMIN-PASSWORD>' > /home/solace/secrets/username_admin_password
echo -n 'JyHWzW46x/1ZluaFDYWqAXc/DI5G/XZn8CAJ3zuY7EQs' > /home/solace/secrets/solace-preshared-key.conf
chmod 600 /home/solace/secrets/*
ls -lan /home/solace/secrets    # confirm files exist, owned by solace (5011)
