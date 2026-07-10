---
# diagnose_ssl_cn.yml
#
# READ-ONLY diagnostic. Makes NO changes to target hosts (it only stages and
# then removes a small temp probe under /tmp).
#
# Purpose: confirm whether the pip failure
#     [SSL] error in system default config (_ssl.c:3179)
# on the CN VMs is caused by host-side OpenSSL / crypto-policy state
# (i.e. VM / gold-image config), by:
#   1. reproducing the EXACT failing context AS THE solace USER, and
#   2. collecting the crypto configuration for side-by-side comparison
#      against a known-good host.
#
# HOW TO RUN
#   Include at least one known-GOOD host in the target list so you get a
#   baseline row to diff against. Use the SAME inventory / connection args
#   your pipeline uses to reach these hosts.
#
#   ansible-playbook -i <inventory> diagnose_ssl_cn.yml \
#       -e "target=cn_solace_hosts" -b
#
#   # ...or target the CN nodes plus one good host explicitly:
#   ansible-playbook -i <inventory> diagnose_ssl_cn.yml \
#       -e "target=cnshdlxj2b7z400.cn.example.com,goodhost.other.example.com" -b
#
# WHAT TO LOOK AT
#   The "COMPARISON SUMMARY" at the very end is the at-a-glance answer.
#   Interpretation:
#     bare_SSLContext = FAIL  -> SSL_CTX_new can't apply the system crypto
#                               policy. openssl.cnf -> crypto-policies backend
#                               is broken/unparseable for this OpenSSL build.
#                               THIS is the "system default config" root cause.
#     bare = OK, default = FAIL -> problem is in default trust store / verify
#                               paths rather than the crypto policy.
#     both = OK               -> context builds fine; if pip still fails the
#                               next error will be cert verification (internal
#                               CA / --trusted-host gap), not this one.
#   Then diff the CN rows against the good row: policy name, openssl version,
#   fips flag, backend sha, backend symlink target, and any OPENSSL_CONF
#   override are the usual culprits.

