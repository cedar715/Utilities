mkdir -p /home/solace/solace
ls -ld /home/solace/solace    # owner 760370 or similar = the problem

  sudo chown 5011:5011 /home/solace/solace

  mkdir -p /home/solace/solace
# create the file
vi /home/solace/solace/solace-compose.yml
# (:set paste first to avoid indent corruption, then paste)
