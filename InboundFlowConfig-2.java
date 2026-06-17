# 1. where are the mount vars + user DEFINED? (so compose reuses, not redefines)
grep -rn 'host_storage_group\|container_storage_group\|apps_root\|host_secrets\|container_secrets' \
  group_vars/ inventories/ roles/*/defaults/ roles/*/vars/ 2>/dev/null | grep -iv '#'

# 2. how does podman-run set --user and the volume mounts in its env/args?
grep -rn 'PSP_USER_ID\|PSP_MOUNT\|--user\|--volume\|-v \|:/var/lib/solace\|mounts\.' \
  roles/solace_systemd/templates/ 2>/dev/null | grep -iv '#'
