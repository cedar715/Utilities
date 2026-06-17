host_storage_group is {{ apps_root }}/solace/storage-group across all inventories, where apps_root is the inventory-specific mount point for the data disk — typically /opt/apps (on-prem srack) or a variable resolved path
solace@myvm[~] $ ls -lan /opt/apps/solace/storage-group
total 36
drwxr-x---.   9 760370 5011   133 Jun 11 02:25 .
drwxr-xr-x.   3   5011 5011    27 Jul  3  2025 ..
drwx------.  12 760370 5011  4096 Jun 11 02:27 config
drwx------.   2 760370 5011 20480 Jun 17 09:53 diagnostics
lrwxrwxrwx.   1 760370 5011    27 Jun 11 02:25 diags -> /var/lib/solace/diagnostics
drwx------.   7 760370 5011   122 Jun 17 10:00 jail
drwx------. 102 760370 5011  4096 Jun 11 02:25 spool
drwx------.   2 760370 5011    58 Jun 11 02:25 spool-cache
drwx------.   3 760370 5011    19 Jul  3  2025 spool-cache-backup
drwx------.   9 760370 5011  4096 Jun 11 02:25 var
