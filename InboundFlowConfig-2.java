mkdir -p /home/solace/.config/systemd/user

cat > /home/solace/.config/systemd/user/solace.service <<'EOF'
[Unit]
Description=Solace PubSub+ broker (rootless podman-compose)
After=default.target

[Service]
Type=simple
WorkingDirectory=/home/solace/solace
ExecStartPre=-/home/solace/.local/bin/podman-compose -f /home/solace/solace/solace-compose.yml down --timeout 60
ExecStart=/home/solace/.local/bin/podman-compose -f /home/solace/solace/solace-compose.yml up
ExecStop=/home/solace/.local/bin/podman-compose -f /home/solace/solace/solace-compose.yml down --timeout 120
Restart=on-failure
RestartSec=10
TimeoutStartSec=600
TimeoutStopSec=120

[Install]
WantedBy=default.target
EOF

  ---
  # make sure your session can reach the user manager (the XDG vars)
export XDG_RUNTIME_DIR=/run/user/5011
export DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/5011/bus

systemctl --user daemon-reload
systemctl --user enable solace.service       # enable (don't start yet — HA bring-up order matters)
systemctl --user status solace.service       # should show "loaded ... inactive (dead)" — good, not started