- name: Diagnose CN SSL / crypto-policy state (read-only)
  hosts: "{{ target | default('all') }}"
  gather_facts: false
  become: true
  vars:
    solace_user: solace
    probe_path: /tmp/ssl_probe.py
    # Change to a token that appears in your internal issuing CA's name,
    # so the trust-store check can tell you if the internal CA is present.
    ca_match: "example"

  tasks:
    - name: Stage the SSL probe (executed later as the solace user)
      copy:
        dest: "{{ probe_path }}"
        mode: "0644"
        content: |
          import ssl, sys, os

          def line(k, v):
              print("PROBE %-22s %s" % (k, v))

          line("python_executable", sys.executable)
          line("python_version", sys.version.split()[0])
          line("openssl_version", ssl.OPENSSL_VERSION)
          line("openssl_version_info", ".".join(str(x) for x in ssl.OPENSSL_VERSION_INFO))

          # Environment that can silently change OpenSSL / pip behaviour per-user.
          for var in ("OPENSSL_CONF", "SSL_CERT_FILE", "SSL_CERT_DIR",
                      "REQUESTS_CA_BUNDLE", "CURL_CA_BUNDLE", "PIP_CERT", "PIP_INDEX_URL"):
              line("env_" + var, os.environ.get(var, "<unset>"))

          # (1) Bare context. This ALONE applies the system_default crypto policy
          #     via SSL_CTX_new. A failure here == the crypto-policies backend
          #     included by /etc/pki/tls/openssl.cnf is broken/unparseable for
          #     this OpenSSL build. This is the smoking gun for the CN failure.
          try:
              ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
              line("bare_SSLContext", "OK")
          except Exception as e:
              line("bare_SSLContext", "FAIL: %s: %s" % (type(e).__name__, e))

          # (2) Default context. Bare + load default trust store / verify paths.
          try:
              ssl.create_default_context()
              line("create_default_context", "OK")
          except Exception as e:
              line("create_default_context", "FAIL: %s: %s" % (type(e).__name__, e))

          line("default_verify_paths", ssl.get_default_verify_paths())
          print("PROBE_END")

    - name: Reproduce the failing context AS solace (login shell, same as pipeline)
      # Mirrors the pipeline's exact invocation: su -s /bin/bash - solace -c "..."
      # The login '-' sources solace's profile, so any per-user OPENSSL_CONF /
      # PATH / custom python3 is faithfully reproduced here.
      command: "su -s /bin/bash - {{ solace_user }} -c 'python3 {{ probe_path }}'"
      register: probe_out
      changed_when: false
      failed_when: false

    - name: Collect host OpenSSL / crypto-policy configuration
      shell: |
        echo "===== OPENSSL VERSION ====="
        openssl version -a 2>&1
        echo "===== ACTIVE CRYPTO POLICY ====="
        update-crypto-policies --show 2>&1
        echo "===== FIPS ====="
        fips-mode-setup --check 2>&1
        echo "fips_enabled=$(cat /proc/sys/crypto/fips_enabled 2>/dev/null)"
        echo "===== BACKEND SYMLINK ====="
        ls -l /etc/crypto-policies/back-ends/opensslcnf.config 2>&1
        echo "resolves_to=$(readlink -f /etc/crypto-policies/back-ends/opensslcnf.config 2>&1)"
        echo "===== BACKEND SHA256 ====="
        sha256sum /etc/crypto-policies/back-ends/opensslcnf.config 2>&1
        echo "===== BACKEND CONTENT ====="
        cat /etc/crypto-policies/back-ends/opensslcnf.config 2>&1
        echo "===== openssl.cnf CRYPTO REFERENCES ====="
        grep -nE 'openssl_conf|crypto_policy|\.include|system_default|providers|fips' /etc/pki/tls/openssl.cnf 2>&1
        echo "===== openssl.cnf SHA256 ====="
        sha256sum /etc/pki/tls/openssl.cnf 2>&1
        echo "===== OPENSSL_CONF ENV OVERRIDES ====="
        grep -riE 'OPENSSL_CONF' /etc/environment /etc/profile /etc/profile.d \
             /home/{{ solace_user }}/.bash_profile /home/{{ solace_user }}/.bashrc \
             /home/{{ solace_user }}/.profile 2>/dev/null || echo "(none found)"
        echo "===== INTERNAL CA PRESENCE (token: {{ ca_match }}) ====="
        trust list 2>/dev/null | grep -iE '{{ ca_match }}' | head || echo "(internal CA token not found in trust store)"
        echo "===== END SYSFACTS ====="
      register: sys_out
      changed_when: false
      failed_when: false

    - name: Build a one-line host signature for the comparison table
      shell: |
        POL=$(update-crypto-policies --show 2>/dev/null | tr -d '\n')
        OSSL=$(openssl version 2>/dev/null | awk '{print $1" "$2}')
        FIPS=$(cat /proc/sys/crypto/fips_enabled 2>/dev/null)
        BSHA=$(sha256sum /etc/crypto-policies/back-ends/opensslcnf.config 2>/dev/null | awk '{print substr($1,1,12)}')
        BTGT=$(readlink -f /etc/crypto-policies/back-ends/opensslcnf.config 2>/dev/null)
        OCONF=$(grep -rhiE 'OPENSSL_CONF' /etc/environment /etc/profile.d 2>/dev/null | tr -d '\n')
        echo "policy=${POL:-?} | openssl=${OSSL:-?} | fips=${FIPS:-?} | backend_sha=${BSHA:-MISSING} | backend=${BTGT:-BROKEN_LINK} | openssl_conf=${OCONF:-none}"
      register: sig_raw
      changed_when: false
      failed_when: false

    - name: Full per-host probe output (as solace)
      debug:
        msg: "{{ probe_out.stdout_lines | default(['<no probe output>']) }}"

    - name: Full per-host system facts
      debug:
        msg: "{{ sys_out.stdout_lines | default(['<no sysfacts>']) }}"

    - name: Remove staged probe (cleanup)
      file:
        path: "{{ probe_path }}"
        state: absent
      changed_when: false

    - name: "COMPARISON SUMMARY  --  CN rows should FAIL and differ from the good host"
      run_once: true
      delegate_to: localhost
      loop: "{{ ansible_play_hosts | sort }}"
      debug:
        msg: >-
          {{ item }}
          :: {{ hostvars[item].probe_out.stdout_lines | default([]) | select('search', 'bare_SSLContext') | list | join(' / ') }}
          :: {{ hostvars[item].probe_out.stdout_lines | default([]) | select('search', 'create_default_context') | list | join(' / ') }}
          :: {{ hostvars[item].sig_raw.stdout | default('n/a') | trim }}
