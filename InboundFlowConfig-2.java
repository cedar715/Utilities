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
