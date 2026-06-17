# 1. is firewalld even running/enforcing on this host?
sudo systemctl is-active firewalld
#   active   = firewalld enforces, rules matter
#   inactive = firewall is off; ports are open by default, nothing to add

# 2. if active — what's already allowed?
sudo firewall-cmd --list-all
sudo firewall-cmd --list-rich-rules
sudo firewall-cmd --list-ports
