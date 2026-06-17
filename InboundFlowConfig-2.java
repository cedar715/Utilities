# is it active/enabled? (don't delete a running unit blind)
sudo systemctl status solace-compose.service --no-pager | head -10
sudo systemctl is-enabled solace-compose.service 2>/dev/null

# what does it actually run? (confirm it's the old rootful attempt)
sudo grep -iE 'ExecStart|User=|WorkingDirectory|podman' /etc/systemd/system/solace-compose.service
  
