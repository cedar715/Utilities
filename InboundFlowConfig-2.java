grep -rn 'solace_local_service_user_uid\|solace_uid\|id -u' group_vars/ inventories/ roles/ | grep -iv '#' | head
