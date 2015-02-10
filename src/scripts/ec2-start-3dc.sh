#!/bin/bash

SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPTDIR/const.sh
source $SCRIPTDIR/nodes.sh

RUNDIR=usr/sinergiafs
DB=/ssd/storage/

ssh $ZKHOST sudo service zookeeper stop
ssh $ZKHOST sudo service zookeeper start
STARTTIME=`ssh $ZKHOST date +%s`000

for n in ${DC1_SERVERS[@]} ${DC1_CLIENTS[@]} ${DC2_SERVERS[@]} ${DC2_CLIENTS[@]} ${DC3_SERVERS[@]} ${DC3_CLIENTS[@]}; do
    ssh $n <<EOF &
sudo killall -9 java
sudo rm /tmp/*.vgc
sudo rm -r ${DB}/ringpaxos-db
EOF
done
# sudo service ntp stop
# sudo ntpdate -b pool.ntp.org

# sudo service ntp stop
# sudo ntpdate -b $ZKHOST
# sudo service ntp start

wait

sleep 10

# write configs
echo "
delete /ringpaxos/boot_time.bin
set /ringpaxos/config/multi_ring_start_time $STARTTIME
set /ringpaxos/config/multi_ring_lambda $LAMBDA
set /ringpaxos/config/multi_ring_delta_t $DELTA
set /ringpaxos/config/multi_ring_m $M
set /ringpaxos/config/reference_ring $REF_RING

set /ringpaxos/topology0/config/stable_storage $STORAGE
set /ringpaxos/topology0/config/tcp_nodelay 1
set /ringpaxos/topology0/config/learner_recovery $RECOVERY
set /ringpaxos/topology0/config/trim_modulo $TRIM_MOD
set /ringpaxos/topology0/config/auto_trim $TRIM_AUTO
set /ringpaxos/topology0/config/proposer_batch_policy $BATCH
set /ringpaxos/topology0/config/p1_resend_time $P1_TIMEOUT
set /ringpaxos/topology0/config/value_resend_time $PROPOSER_TIMEOUT

set /ringpaxos/topology1/config/stable_storage $STORAGE
set /ringpaxos/topology1/config/tcp_nodelay 1
set /ringpaxos/topology1/config/learner_recovery $RECOVERY
set /ringpaxos/topology1/config/trim_modulo $TRIM_MOD
set /ringpaxos/topology1/config/auto_trim $TRIM_AUTO
set /ringpaxos/topology1/config/proposer_batch_policy $BATCH
set /ringpaxos/topology1/config/p1_resend_time $P1_TIMEOUT
set /ringpaxos/topology1/config/value_resend_time $PROPOSER_TIMEOUT

set /ringpaxos/topology2/config/stable_storage $STORAGE
set /ringpaxos/topology2/config/tcp_nodelay 1
set /ringpaxos/topology2/config/learner_recovery $RECOVERY
set /ringpaxos/topology2/config/trim_modulo $TRIM_MOD
set /ringpaxos/topology2/config/auto_trim $TRIM_AUTO
set /ringpaxos/topology2/config/proposer_batch_policy $BATCH
set /ringpaxos/topology2/config/p1_resend_time $P1_TIMEOUT
set /ringpaxos/topology2/config/value_resend_time $PROPOSER_TIMEOUT

set /ringpaxos/topology3/config/stable_storage $STORAGE
set /ringpaxos/topology3/config/tcp_nodelay 1
set /ringpaxos/topology3/config/learner_recovery $RECOVERY
set /ringpaxos/topology3/config/trim_modulo $TRIM_MOD
set /ringpaxos/topology3/config/auto_trim $TRIM_AUTO
set /ringpaxos/topology3/config/proposer_batch_policy $BATCH
set /ringpaxos/topology3/config/p1_resend_time $P1_TIMEOUT
set /ringpaxos/topology3/config/value_resend_time $PROPOSER_TIMEOUT
" | ssh $ZKHOST $ZKDIR/bin/zkCli.sh -server $ZKHOST:2182

sleep 2


#start coordinators for each ring
#------------
xterm -e "ssh $DC1_ACC_0 $RUNDIR/node-ec2.sh; sleep 1000" &
xterm -e "ssh $DC1_REP_0 $RUNDIR/node-ec2.sh; sleep 1000" &
xterm -e "ssh $DC2_REP_0 $RUNDIR/node-ec2.sh; sleep 1000" &
xterm -e "ssh $DC3_REP_0 $RUNDIR/node-ec2.sh; sleep 1000" &

sleep 5

# start everything else
#------------
xterm -e "ssh $DC2_ACC_0 $RUNDIR/node-ec2.sh; sleep 1000" &

xterm -e "ssh $DC1_REP_1 $RUNDIR/node-ec2.sh; sleep 1000" &
xterm -e "ssh $DC1_REP_2 $RUNDIR/node-ec2.sh; sleep 1000" &

xterm -e "ssh $DC2_REP_1 $RUNDIR/node-ec2.sh; sleep 1000" &
xterm -e "ssh $DC2_REP_2 $RUNDIR/node-ec2.sh; sleep 1000" &

xterm -e "ssh $DC3_REP_1 $RUNDIR/node-ec2.sh; sleep 1000" &
xterm -e "ssh $DC3_REP_2 $RUNDIR/node-ec2.sh; sleep 1000" &


# #start acceptors for the big ring (these HAVE to be started last due to a strange behaviour of ring paxos latency)
# #------------
# xterm -e "ssh $DC1_ACC_0 $RUNDIR/node-ec2.sh" &
# xterm -e "ssh $DC2_ACC_0 $RUNDIR/node-ec2.sh" &

# # start paxos small rings
# #------------
# xterm -e "ssh $DC1_REP_0 $RUNDIR/node-ec2.sh" &
# xterm -e "ssh $DC1_REP_1 $RUNDIR/node-ec2.sh" &
# xterm -e "ssh $DC1_REP_2 $RUNDIR/node-ec2.sh" &

# xterm -e "ssh $DC2_REP_0 $RUNDIR/node-ec2.sh" &
# xterm -e "ssh $DC2_REP_1 $RUNDIR/node-ec2.sh" &
# xterm -e "ssh $DC2_REP_2 $RUNDIR/node-ec2.sh" &

# xterm -e "ssh $DC3_REP_0 $RUNDIR/node-ec2.sh" &
# xterm -e "ssh $DC3_REP_1 $RUNDIR/node-ec2.sh" &
# xterm -e "ssh $DC3_REP_2 $RUNDIR/node-ec2.sh" &

wait
