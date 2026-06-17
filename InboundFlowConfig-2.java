. Ctrl-C the hanging systemctl stop (it's safe — it just stops waiting, doesn't kill anything mid-operation).
2. Force-kill the unit instead of graceful stop:
bashsudo systemctl kill --signal=SIGKILL solace-compose.service
sudo systemctl reset-failed solace-compose.service 2>/dev/null
3. If it's still "active", disable + mask the ExecStop path and stop again — but the cleaner route is to just disable it and kill the leftover process directly:
bashsudo systemctl disable solace-compose.service
sudo rm -f /etc/systemd/system/solace-compose.service
sudo systemctl daemon-reload
sudo systemctl reset-failed solace-compose.service 2>/dev/null
4. Kill the lingering conmon directly (the actual container helper that's still alive — pid from your earlier status was the conmon for solace_solace_1):
bashsudo pkill -9 -f 'conmon.*solace_solace_1'
# confirm it's gone
ps -ef | grep -i 'conmon.*solace' | grep -v grep || echo "conmon gone"
5. Then clean the container/storage as solace (the real owner — this is where it actually lives):
bash# su to solace, set XDG, then:
podman rm -af
podman pod rm -af
podman unshare rm -rf /opt/apps/solace/storage-group
