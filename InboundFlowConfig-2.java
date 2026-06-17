solace@cnlvatfss003[DEV][solace] $ ss -tlnp 2>/dev/null | grep -E '8300|8301|8302|8741'
solace@cnlvatfss003[DEV][solace] $ cd /home/solace/solace
solace@cnlvatfss003[DEV][solace] $ /home/solace/.local/bin/podman-compose -f solace-compose.yml up 2>&1 | tee /tmp/monitor-boot.log
c74626cef70f058d792497af012c27a4d9a6e06a950d61d8a3110430fdd0e4cb
e1ebeb6737898472172e0f6eeea5cfb4ca78fbce9767f7c1d30743dc139ab083
Port mappings have been discarded as one of the Host, Container, Pod, and None network modes are in use
e76aa619d53304c109b9afb64479501492ffaf8b962c299f6002c75d726d1b74
[solace] | Host Boot ID: c497d9a8-3743-44bc-8840-3952e74f5e81
[solace] | Starting PubSub+ Software Event Broker Container: Wed Jun 17 07:18:16 UTC 2026
[solace] | Setting umask to 077
[solace] | SolOS Version: soltr_10.25.0.217
[solace] |   File: /
[solace] |   Size: 50           Blocks: 0          IO Block: 4096   directory
[solace] | Device: 31h/49d      Inode: 269475649   Links: 1
[solace] | Access: (0555/dr-xr-xr-x)  Uid: (    0/    root)   Gid: (    0/    root)
[solace] | Access: 2026-06-17 07:18:26.879806890 +0000
[solace] | Modify: 2026-06-17 07:18:16.146848003 +0000
[solace] | Change: 2026-06-17 07:18:18.662838365 +0000
[solace] |  Birth: 2026-06-17 07:18:16.146848003 +0000
[solace] | unknownEarlyBirdLogPrefix: /opt/sbox/loadbuild/jenkins/slave/workspace/pubsubplus_git_10.25.0/solcbr/dataplane/sw/assuredDelivery/common/include/adCmnRuntime.hpp:104 WARN IOV_MAX is 1024
[solace] | /usr/local/lib/python3.9/site-packages/flex/__init__.py:6: UserWarning: pkg_resources is deprecated as an API. See https://setuptools.pypa.io/en/latest/pkg_resources.html. The pkg_resources package is slated for removal as early as 2025-11-30. Refrain from using this package or pin to Setuptools<81.
[solace] |   import pkg_resources
[solace] | Unable to raise event; rc(would block)
[solace] | consul: no process found
[solace] | consul: no process found
solace@cnlvatfss003[DEV][solace] $ grep -iE 'error|fatal|fail|abort|exit|denied|violation|cannot' /tmp/monitor-boot.log
solace@cnlvatfss003[DEV][solace] $ nc -zv 10.27.245.146 8300 2>&1    # primary
Ncat: Version 7.92 ( https://nmap.org/ncat )
Ncat: Connected to 10.27.245.146:8300.
Ncat: 0 bytes sent, 0 bytes received in 0.02 seconds.
solace@cnlvatfss003[DEV][solace] $ nc -zv 10.27.245.147 8300 2>&1    # backup
Ncat: Version 7.92 ( https://nmap.org/ncat )
Ncat: Connected to 10.27.245.147:8300.
Ncat: 0 bytes sent, 0 bytes received in 0.02 seconds.
solace@cnlvatfss003[DEV][solace] $ 
