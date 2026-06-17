sudo tee /etc/systemd/system/user@5011.service.d/delegate.conf >/dev/null <<'EOF'
[Service]
Delegate=memory pids cpu cpuset
LimitNOFILE=1048576
LimitMEMLOCK=infinity
LimitCORE=infinity
EOF
sudo systemctl daemon-reload
sudo systemctl restart user@5011.service
