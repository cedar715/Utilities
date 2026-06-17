grep -rn 'connectvia\|connect_via\|redundancy_group_node\|cnlvatfss\|cdc-dev1' \
  group_vars/ inventories/ roles/solace_systemd/ 2>/dev/null | grep -iv '#' | head -20
