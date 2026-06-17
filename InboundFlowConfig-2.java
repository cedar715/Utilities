# is consul actually listening on the redundancy ports?
ss -tlnp 2>/dev/null | grep -E '8300|8301|8302|8741'

# can VM1 reach VM2 and VM3 on consul's port?
nc -zv 10.27.245.147 8300 2>&1
nc -zv 10.27.245.148 8300 2>&1
