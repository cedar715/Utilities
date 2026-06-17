Since prod derives the UID live and compose needs it at render time, add this to roles/solace_compose/tasks/main.yml before the template task:

- name: Resolve solace uid/gid (mirror podman-run's $(id -u solace))
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


  ---

  roles/solace_compose/templates/solace-compose.yml.j2:
# Faithful twin of the podman-run path. All values from the SAME mounts.* /
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
