solace@cnlvatfss001[DEV][~] $ podman exec -it solace cli

Solace PubSub+ Enterprise Version 10.25.0.217

This Solace product is proprietary software of
Solace Corporation. By accessing this Solace product
you are agreeing to the license terms and conditions
located at http://www.solace.com/license-software

Copyright 2004-2026 Solace Corporation. All rights reserved.

Operating Mode: Message Routing Node

cncdcdev1primary> show service

Msg-Backbone:       Enabled
  VRF:              management
  SMF:              Enabled
    Web-Transport:  Enabled
  REST Incoming:    Enabled
  REST Outgoing:    Enabled
  MQTT:             Enabled
  AMQP:             Enabled
  Health-check:     Enabled
  Health-check SSL: Enabled
  Mate-link:        Enabled
  Redundancy:       Enabled

Max Incoming Connections:       100
  Service SMF:                  100
  Service Web-Transport:        100
  Service REST:                 100
  Service MQTT:                 100
  Service AMQP:                 100
Max Outgoing Connections:
  Service REST:                 100
---Press any key to continue, or `q' to quit---
Max SSL Connections:            100

Event Threshold                           Set Value      Clear Value
---------------------------------- ---------------- ----------------
Incoming Connections                        80%(80)          60%(60) 
  Service SMF                               80%(80)          60%(60) 
Outgoing Connections
  Service REST                              80%(80)          60%(60) 
SSL Connections                             80%(80)          60%(60) 


Flags Legend:
TP - Transport
T+U - TCP and UDP
S - SSL (Y=Yes, N=No, -=not-applicable)
C - Compressed (Y=Yes, N=No, -=not-applicable)
R - Routing Ctrl (Y=Yes, N=No, -=not-applicable)
VRF - VRF (Mgmt=management, MsgBB=msg-backbone)
A - Admin State (U=Up, D=Down, -=not-applicable)
O - Oper State (U=Up, D=Down, -=not-applicable)

                                              Status
---Press any key to continue, or `q' to quit---
Service    TP  S C R VRF   MsgVpn          Port  A O Failed Reason
---------- --- ----- ----- --------------- ----- --- --------------------------
SEMP       TCP N - - Mgmt                   8080 U U 
SEMP       TCP Y - - Mgmt                   1943 U D No Cert
SMF        TCP N N N Mgmt                  55555 U U 
SMF        TCP N Y N Mgmt                  55003 U U 
SMF        TCP N N Y Mgmt                  55556 U D 
SMF        TCP Y N N Mgmt                  55443 U U No Cert
SMF        WEB N - - Mgmt                   8008 U U 
SMF        WEB Y - - Mgmt                   1443 U D No Cert
MQTT       TCP N - - Mgmt  default          1883 U U 
MQTT       TCP Y - - Mgmt  default          8883 U D No Cert
MQTT       WEB N - - Mgmt  default          8000 U U 
MQTT       WEB Y - - Mgmt  default          8443 U D No Cert
AMQP       TCP Y - - Mgmt                        U D No Cert
AMQP       TCP N - - MsgBB default          5672 U U 
AMQP       TCP Y - - MsgBB default          5671 U D No Cert
REST       WEB N - - Mgmt  default          9000 U U 
REST       WEB Y - - Mgmt  default          9443 U D No Cert
MATELINK   TCP Y N - Mgmt                   8741 U U 
HEALTHCHK  TCP N N - Mgmt                   5550 U U 
HEALTHCHK  TCP Y N - Mgmt                   5553 U D No Cert
---Press any key to continue, or `q' to quit---
REDUNDANCY TCP Y N - Mgmt                   8300 U U 
REDUNDANCY T+U Y N - Mgmt                   8301 U U 
REDUNDANCY T+U Y N - Mgmt                   8302 U U 
cncdcdev1primary>  
 Inactivity timeout expired. Closing session....

solace@cnlvatfss001[DEV][~] $ ls -l /home/solace/solace/
total 4
-rw-r-----. 1 solace solace 3465 Jun 17 15:49 solace-compose.yml
solace@cnlvatfss001[DEV][~] $ pwd
/home/solace
solace@cnlvatfss001[DEV][~] $ ls -lrt secrets/
total 12
-rw-------. 1 solace solace 2888 Jun  9 12:34 server.cert
-rw-------. 1 solace solace   13 Jun 17 13:03 username_admin_password
-rw-------. 1 solace solace   44 Jun 17 13:03 solace-preshared-key.conf
solace@cnlvatfss001[DEV][~] $ 
