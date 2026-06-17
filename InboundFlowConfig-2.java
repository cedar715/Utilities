grep -rn 'LimitNOFILE\|nofile\|delegate.conf\|1048576\|524288' \
  roles/admin_solace_config_system_enablement_post_user_creation/ \
  roles/admin_solace_config_system_enablement/ 2>/dev/null | grep -iv '#'
