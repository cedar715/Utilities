# as appadmintemp, become solace (now works via the sudoers grant)
sudo -u solace bash -c '
  python3 -c "import podman_compose" 2>/dev/null && echo "already installed" && exit 0
  python3 -m pip install podman-compose --user --index-url <your py_artifactory value>
'
echo "exit=$?"
