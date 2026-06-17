# 1. is the graphroot on a noexec mount?
findmnt -T /opt/apps/solace/podman/graphroot -o TARGET,SOURCE,FSTYPE,OPTIONS

# 2. SELinux mode + the graphroot label (you relabeled this earlier — confirm it held)
getenforce
sudo ls -laZd /opt/apps/solace/podman/graphroot

  And as appadmintemp:
bash# 3. any SELinux denial in the last few minutes?
sudo ausearch -m avc -ts recent 2>/dev/null | tail -30
