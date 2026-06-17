solace@cnlvatfss003[DEV][solace] $ cat solace-compose.yml | grep nodetype
      redundancy_group_node_cncdcdev1primary_nodetype: message_routing
      redundancy_group_node_cncdcdev1backup_nodetype: message_routing
      redundancy_group_node_cncdcdev1monitor_nodetype: monitoring
      nodetype: monitoring
solace@cnlvatfss003[DEV][solace] $ /home/solace/.local/bin/podman-compose -f solace-compose.yml up 
c74626cef70f058d792497af012c27a4d9a6e06a950d61d8a3110430fdd0e4cb
a6dde192d46c6e8ea5c8a617ccfcadc4ceef18da7c8ef4f8ae2480d5b9be95f5
Port mappings have been discarded as one of the Host, Container, Pod, and None network modes are in use
9fd585282df51eda986da42e7f46dcf497c489154868942d1160c4f9d8e8a61c
[solace] | Host Boot ID: c497d9a8-3743-44bc-8840-3952e74f5e81
[solace] | Starting PubSub+ Software Event Broker Container: Wed Jun 17 07:31:55 UTC 2026
[solace] | Setting umask to 077
[solace] | SolOS Version: soltr_10.25.0.217
[solace] |   File: /
[solace] |   Size: 50           Blocks: 0          IO Block: 4096   directory
[solace] | Device: 31h/49d      Inode: 976723      Links: 1
[solace] | Access: (0555/dr-xr-xr-x)  Uid: (    0/    root)   Gid: (    0/    root)
[solace] | Access: 2026-06-17 07:32:06.345667885 +0000
[solace] | Modify: 2026-06-17 07:31:55.783708343 +0000
[solace] | Change: 2026-06-17 07:31:58.140699313 +0000
[solace] |  Birth: 2026-06-17 07:31:55.783708343 +0000
[solace] | unknownEarlyBirdLogPrefix: /opt/sbox/loadbuild/jenkins/slave/workspace/pubsubplus_git_10.25.0/solcbr/dataplane/sw/assuredDelivery/common/include/adCmnRuntime.hpp:104 WARN IOV_MAX is 1024
[solace] | /usr/local/lib/python3.9/site-packages/flex/__init__.py:6: UserWarning: pkg_resources is deprecated as an API. See https://setuptools.pypa.io/en/latest/pkg_resources.html. The pkg_resources package is slated for removal as early as 2025-11-30. Refrain from using this package or pin to Setuptools<81.
[solace] |   import pkg_resources
[solace] | Unable to raise event; rc(would block)
[solace] | consul: no process found
[solace] | consul: no process found
solace@cnlvatfss003[DEV][solace] $ 
