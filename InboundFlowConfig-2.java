# see it from root's view — confirm 760370
sudo ls -lan /home/solace/secrets

# what's inside (preserve cert if present)
sudo ls -la /home/solace/secrets

  sudo chown -R 5011:5011 /home/solace/secrets
sudo ls -lan /home/solace/secrets    # now owner 5011 5011
