solace@cnlvatfss001[DEV][~] $ ss -tlnp 2>/dev/null | grep -E '8300|8301|8302|8741'
LISTEN 0      1            0.0.0.0:8741       0.0.0.0:*    users:(("dataplane",pid=648211,fd=148))                                         
LISTEN 0      4096         0.0.0.0:8301       0.0.0.0:*    users:(("consul",pid=648491,fd=16))                                             
LISTEN 0      4096         0.0.0.0:8300       0.0.0.0:*    users:(("consul",pid=648491,fd=8))                                              
LISTEN 0      4096         0.0.0.0:8302       0.0.0.0:*    users:(("consul",pid=648491,fd=12))                                             
solace@cnlvatfss001[DEV][~] $ nc -zv 10.27.245.147 8300 2>&1
Ncat: Version 7.92 ( https://nmap.org/ncat )
Ncat: Connected to 10.27.245.147:8300.
Ncat: 0 bytes sent, 0 bytes received in 0.02 seconds.
solace@cnlvatfss001[DEV][~] $ nc -zv 10.27.245.148 8300 2>&1
Ncat: Version 7.92 ( https://nmap.org/ncat )
Ncat: Connection refused.
solace@cnlvatfss001[DEV][~] $ podman exec -it solace cli

Solace PubSub+ Enterprise Version 10.25.0.217

This Solace product is proprietary software of
Solace Corporation. By accessing this Solace product
you are agreeing to the license terms and conditions
located at http://www.solace.com/license-software

Copyright 2004-2026 Solace Corporation. All rights reserved.

Operating Mode: Message Routing Node

solace1> show redundancy detail
Configuration Status     : Enabled
Redundancy Status        : Down
  Last Failure Reason    : One or more nodes is offline
  Last Failure Time      : Jun 17 2026 07:14:53 UTC
Operating Mode           : Message Routing Node
Switchover Mechanism     : Hostlist
Auto Revert              : No
Redundancy Mode          : Active/Standby
Active-Standby Role      : Primary
Mate Router Name         : cncdcdev1backup
  SMF plain-text Port    : 
  SMF compressed Port    : 
  SMF SSL Port           : 
  Mate-Link Connect Via  : cdc-dev1-b.51080.cn.app.standardchartered.com
    Remote Port          : 8741
    SSL                  : Yes
ADB Link To Mate         : Up
  Last Failure Reason    : Mate Link Restart
  Last Failure Time      : Jun 17 2026 05:56:46 UTC
ADB Hello To Mate        : Up
  Last Failure Reason    : N/A
  Last Failure Time      : 
  Hello Interval (ms)    : 1000
  Hello Timeout (ms)     : 3000
  Avg Hello Latency (ms) : 0
  Max Hello Latency (ms) : 2

Interface        Static Address           Status
---------------- ------------------------ ------
intf0            10.27.245.146            Up
---Press any key to continue, or `q' to quit---

                               Primary Virtual Router  Backup Virtual Router
                               ----------------------  ----------------------
Activity Status                Local Active            Shutdown
Redundancy Interface Status    Up                      
VRRP Status                                            
  intf0                        Initialize              
VRRP Priority                  250                     
Message Spool Status           AD-Active               

Priority Reported By Mate:     Standby                 
  ADB Hello Protocol             Standby                 
  VRRP                                                 
    intf0                      Standby (100)           

Activity Status:               Local Active            
  Operational Status             Ready                   
    Redundancy Config Status       Enabled                 
    Message Spool Status           Ready                   
  SMRP Status                    Ready                   
    Db Build Status                Ready                   
    Db Sync Status                 Ready                   
  Internal Priority              Active                  
  Internal Activity Status       Local Active            
  Internal Redundancy State      Pri-Active              

Message Spool Status:          Ready                   
  Message Spool Config Status    Enabled                 
  VRID Config Status             Ready                   
  ADB Status                     Ready                   
---Press any key to continue, or `q' to quit---
    Flash Module Status            Ready                   
    Power Module Status            Ready                   
  ADB Contents                   Ready                   
    Local Contents Key             149.141.67.102:2,1      
    Mate Contents Key              149.141.67.102:2,1      
    Schema Match                   Yes                     
  Disk Status                    Ready                   
  Disk Contents                  Ready                   
    Disk Key (Primary)             149.141.67.102:2,1      
    Disk Key (Backup)              149.141.67.102:2,1      
  ADB Datapath Status            Ready                   
  Internal Redundancy State      AD-Active               
  Lock Owner                     N/A                     

solace1> 
solace1> show redundancy group
Node Router-Name   Node Type       Address           Status
-----------------  --------------  ----------------  ---------
cncdcdev1backup    Message-Router  cdc-dev1-b.51080  Online
                                     .cn.app.standa    
                                     rdchartered.co    
                                     m                 
cncdcdev1monitor   Monitor         cdc-dev1-c.51080  Offline
                                     .cn.app.standa    
                                     rdchartered.co    
                                     m                 
cncdcdev1primary*  Message-Router  cdc-dev1-a.51080  Online
                                     .cn.app.standa    
                                     rdchartered.co    
                                     m                 

* - indicates the current node
solace1> 
