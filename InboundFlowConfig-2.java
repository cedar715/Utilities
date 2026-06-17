# what does id resolve to (the --user value podman-run passes)?
id -u solace; id -g solace          # expect 5011 / 5011
solace@myvm[~] $ id -u solace; id -g solace
5011
5011
  
# confirm the storage var path prod actually mounts
grep -rn 'host_storage_group\|apps_root\|/var/lib/solace' roles/solace_runtime_deploy/ group_vars/ inventories/ | grep -iv '#' | head
appadmintemp@myvm3[DEV][51080-solace-pubsubplus-podman-jgd] $ grep -rn 'host_storage_group\|apps_root\|/var/lib/solace' roles/solace_runtime_deploy/ group_vars/ inventories/ | grep -iv '#' | head
roles/solace_runtime_deploy/tasks/main.yml:34:    - "{{ mounts.host_storage_group }}"
