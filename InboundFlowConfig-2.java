solace@cnlvatfss003[DEV][solace] $ findmnt -T /opt/apps/solace/podman/graphroot -o TARGET,SOURCE,FSTYPE,OPTIONS
TARGET    SOURCE                       FSTYPE OPTIONS
/opt/apps /dev/mapper/appsvg-optappslv xfs    rw,relatime,seclabel,attr2,inode64,logbufs=8,logbsize=32k,noquota
solace@cnlvatfss003[DEV][solace] $ 
solace@cnlvatfss003[DEV][solace] $ getenforce
Enforcing
solace@cnlvatfss003[DEV][solace] $ sudo ls -laZd /opt/apps/solace/podman/graphroot

We trust you have received the usual lecture from the local System
Administrator. It usually boils down to these three things:

    #1) Respect the privacy of others.
    #2) Think before you type.
    #3) With great power comes great responsibility.

[sudo] password for solace: 
^Csudo: a password is required
solace@cnlvatfss003[DEV][solace] $ ls -laZd /opt/apps/solace/podman/graphroot
drwx------. 9 solace solace unconfined_u:object_r:unlabeled_t:s0 4096 Jun 17 14:43 /opt/apps/solace/podman/graphroot
solace@cnlvatfss003[DEV][solace] $ exit
logout
You have mail in /var/spool/mail/root
root@cnlvatfss003[DEV][~] # exit
logout
appadmintemp@cnlvatfss003[DEV][~] $ sudo ls -laZd /opt/apps/solace/podman/graphroot
drwx------. 9 solace solace unconfined_u:object_r:unlabeled_t:s0 4096 Jun 17 14:43 /opt/apps/solace/podman/graphroot
appadmintemp@cnlvatfss003[DEV][~] $ sudo ausearch -m avc -ts recent 2>/dev/null | tail -30
node=cnlvatfss003 type=SYSCALL msg=audit(1781678764.550:309816): arch=c000003e syscall=257 success=no exit=-13 a0=ffffff9c a1=7ffda74d5d10 a2=0 a3=0 items=1 ppid=2537302 pid=103230 auid=4294967295 uid=0 gid=0 euid=0 suid=0 fsuid=0 egid=0 sgid=0 fsgid=0 tty=(none) ses=4294967295 comm="local" exe="/usr/libexec/postfix/local" subj=system_u:system_r:postfix_local_t:s0 key=(null)
node=cnlvatfss003 type=AVC msg=audit(1781678764.550:309816): avc:  denied  { search } for  pid=103230 comm="local" name="2537302" dev="proc" ino=42117800 scontext=system_u:system_r:postfix_local_t:s0 tcontext=system_u:system_r:postfix_master_t:s0 tclass=dir permissive=0
----
time->Wed Jun 17 14:46:04 2026
node=cnlvatfss003 type=PROCTITLE msg=audit(1781678764.551:309817): proctitle=6C6F63616C002D7400756E6978
node=cnlvatfss003 type=PATH msg=audit(1781678764.551:309817): item=0 name="/proc/2537302/stat" nametype=UNKNOWN cap_fp=0 cap_fi=0 cap_fe=0 cap_fver=0 cap_frootid=0
node=cnlvatfss003 type=CWD msg=audit(1781678764.551:309817): cwd="/var/spool/postfix"
node=cnlvatfss003 type=SYSCALL msg=audit(1781678764.551:309817): arch=c000003e syscall=257 success=no exit=-13 a0=ffffff9c a1=7ffda74d5d10 a2=0 a3=0 items=1 ppid=2537302 pid=103230 auid=4294967295 uid=0 gid=0 euid=0 suid=0 fsuid=0 egid=0 sgid=0 fsgid=0 tty=(none) ses=4294967295 comm="local" exe="/usr/libexec/postfix/local" subj=system_u:system_r:postfix_local_t:s0 key=(null)
node=cnlvatfss003 type=AVC msg=audit(1781678764.551:309817): avc:  denied  { search } for  pid=103230 comm="local" name="2537302" dev="proc" ino=42117800 scontext=system_u:system_r:postfix_local_t:s0 tcontext=system_u:system_r:postfix_master_t:s0 tclass=dir permissive=0
----
time->Wed Jun 17 14:46:04 2026
node=cnlvatfss003 type=PROCTITLE msg=audit(1781678764.552:309818): proctitle=6C6F63616C002D7400756E6978
node=cnlvatfss003 type=PATH msg=audit(1781678764.552:309818): item=0 name="/proc/2537302/stat" nametype=UNKNOWN cap_fp=0 cap_fi=0 cap_fe=0 cap_fver=0 cap_frootid=0
node=cnlvatfss003 type=CWD msg=audit(1781678764.552:309818): cwd="/var/spool/postfix"
node=cnlvatfss003 type=SYSCALL msg=audit(1781678764.552:309818): arch=c000003e syscall=257 success=no exit=-13 a0=ffffff9c a1=7ffda74d5d00 a2=0 a3=0 items=1 ppid=2537302 pid=103230 auid=4294967295 uid=0 gid=0 euid=0 suid=0 fsuid=0 egid=0 sgid=0 fsgid=0 tty=(none) ses=4294967295 comm="local" exe="/usr/libexec/postfix/local" subj=system_u:system_r:postfix_local_t:s0 key=(null)
node=cnlvatfss003 type=AVC msg=audit(1781678764.552:309818): avc:  denied  { search } for  pid=103230 comm="local" name="2537302" dev="proc" ino=42117800 scontext=system_u:system_r:postfix_local_t:s0 tcontext=system_u:system_r:postfix_master_t:s0 tclass=dir permissive=0
----
time->Wed Jun 17 14:46:04 2026
node=cnlvatfss003 type=PROCTITLE msg=audit(1781678764.555:309819): proctitle=6C6F63616C002D7400756E6978
node=cnlvatfss003 type=PATH msg=audit(1781678764.555:309819): item=0 name="/proc/2537302/stat" nametype=UNKNOWN cap_fp=0 cap_fi=0 cap_fe=0 cap_fver=0 cap_frootid=0
node=cnlvatfss003 type=CWD msg=audit(1781678764.555:309819): cwd="/var/spool/postfix"
node=cnlvatfss003 type=SYSCALL msg=audit(1781678764.555:309819): arch=c000003e syscall=257 success=no exit=-13 a0=ffffff9c a1=7ffda74d5d10 a2=0 a3=0 items=1 ppid=2537302 pid=103230 auid=4294967295 uid=0 gid=0 euid=0 suid=0 fsuid=0 egid=0 sgid=0 fsgid=0 tty=(none) ses=4294967295 comm="local" exe="/usr/libexec/postfix/local" subj=system_u:system_r:postfix_local_t:s0 key=(null)
node=cnlvatfss003 type=AVC msg=audit(1781678764.555:309819): avc:  denied  { search } for  pid=103230 comm="local" name="2537302" dev="proc" ino=42117800 scontext=system_u:system_r:postfix_local_t:s0 tcontext=system_u:system_r:postfix_master_t:s0 tclass=dir permissive=0
----
time->Wed Jun 17 14:46:04 2026
node=cnlvatfss003 type=PROCTITLE msg=audit(1781678764.556:309820): proctitle=6C6F63616C002D7400756E6978
node=cnlvatfss003 type=PATH msg=audit(1781678764.556:309820): item=0 name="/proc/2537302/stat" nametype=UNKNOWN cap_fp=0 cap_fi=0 cap_fe=0 cap_fver=0 cap_frootid=0
node=cnlvatfss003 type=CWD msg=audit(1781678764.556:309820): cwd="/var/spool/postfix"
node=cnlvatfss003 type=SYSCALL msg=audit(1781678764.556:309820): arch=c000003e syscall=257 success=no exit=-13 a0=ffffff9c a1=7ffda74d5a30 a2=0 a3=0 items=1 ppid=2537302 pid=103230 auid=4294967295 uid=0 gid=0 euid=0 suid=0 fsuid=0 egid=0 sgid=0 fsgid=0 tty=(none) ses=4294967295 comm="local" exe="/usr/libexec/postfix/local" subj=system_u:system_r:postfix_local_t:s0 key=(null)
node=cnlvatfss003 type=AVC msg=audit(1781678764.556:309820): avc:  denied  { search } for  pid=103230 comm="local" name="2537302" dev="proc" ino=42117800 scontext=system_u:system_r:postfix_local_t:s0 tcontext=system_u:system_r:postfix_master_t:s0 tclass=dir permissive=0
appadmintemp@cnlvatfss003[DEV][~] $ 
