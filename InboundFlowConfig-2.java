# make secrets owned by container-0 (= host solace 5011), matching VM1 and the user:"0:0" model
podman unshare chown -R 0:0 /home/solace/secrets
podman unshare ls -lan /home/solace/secrets   # now owner 0 0
# then the direct write works (you're solace, dir is solace-owned now)
echo -n '<REAL-ADMIN-PASSWORD>' > /home/solace/secrets/username_admin_password
echo -n 'JyHWzW46x/1ZluaFDYWqAXc/DI5G/XZn8CAJ3zuY7EQs' > /home/solace/secrets/solace-preshared-key.conf
chmod 600 /home/solace/secrets/*
