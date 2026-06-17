ss -tlnp 2>/dev/null | grep -E '8080|1943|943'
http://10.27.245.146:8080/SEMP/v2/monitor/about/api

# on VM1 — is the web/SEMP port listening?
ss -tlnp 2>/dev/null | grep -E '8080|1943|943|8008'
# does it answer locally?
curl -s -u admin:admin http://localhost:8080/SEMP/v2/monitor/about/api | grep -o 'responseCode.:.200'
