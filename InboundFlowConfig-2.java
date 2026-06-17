podman exec solace sh -c 'ps aux | grep -i consul | grep -v grep'   # should show a consul process
