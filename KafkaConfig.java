---
# diagnose_ssl_config.yml
#
# Purpose: pinpoint the "[SSL] error in system default config (_ssl.c:3179)"
# that is breaking dnf / requests / get-pip on the CN podman hosts.
#
# Safe by design: every task uses command / shell / slurp / stat.
# None of these construct a Python SSLContext, so they will NOT hit the
# same failure that dnf/uri/get_url do. All results are printed to the
# Ansible output so you can read them straight from the run log.
#
# Run:
#   ansible-playbook -i <your_inventory> diagnose_ssl_config.yml --limit machine1,machine2,machine3
#
# If you cannot escalate on these hosts, set become: false below. The two
# config files read here are normally world-readable, so it will still work.

- name: Diagnose OpenSSL system-default-config failure
  hosts: all
  gather_facts: true
  become: true
  any_errors_fatal: false

  tasks:

    # 1. Reproduce the exact failure and capture OpenSSL's own error text.
    #    This is the ground truth: if this fails, it is host-level, not Ansible.
    - name: Reproduce SSLContext creation
      ansible.builtin.command:
        cmd: >-
          python3 -c "import ssl;
          ssl.create_default_context();
          print('SSL_CONTEXT_OK', ssl.OPENSSL_VERSION)"
      register: ssl_repro
      changed_when: false
      failed_when: false

    - name: Show reproduction result
      ansible.builtin.debug:
        msg:
          - "rc: {{ ssl_repro.rc }}"
          - "stdout: {{ ssl_repro.stdout | default('') }}"
          - "stderr: {{ ssl_repro.stderr_lines | default([]) }}"

    # 2. OpenSSL version / build details (which openssl.cnf it uses: OPENSSLDIR).
    - name: openssl version -a
      ansible.builtin.command: openssl version -a
      register: openssl_ver
      changed_when: false
      failed_when: false

    - name: Show openssl version
      ansible.builtin.debug:
        var: openssl_ver.stdout_lines

    # 3. Current crypto-policy (the usual culprit on RHEL/CentOS 9).
    - name: update-crypto-policies --show
      ansible.builtin.command: update-crypto-policies --show
      register: crypto_policy
      changed_when: false
      failed_when: false

    - name: Show crypto policy
      ansible.builtin.debug:
        msg: "crypto-policy: {{ crypto_policy.stdout | default('n/a') }} (rc={{ crypto_policy.rc }})"

    # 4. FIPS state. fips_enabled=1 with a non-FIPS policy = classic mismatch.
    - name: Read /proc/sys/crypto/fips_enabled
      ansible.builtin.command: cat /proc/sys/crypto/fips_enabled
      register: fips_enabled
      changed_when: false
      failed_when: false

    - name: Show FIPS state
      ansible.builtin.debug:
        msg: "fips_enabled: {{ fips_enabled.stdout | default('n/a') }}"

    # 5. A stray OPENSSL_CONF env var pointing at a bad file also triggers this.
    - name: Check OPENSSL_CONF in environment
      ansible.builtin.shell: 'echo "OPENSSL_CONF=${OPENSSL_CONF:-<unset>}"'
      register: openssl_conf_env
      changed_when: false
      failed_when: false

    - name: Show OPENSSL_CONF env
      ansible.builtin.debug:
        msg: "{{ openssl_conf_env.stdout }}"

    # 6. Locate the system openssl.cnf and confirm it exists.
    - name: Stat /etc/pki/tls/openssl.cnf
      ansible.builtin.stat:
        path: /etc/pki/tls/openssl.cnf
      register: openssl_cnf_stat

    - name: Slurp openssl.cnf
      ansible.builtin.slurp:
        src: /etc/pki/tls/openssl.cnf
      register: openssl_cnf
      when: openssl_cnf_stat.stat.exists

    - name: Show the .include lines and [system_default_sect] area of openssl.cnf
      ansible.builtin.debug:
        msg: >-
          {{ (openssl_cnf.content | b64decode).splitlines()
             | select('search', '(?i)(\.include|system_default|MinProtocol|CipherString|Ciphersuites|Groups|SignatureAlgorithms|Options|Providers)')
             | list }}
      when: openssl_cnf_stat.stat.exists

    # 7. Read the crypto-policies OpenSSL backend that openssl.cnf .includes.
    #    This is where an unsupported/invalid directive most often lives.
    - name: Stat crypto-policies opensslcnf backend
      ansible.builtin.stat:
        path: /etc/crypto-policies/back-ends/opensslcnf.config
      register: backend_stat

    - name: Slurp crypto-policies opensslcnf backend
      ansible.builtin.slurp:
        src: /etc/crypto-policies/back-ends/opensslcnf.config
      register: backend_cfg
      when: backend_stat.stat.exists

    - name: Show full crypto-policies opensslcnf backend
      ansible.builtin.debug:
        var: (backend_cfg.content | b64decode).splitlines()
      when: backend_stat.stat.exists

    - name: Note if backend is missing (broken/partial crypto-policies state)
      ansible.builtin.debug:
        msg: "MISSING /etc/crypto-policies/back-ends/opensslcnf.config — crypto-policies is in a broken/partial state"
      when: not backend_stat.stat.exists

    # 8. Force OpenSSL to parse the config directly. Unlike the opaque Python
    #    message, the C tool often names the exact bad line / directive.
    - name: Force openssl to load the config (surfaces the parse error)
      ansible.builtin.command: openssl ciphers -v
      register: openssl_parse
      changed_when: false
      failed_when: false

    - name: Show openssl config-parse result
      ansible.builtin.debug:
        msg:
          - "rc: {{ openssl_parse.rc }}"
          - "stderr: {{ openssl_parse.stderr_lines | default([]) }}"

    # 9. One-line verdict.
    - name: Verdict
      ansible.builtin.debug:
        msg: >-
          {{ 'SSL context creation WORKS on this host — problem is elsewhere.'
             if ssl_repro.rc == 0
             else 'CONFIRMED host-level OpenSSL config failure. Inspect the [system_default_sect] / crypto-policies backend output above for the offending directive (commonly MinProtocol, CipherString, Ciphersuites, Groups, SignatureAlgorithms, or a FIPS/policy mismatch).' }}
