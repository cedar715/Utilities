solace@cnlvatfss003[DEV][~] $ systemctl --user start solace.service
solace@cnlvatfss003[DEV][~] $ systemctl --user status solace.service --no-pager | grep Active:
     Active: inactive (dead) since Wed 2026-06-17 14:41:59 CST; 7s ago
solace@cnlvatfss003[DEV][~] $ podman ps --format '{{.Names}} {{.Status}}'
solace@cnlvatfss003[DEV][~] $ podman logs solace 2>&1 | tail -20
Error: no container with name or ID "solace" found: no such container
solace@cnlvatfss003[DEV][~] $ 

