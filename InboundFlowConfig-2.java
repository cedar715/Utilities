solace@cnlvatfss003[DEV][~] $ podman ps -a                    # should be empty
CONTAINER ID  IMAGE       COMMAND     CREATED     STATUS      PORTS       NAMES
solace@cnlvatfss003[DEV][~] $ podman images                   # likely empty (reset removes images too — re-pull later)
REPOSITORY  TAG         IMAGE ID    CREATED     SIZE
solace@cnlvatfss003[DEV][~] $ podman volume ls                # empty
DRIVER      VOLUME NAME
solace@cnlvatfss003[DEV][~] $ cat /home/solace/.config/containers/storage.conf | grep -iE 'graphroot|runroot|driver'
  driver = "overlay"
  graphRoot = "/opt/apps/solace/podman/graphroot"
  runRoot = "/opt/apps/solace/podman/runroot"
solace@cnlvatfss003[DEV][~] $ podman unshare ls -lan /opt/apps/solace/storage-group 2>/dev/null && echo "^ at /opt/apps"
solace@cnlvatfss003[DEV][~] $ podman unshare ls -lan /home/solace/solace/storage-group 2>/dev/null && echo "^ at ~/solace"
solace@cnlvatfss003[DEV][~] $ podman unshare ls -lan /app/solace/storage-group 2>/dev/null && echo "^ at /app"
solace@cnlvatfss003[DEV][~] $ 

