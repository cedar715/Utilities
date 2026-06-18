#!/usr/bin/env bash
# backup-solace-host-files.sh — run as appadmintemp (uses sudo)
set -uo pipefail
TS=$(date +%Y%m%d-%H%M%S)
DEST=/tmp/solace-host-backup-$(hostname)-$TS
mkdir -p "$DEST"

sudo cp /etc/systemd/system/user@.service.d/delegate.conf  "$DEST/" 2>/dev/null
sudo cp /etc/sudoers.d/appadmintemp-solace                 "$DEST/" 2>/dev/null
sudo cp /etc/hosts                                          "$DEST/" 2>/dev/null

# SELinux fcontext rule isn't a file to copy — record it:
sudo semanage fcontext -l | grep graphroot                > "$DEST/selinux-fcontext-graphroot.txt" 2>/dev/null

# also record the live limit + label state for verification later
systemctl show "user@$(id -u solace).service" -p LimitNOFILE -p Delegate > "$DEST/user-manager-limits.txt"
sudo ls -laZd /opt/apps/solace/podman/graphroot           > "$DEST/graphroot-label.txt" 2>/dev/null

echo "Host-level files backed up to $DEST"
sudo chown -R appadmintemp:appadmintemp "$DEST" 2>/dev/null
tar czf "$DEST.tar.gz" -C /tmp "$(basename "$DEST")"
echo "Archive: $DEST.tar.gz"
