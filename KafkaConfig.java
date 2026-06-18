The order matters — these must all be true before podman-compose up. As Ansible tasks, in dependency order:
PROVISIONING PLAY (connect: appadmintemp, become: root)
  1. cgroup v2 active                    [assert; fail if not cgroup2fs]
  2. subuid/subgid range for solace      [≥65536 wide]
  3. linger enabled for solace           [loginctl enable-linger]
  4. user@<uid>.service.d/delegate.conf  [Delegate=... + LimitNOFILE=1048576 + LimitMEMLOCK=infinity + LimitCORE=infinity]
     → daemon-reload + restart user@<uid>.service
  5. sudoers grant appadmintemp→solace   [the Centrify workaround]
  6. graphroot exists + SELinux relabel  [semanage fcontext -e /var/lib/containers; restorecon -R]  ← MUST be recursive, and re-run after any podman reset
  7. /etc/hosts HA entries               [3 FQDN→IP lines]
  8. podman-compose installed for solace

DEPLOY PLAY (connect: appadmintemp, become_user: solace — works after #5)
  9. set_fact solace_uid = id -u solace  [for the compose user: line]
 10. ensure storage-group dir exists, owned 0:0 (via podman unshare)
 11. ensure secrets dir owned by solace + cert/password/PSK present
 12. assert substrate (the verify gates below) BEFORE starting
 13. template compose + systemd user unit
 14. systemctl --user daemon-reload + enable + start
 15. wait for SEMP 200 / show redundancy
The critical anti-loop insight: add an assertion task (step 12) that checks every substrate gate and fails loudly before podman-compose up runs. Your repo already has roles/solace_assertion_target — that's exactly the pattern. It should verify:
yaml# the substrate assertions that prevent the loop
- assert nofile ceiling = 1048576       (systemctl show user@<uid> -p LimitNOFILE)
- assert Delegate present
- assert cgroup2fs
- assert graphroot label = container_var_lib_t   (ls -Z)
- assert subuid range exists
- assert sudo -n -u solace whoami == solace
- assert storage-group exists, owned correctly
- assert secrets present (cert, password, PSK)
- assert FQDNs resolve (getent)
