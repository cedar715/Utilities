# as appadmintemp
stat -fc %T /sys/fs/cgroup                                    # cgroup2fs
loginctl show-user solace -p Linger                          # Linger=yes (may need enabling)
systemctl show user@5011.service -p LimitNOFILE              # 1048576 (may need the drop-in)
grep solace /etc/subuid                                       # range exists
ls -laZd /opt/apps/solace/podman/graphroot 2>/dev/null       # container_var_lib_t (if relocated)
ss -tlnp | grep ':2222'                                       # does host sshd own 2222 here too?
