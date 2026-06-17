sudo tee -a /etc/hosts >/dev/null <<'EOF'

# Solace HA redundancy group (cdc-dev1) — added for matelink/config-sync resolution
10.27.245.146 cdc-dev1-a.51080.cn.app.example.com
10.27.245.147 cdc-dev1-b.51080.cn.app.example.com
10.27.245.148 cdc-dev1-c.51080.cn.app.example.com
EOF
