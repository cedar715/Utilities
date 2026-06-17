
# is it up now, or exited?
systemctl --user status solace.service --no-pager | grep -E 'Active:|Main PID:'
podman ps --format '{{.Names}} {{.Status}}'

# the FULL tail — what's the LAST thing before it stops (past the lttng lines)?
podman logs solace 2>&1 | tail -40
