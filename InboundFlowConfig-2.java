# on VM3 — run foreground and capture EVERYTHING, not just the tail
cd /home/solace/solace
/home/solace/.local/bin/podman-compose -f solace-compose.yml up 2>&1 | tee /tmp/monitor-boot.log
# let it die (~47s), Ctrl-C, then:
grep -iE 'error|fatal|fail|abort|exit|denied|violation|cannot' /tmp/monitor-boot.log


  # on VM3 — can the monitor reach primary/backup consul ports?
nc -zv 10.27.245.146 8300 2>&1    # primary
nc -zv 10.27.245.147 8300 2>&1    # backup
