sudo ls -l /etc/systemd/system/user@.service.d/
sudo cat /etc/systemd/system/user@.service.d/*.conf


  sudo mkdir -p /etc/systemd/system/user@.service.d
sudo tee /etc/systemd/system/user@.service.d/solace-limits.conf >/dev/null <<'EOF'
[Service]
LimitNOFILE=1048576
LimitMEMLOCK=infinity
LimitCORE=infinity
EOF
sudo systemctl daemon-reload
sudo systemctl restart user@5011.service     # drops solace session if active — fine, nothing deployed
# verify
systemctl show user@5011.service -p LimitNOFILE   # must now read 1048576
