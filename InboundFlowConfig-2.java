Both confirmed, and the second grep tells me something useful: there's no UID variable anywhere — prod always derives it live via $(id -u {{ solace_local_service_user }}). Every reference computes it at runtime. That's the one place compose genuinely can't mirror podman-run (YAML can't shell out), so the role must resolve it at render time. Now I have everything — here's the final, no-placeholder template.
The complete prod-faithful compose template
roles/solace_compose/templates/solace-compose.yml.j2:
yaml# Faithful twin of the podman-run path. All values from the SAME mounts.* /
# apps_root inventory vars podman-run uses. Differs only in runtime mechanism.
services:
  solace:
    image: "{{ solace_image }}:{{ solace_image_tag }}"
    container_name: "{{ solace_container_name | default('solace') }}"
    hostname: "{{ solace_routername }}"

    # podman-run uses: --user $(id -u solace)  → resolved to a fact (see tasks below)
    user: "{{ solace_resolved_uid }}:{{ solace_resolved_gid }}"

    shm_size: 1g
    ulimits:
      core: -1
      nofile:
        soft: 2448
        hard: 1048576
      # NO memlock — from user@.service.d/delegate.conf, not here

    volumes:
      # :Z == podman-run's relabel=private (NOT :z / named volume)
      - "{{ mounts.host_storage_group }}:{{ mounts.container_storage_group }}:Z"
      - "{{ mounts.host_secrets }}:{{ mounts.container_secrets }}:Z"
      - "{{ mounts.host_mnt_solace }}:{{ mounts.container_mnt_solace }}:Z"
{% if mounts.host_etc_hosts is defined %}
      - "{{ mounts.host_etc_hosts }}/hosts:{{ mounts.container_etc_hosts }}:Z"
{% endif %}

    env_file:
      - "/home/{{ solace_local_service_user }}/etc_solace/solace.env"

    # NO cap_drop / read_only / no-new-privileges (kill consul)
    # NO ports: here when host networking (HA); for standalone, omit 2222 if host sshd owns it
The UID-resolution task the role needs (the one real compose-vs-run difference)
Since prod derives the UID live and compose needs it at render time, add this to roles/solace_compose/tasks/main.yml before the template task:
yaml- name: Resolve solace uid/gid (mirror podman-run's $(id -u solace))
  ansible.builtin.command: "id -{{ item.flag }} {{ solace_local_service_user }}"
  register: _solace_ids
  changed_when: false
  loop:
    - { flag: 'u', name: 'uid' }
    - { flag: 'g', name: 'gid' }

- name: Set uid/gid facts for the compose template
  ansible.builtin.set_fact:
    solace_resolved_uid: "{{ _solace_ids.results[0].stdout }}"
    solace_resolved_gid: "{{ _solace_ids.results[1].stdout }}"
This makes user: resolve to 5011:5011 on every node automatically — exactly what --user $(id -u solace) produces in podman-run, no hardcoding.
What this template fixes vs GHCP's broken one — the full diff
AspectGHCP (broken)This (prod-faithful)user1001:0 (invented)5011:5011 via id -u (matches podman-run)storagenamed volume + device: + /opt/apps hardcoded{{ mounts.host_storage_group }} direct bindrelabel:z (shared):Z (private, = prod's relabel=private)secrets/mnt mountsmissingall three, from mounts.*capscap_drop: ALL, read_only, no-new-privilegesnone (consul survives)envinlinesolace.env (same file podman-run uses)
Two things to verify before you commit it

The solace.env file exists for the compose path — podman-run generates it via solace.env.j2. Confirm the compose flow also renders it (it should, if solace_systemd runs for both runtimes), or the broker won't get the admin password / preshared-key paths:

bash   ls -l /home/solace/etc_solace/solace.env    # on VM2 (or wherever compose deployed)

The secrets + mnt_solace dirs exist on the host before first boot, or the bind mounts fail:

bash   ls -ld /home/solace/secrets /home/solace/mnt_solace /opt/apps/solace/storage-group
Where you've landed
You now have the exact, complete spec for a prod-faithful compose template — reverse-engineered from the proven podman-run flow, using its own variables, matching user/paths/relabel/mounts/env. Drop the template + the UID-resolution task into roles/solace_compose/, and the role will generate config that boots identically to prod. That's the finish line for the role — after which VM3 and the HA pass run through clean automation.
Honestly, though — you've been at this for days and it's deep in the night. This message is the complete blueprint; it'll be just as valid after sleep, and editing the role with a clear head beats doing it exhausted and introducing a typo you'll chase for an hour. You've done the hard reverse-engineering; the template edit is mechanical and can wait for morning.
When you're ready to apply it, paste the two verifications above and I'll confirm the role's good to deploy on VM3. Genuinely good work getting from "nothing boots" to a fully-specified, prod-matched automation — that's the whole battle, basically won.
