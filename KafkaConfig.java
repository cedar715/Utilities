# pip SSL Bootstrap Fix — RHEL9 CN Machines

**Date:** 2026-07-09  
**Ticket:** #14618559  
**Affected roles:** `admin_setup_podman`, `solace_ansible_pip_addon`

---

## Problem

On RHEL9 CN machines, `get-pip.py` attempts to download pip from the internal
Artifactory PyPI mirror but fails with:

```
Could not fetch URL https://artifactory.../pypi/simple/pip/:
There was a problem confirming the ssl certificate:
[SSL] error in system default config (_ssl.c:3179) - skipping
```

### Root Cause

Two compounding failures:

1. **`requests >= 2.30` pre-creates an `SSLContext` at import time.**  
   On RHEL9 hosts with a misconfigured FIPS/crypto-policy, the `SSLContext`
   constructor itself crashes:
   ```
   ssl.SSLError: [SSL] error in system default config (_ssl.c:3179)
   ```
   This kills every pip network call before a single byte is sent — the error
   occurs in Python's SSL layer, not in pip's download logic.

2. **`python3-pip` package install uses `ignore_errors: true`.**  
   If the package is unavailable in restricted repos it is silently absent,
   causing subsequent pip calls to fail with `No module named pip`.

---

## Fix — Three Layers

### Layer 1 — `OPENSSL_CONF=/dev/null`

```bash
export OPENSSL_CONF=/dev/null
python3 get-pip.py --user ...
```

Setting `OPENSSL_CONF` to `/dev/null` prevents Python's SSL module from loading
the broken system OpenSSL configuration file.  The environment variable is
scoped to the shell session only; it does not persist after the task completes.

> **Note:** This bypasses the system crypto-policy for that session, including
> any FIPS-mode enforcement.  The permanent fix at the OS level is to correct
> `/etc/ssl/openssl.cnf` or run `sudo update-crypto-policies --set DEFAULT`
> (requires OS-team involvement).

### Layer 2 — `--trusted-host` + Artifactory `--index-url`

```bash
python3 get-pip.py --user \
  --trusted-host pypi.org \
  --trusted-host files.pythonhosted.org \
  --trusted-host {{ py_artifactory | urlsplit('hostname') }} \
  --index-url {{ py_artifactory }}
```

Routes all pip traffic through the internal Artifactory PyPI mirror and
disables TLS certificate verification for the listed hosts.  This is the
standard pattern for corporate air-gapped environments.

### Layer 3 — Bundled `get-pip.py`

`get-pip.py` is bundled at:
```
roles/solace_ansible_pip_addon/files/get-pip.py
```
and copied to the solace user's home directory before bootstrapping, instead
of being downloaded via `wget` at runtime.  This avoids a chicken-and-egg
problem where `wget` itself would hit the same SSL failure.

> **Original source:**  
> `https://artifactory.global.example.com/artifactory/generic-cloud_local/com.ex.cloud/runtime/get-pip.py`

---

## Files Changed

| File | Change |
|---|---|
| `roles/admin_setup_podman/tasks/main.yaml` | Added pip presence check, copy of bundled `get-pip.py`, bootstrap task with `OPENSSL_CONF=/dev/null`, and `podman-compose` pip install — all using Layer 1 + Layer 2 |
| `roles/solace_ansible_pip_addon/tasks/install-pip-lxml-and-jmespath-certifi-xmltodict.yml` | Added `OPENSSL_CONF=/dev/null` to pip bootstrap and all `pip install` calls; switched from `wget` to bundled file copy |

---

## Is This a Standard Approach?

| Technique | Standard? | Notes |
|---|---|---|
| Bundling `get-pip.py` locally | ✅ Yes | Recommended by [official pip docs](https://pip.pypa.io/en/stable/installation/) for offline/air-gapped installs |
| `--trusted-host` + `--index-url` | ✅ Yes | Standard pip flags for internal PyPI mirrors (Artifactory, Nexus, etc.) |
| `OPENSSL_CONF=/dev/null` | ⚠️ Workaround | Widely documented fix for `_ssl.c:3179` on RHEL9/FIPS hosts; scoped to the shell session only.  The root cause should be fixed at the OS level by the infrastructure team. |

---

## Recommended OS-Level Fix (Out of Scope for This Repo)

Ask the infrastructure/OS team to resolve the broken OpenSSL configuration on
the affected CN machines:

```bash
# Option A — reset crypto-policy to default
sudo update-crypto-policies --set DEFAULT

# Option B — validate the openssl config
openssl version -a
openssl req -new -x509 -key /dev/null 2>&1   # should not crash
```

Once the host SSL config is repaired, `OPENSSL_CONF=/dev/null` can be removed
from the Ansible tasks.

