# on EACH node, as solace — the admin password + preshared key as files
mkdir -p /home/solace/secrets
echo -n '<REAL-ADMIN-PASSWORD>' > /home/solace/secrets/username_admin_password
echo -n '' > /home/solace/secrets/solace-preshared-key.conf
chmod 600 /home/solace/secrets/*
