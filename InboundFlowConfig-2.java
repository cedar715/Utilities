# on VM2 (target) — open a temp listener on an HA port
nc -l 8741 &
# on VM1 (source) — try to reach it
nc -zv 10.27.245.147 8741
#   "succeeded" / "open" = path is clear
#   "timed out" / "refused from filtering" = blocked, need a rule
# kill the listener on VM2 afterward: kill %1
