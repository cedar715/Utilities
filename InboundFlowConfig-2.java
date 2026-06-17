systemctl show user@5011.service -p LimitNOFILE -p LimitMEMLOCK -p Delegate
# want: LimitNOFILE=1048576
#       LimitMEMLOCK=infinity (shows as a big number or 'infinity')
#       Delegate=yes  (systemd reports the specific controllers as Delegate=yes)
