appadmintemp@cnlvatfss003[DEV][51080-solace-pubsubplus-podman-jgd] $ grep -rn 'LimitNOFILE\|nofile\|delegate.conf\|1048576\|524288' \
  roles/admin_solace_config_system_enablement_post_user_creation/ \
  roles/admin_solace_config_system_enablement/ 2>/dev/null | grep -iv '#'
roles/admin_solace_config_system_enablement_post_user_creation/tasks/main.yml:1:- name: In order to override cpu and mem create file /etc/systemd/system/user@id.service.d/delegate.conf
roles/admin_solace_config_system_enablement_post_user_creation/tasks/main.yml:4:    cat <<-EOF > /etc/systemd/system/user@$(id -u {{ solace_local_service_user }}).service.d/delegate.conf
roles/admin_solace_config_system_enablement_post_user_creation/tasks/main.yml:28:    LimitNOFILE=1048576
roles/admin_solace_config_system_enablement/tasks/main.yml:26:      {{ solace_local_service_user }} hard nproc 1048576
roles/admin_solace_config_system_enablement/tasks/main.yml:27:      {{ solace_local_service_user }} soft nproc 1048576
roles/admin_solace_config_system_enablement/tasks/main.yml:28:      {{ solace_local_service_user }} hard nofile 1048576
roles/admin_solace_config_system_enablement/tasks/main.yml:29:      {{ solace_local_service_user }} soft nofile 1048576
