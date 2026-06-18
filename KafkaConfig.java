# on the VM, make the solace backup readable by appadmintemp
sudo cp /home/solace/solace-backup-*.tar.gz /tmp/
sudo chown appadmintemp:appadmintemp /tmp/solace-backup-*.tar.gz
