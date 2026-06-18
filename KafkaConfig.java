#!/usr/bin/env bash
# backup-solace-node.sh — run as solace on each node; sudo parts noted
set -uo pipefail
TS=$(date +%Y%m%d-%H%M%S)
DEST=~/solace-backup-$(hostname)-$TS
mkdir -p "$DEST"/{compose,systemd,containers,host}

cp /home/solace/solace/solace-compose.yml                    "$DEST/compose/"          2>/dev/null
cp /home/solace/.config/systemd/user/solace.service          "$DEST/systemd/"          2>/dev/null
cp /home/solace/.config/containers/storage.conf              "$DEST/containers/"       2>/dev/null
# secrets: record metadata, copy cert (NOT the password/PSK in cleartext into a backup you might share)
ls -l /home/solace/secrets/                                > "$DEST/secrets-listing.txt"
cp /home/solace/secrets/server.cert                          "$DEST/"                  2>/dev/null

echo "Backed up to $DEST"
tar czf "$DEST.tar.gz" -C "$(dirname "$DEST")" "$(basename "$DEST")"
echo "Archive: $DEST.tar.gz"
