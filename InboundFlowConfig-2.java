# as appadmintemp
sudo systemctl status solace-compose.service 2>&1 | head -3
ls -l /etc/systemd/system/*solace* 2>/dev/null || echo "no system solace units — good"
