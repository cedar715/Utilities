ppadmintemp@vm3[DEV][~] $ sudo systemctl status solace-compose.service --no-pager | head -10
● solace-compose.service - Solace PubSub+ Enterprise (podman-compose) — machine3
     Loaded: loaded (/etc/systemd/system/solace-compose.service; enabled; preset: disabled)
     Active: active (exited) since Tue 2026-06-16 02:35:15 CST; 1 day 8h ago
       Docs: https://docs.solace.com
   Main PID: 3917626 (code=exited, status=0/SUCCESS)
      Tasks: 1 (limit: 203462)
     Memory: 888.0K (peak: 30.6M)
        CPU: 1.382s
     CGroup: /system.slice/solace-compose.service
             └─3917725 /usr/bin/conmon --api-version 1 -c a113cd46ca22abdf74050fc87aee904564fc5fda151e0bd614cb80240599f4c9 -u a113cd46ca22abdf74050fc87aee904564fc5fda151e0bd614cb80240599f4c9 -r /usr/bin/crun -b /opt/apps/solace/podman/graphroot/overlay-containers/a113cd46ca22abdf74050fc87aee904564fc5fda151e0bd614cb80240599f4c9/userdata -p /opt/apps/solace/podman/runroot/overlay-containers/a113cd46ca22abdf74050fc87aee904564fc5fda151e0bd614cb80240599f4c9/userdata/pidfile -n solace_solace_1 --exit-dir /run/user/5011/libpod/tmp/exits --persist-dir /run/user/5011/libpod/tmp/persist/a113cd46ca22abdf74050fc87aee904564fc5fda151e0bd614cb80240599f4c9 --full-attach -s -l k8s-file:/app/solace/logs/noformat-logs/solace/solace.log --log-level warning --syslog --log-size-max 10000000 --runtime-arg --log-format=json --runtime-arg --log --runtime-arg=/opt/apps/solace/podman/runroot/overlay-containers/a113cd46ca22abdf74050fc87aee904564fc5fda151e0bd614cb80240599f4c9/userdata/oci-log --conmon-pidfile /opt/apps/solace/podman/runroot/overlay-containers/a113cd46ca22abdf74050fc87aee904564fc5fda151e0bd614cb80240599f4c9/userdata/conmon.pid --exit-command /usr/bin/podman --exit-command-arg --root --exit-command-arg /opt/apps/solace/podman/graphroot --exit-command-arg --runroot --exit-command-arg /opt/apps/solace/podman/runroot --exit-command-arg --log-level --exit-command-arg warning --exit-command-arg --cgroup-manager --exit-command-arg systemd --exit-command-arg --tmpdir --exit-command-arg /run/user/5011/libpod/tmp --exit-command-arg --network-config-dir --exit-command-arg "" --exit-command-arg --network-backend --exit-command-arg netavark --exit-command-arg --volumepath --exit-command-arg /opt/apps/solace/podman/graphroot/volumes --exit-command-arg --db-backend --exit-command-arg sqlite --exit-command-arg --transient-store=false --exit-command-arg --hooks-dir --exit-command-arg /usr/share/containers/oci/hooks.d --exit-command-arg --runtime --exit-command-arg crun --exit-command-arg --storage-driver --exit-command-arg overlay --exit-command-arg --events-backend --exit-command-arg journald --exit-command-arg container --exit-command-arg cleanup --exit-command-arg --stopped-only --exit-command-arg a113cd46ca22abdf74050fc87aee904564fc5fda151e0bd614cb80240599f4c9
appadmintemp@cnlvatfss003[DEV][~] $ sudo systemctl is-enabled solace-compose.service 2>/dev/null
enabled
appadmintemp@cnlvatfss003[DEV][~] $ sudo grep -iE 'ExecStart|User=|WorkingDirectory|podman' /etc/systemd/system/solace-compose.service
#   rootless podman needs cgroup controller delegation to spawn sub-processes
#   to the service, enabling full rootless podman even from a pipeline.
Description=Solace PubSub+ Enterprise (podman-compose) — machine3
# while our service is starting, causing 'podman ps' to exit 125.
User=solace
# Without Delegate=yes, rootless podman cannot create sub-cgroups and
# podman-compose derives the compose project name from the working directory
# Setting WorkingDirectory here makes ExecStartPre (down) and ExecStart (up)
WorkingDirectory=/home/solace
# PYTHONPATH: podman-compose is installed in solace user's local Python env.
# XDG_RUNTIME_DIR: rootless podman runtime directory for the solace user.
# HOME: ensure podman finds the correct user storage config (~/.config/containers)
ExecStartPre=/bin/bash -c 'mkdir -p /run/user/5011 && chown solace:solace /run/user/5011 && chmod 0700 /run/user/5011'
ExecStartPre=-/usr/local/bin/podman-compose-solace \
ExecStart=/usr/local/bin/podman-compose-solace \
    --podman-run-args '--user 0:0 --systemd=always --cgroupns=private --cgroups=enabled --sdnotify=ignore --security-opt label=disable --ulimit=core=-1 --ulimit=nofile=2448:1048576' \
ExecStop=/usr/local/bin/podman-compose-solace \
appadmintemp@cnlvatfss003[DEV][~] $ 
