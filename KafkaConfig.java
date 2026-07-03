@workspace You are reviewing a PR that adds a parallel podman-compose runtime
alongside our existing, proven podman-run + systemd flow for deploying Solace
PubSub+ Enterprise HA. Same Ansible playbooks the team already uses; compose is
integrated in, selectable via broker_runtime: podman_run | podman_compose.
podman-run stays the untouched default.

FIRST, inspect the repo and report what you found before reviewing:
- the existing podman-run path: roles/solace_systemd/templates/solace.service.j2,
  solace.conf.j2, solace.env.j2, and how --user / mounts / env are built
- var sources (group_vars, inventories, mounts.* / apps_root)
- the compose additions in this PR (template, tasks, unit branching, gating var)
Match ALL existing conventions; do not invent a new style.

THEN review against these, output per finding as:
[SEVERITY] file:line — issue — regression risk to podman-run — fix

1. NON-REGRESSION (highest priority): with broker_runtime unset/podman_run, prove
   every run-path task/template/handler is unchanged. Any behavior change to the
   default path = BLOCKER. Verify compose tasks are gated and the gate defaults to
   run. Confirm the two runtimes can't run simultaneously (no port/cgroup/storage
   collision; starting one displaces the other).

2. SEAMLESS SWITCH: trace run→compose→run on one node. Each direction must end
   clean with no manual step / no residual artifact (stale unit, leftover
   container, orphaned storage, wrong ownership). Both runtimes must resolve to the
   SAME container identity: user "0:0"=id -u solace, same storage-group path+owner,
   :Z relabel, same mounts (storage/secrets/mnt_solace), same solace.env. Unit must
   branch (compose=Type=simple foreground up) and switch must daemon-reload +
   enable/disable correctly.

3. CONVENTIONS: naming, tags, become/identity, var precedence, idempotency, handlers
   match existing. Vars from existing inventory (mounts.*, apps_root), no hardcoded
   duplicates. Compose template idempotent (re-run must not thrash/restart).

4. ROOTLESS/SOLACE TRAPS — verify each handled:
   - :Z not :z; no named-volume device: wrapper
   - NO cap_drop:ALL / read_only / no-new-privileges (kill consul)
   - no memlock in ulimits; nofile hard=1048576
   - no published ports under network_mode: host; no 2222 collision
   - hostname/routername correct per HA role (wrong hostname breaks monitor join)
   - substrate (cgroup v2, linger, delegate.conf nofile/Delegate, SELinux graphroot
     label, subuid) asserted BEFORE compose starts; SELinux relabel re-applied after
     any podman system reset

5. HA & SECRETS: redundancy env correct per role, monitor omits activestandbyrole/
   matelink/configsync; PSK/cert per-node (not Config-Synced); switch must not wipe
   storage unless guarded.

6. ROLLBACK: compose failure must revert to run via existing playbook cleanly and
   documented; failures must surface clearly (no silent loop from Type/notify or
   missing storage → vmr-solaudit).

END with a go/no-go: "Can a node switch run→compose→run with zero manual
intervention and zero podman-run regression?" If no, list the exact blockers.
Flag anywhere you had to guess a convention because the diff was ambiguous.
