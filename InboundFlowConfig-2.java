grep -rn 'solace_local_service_user_uid\|solace_uid\|id -u' group_vars/ inventories/ roles/ | grep -iv '#' | head
appadmintemp@myvm3[DEV][51080-solace-pubsubplus-podman-jgd] $ grep -rn 'solace_local_service_user_uid\|solace_uid\|id -u' group_vars/ inventories/ roles/ | grep -iv '#' | head
roles/admin_asg_scale_in_setup/tasks/main.yml:34:    echo "export XDG_RUNTIME_DIR=/run/user/$(id -u)" > ~/.bashrc.d/systemd
roles/admin_files_directory_delete/tasks/main.yml:6:    rm -rf /etc/systemd/system/user@$(id -u {{ solace_local_service_user }}).service.d/delegate.conf
roles/admin_files_directory_delete_incl_storage/tasks/main.yml:8:    rm -rf /etc/systemd/system/user@$(id -u {{ solace_local_service_user }}).service.d/delegate.conf
roles/admin_reset_setup/tasks/main.yml:6:    rm -rf /etc/systemd/system/user@$(id -u {{ solace_local_service_user }}).service.d/delegate.conf
roles/admin_solace_config_system_enablement_post_user_creation/tasks/main.yml:3:    mkdir -p /etc/systemd/system/user@$(id -u {{ solace_local_service_user }}).service.d/
roles/admin_solace_config_system_enablement_post_user_creation/tasks/main.yml:4:    cat <<-EOF > /etc/systemd/system/user@$(id -u {{ solace_local_service_user }}).service.d/delegate.conf
roles/solace_assertion_target/tasks/main.yml:57:    stat /etc/systemd/system/user@$(id -u {{ solace_local_service_user }}).service.d/
roles/solace_assertion_target/tasks/main.yml:64:    success_msg: "Directory /etc/systemd/system/user@$(id -u {{ solace_local_service_user }}).service.d/ exists"
roles/solace_assertion_target/tasks/main.yml:65:    fail_msg: "Directory /etc/systemd/system/user@$(id -u {{ solace_local_service_user }}).service.d/ does not exist, admin playbook need to be executed!"
roles/solace_assertion_target/tasks/main.yml:96:    cat "/sys/fs/cgroup/user.slice/user-$(id -u {{ solace_local_service_user }}).slice/user@$(id -u {{ solace_local_service_user }}).service/cgroup.controllers"  | grep cpu | wc -l
