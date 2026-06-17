# on VM3 — run foreground and capture EVERYTHING, not just the tail
cd /home/solace/solace
/home/solace/.local/bin/podman-compose -f solace-compose.yml up 2>&1 | tee /tmp/monitor-boot.log
# let it die (~47s), Ctrl-C, then:
grep -iE 'error|fatal|fail|abort|exit|denied|violation|cannot' /tmp/monitor-boot.log
