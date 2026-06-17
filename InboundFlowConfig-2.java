# where does the OLD container mount /var/lib/solace from?
podman ps -a --format '{{.Names}}'
podman inspect solace_solace_1 --format '{{range .Mounts}}{{.Source}} -> {{.Destination}}{{"\n"}}{{end}}' 2>/dev/null
# (use whatever container name 'podman ps -a' shows)
