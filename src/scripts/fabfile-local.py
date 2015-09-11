from fabric.api import *
import time
import subprocess
import os

# IMPORTANT: Set the following variables to the correct path
# ----------------------------------
FSDIR=os.path.expanduser('~/usr/sinergiafs')
UPAXOSDIR=os.path.expanduser('~/usr/Paxos-trunk')
ZKHOST='localhost:2181'

# metadata replica parameters
REPLICA_CONFIG = {
    'JVMOPT' : '-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/tmp/java-$$.vgc',
    'GC' : '-XX:+UseConcMarkSweepGC',
    'LIBPATH' : os.path.expanduser('~/usr/lib'),
    'NPARTITIONS' : 3,
    'ZKHOST' : ZKHOST,
}

# URingPaxos parameters
MRP_CONFIG = {
    'MRP_START_TIME' : 0,
    'MRP_DELTA' : 10,
    'MRP_LAMBDA' : 100000,
    'MRP_M' : 1,
    'MRP_REF_RING' : 0,
    'MRP_STORAGE' : 'ch.usi.da.paxos.storage.NoStorage',
    'MRP_BATCH' : 'none',
    'MRP_RECOVERY' : 0,
    'MRP_TRIM_MOD' : 0,
    'MRP_TRIM_AUTO' : 0,
    'MRP_P1_TIMEOUT' : 10000,
    'MRP_PROPOSER_TIMEOUT' : 10000,
}

# FUSE mount options
FUSE_OPTIONS = " ".join([
    '-o direct_io',
    '-o noauto_cache',
    # '-o entry_timeout=10s',
    # '-o negative_timeout=10s',
    # '-o attr_timeout=10s',
    # '-o ac_attr_timeout=10s',
])



# Script to configure zookeeper -> MRP_CONFIG will be interpolated with it later
ZKCONFIG ="""
delete /ringpaxos/boot_time.bin
set /ringpaxos/config/multi_ring_start_time %(MRP_START_TIME)s
set /ringpaxos/config/multi_ring_lambda %(MRP_LAMBDA)s
set /ringpaxos/config/multi_ring_delta_t %(MRP_DELTA)s
set /ringpaxos/config/multi_ring_m %(MRP_M)s
set /ringpaxos/config/reference_ring %(MRP_REF_RING)s

set /ringpaxos/topology0/config/stable_storage %(MRP_STORAGE)s
set /ringpaxos/topology0/config/tcp_nodelay 1
set /ringpaxos/topology0/config/learner_recovery %(MRP_RECOVERY)s
set /ringpaxos/topology0/config/trim_modulo %(MRP_TRIM_MOD)s
set /ringpaxos/topology0/config/auto_trim %(MRP_TRIM_AUTO)s
set /ringpaxos/topology0/config/proposer_batch_policy %(MRP_BATCH)s
set /ringpaxos/topology0/config/p1_resend_time %(MRP_P1_TIMEOUT)s
set /ringpaxos/topology0/config/value_resend_time %(MRP_PROPOSER_TIMEOUT)s

set /ringpaxos/topology1/config/stable_storage %(MRP_STORAGE)s
set /ringpaxos/topology1/config/tcp_nodelay 1
set /ringpaxos/topology1/config/learner_recovery %(MRP_RECOVERY)s
set /ringpaxos/topology1/config/trim_modulo %(MRP_TRIM_MOD)s
set /ringpaxos/topology1/config/auto_trim %(MRP_TRIM_AUTO)s
set /ringpaxos/topology1/config/proposer_batch_policy %(MRP_BATCH)s
set /ringpaxos/topology1/config/p1_resend_time %(MRP_P1_TIMEOUT)s
set /ringpaxos/topology1/config/value_resend_time %(MRP_PROPOSER_TIMEOUT)s

set /ringpaxos/topology2/config/stable_storage %(MRP_STORAGE)s
set /ringpaxos/topology2/config/tcp_nodelay 1
set /ringpaxos/topology2/config/learner_recovery %(MRP_RECOVERY)s
set /ringpaxos/topology2/config/trim_modulo %(MRP_TRIM_MOD)s
set /ringpaxos/topology2/config/auto_trim %(MRP_TRIM_AUTO)s
set /ringpaxos/topology2/config/proposer_batch_policy %(MRP_BATCH)s
set /ringpaxos/topology2/config/p1_resend_time %(MRP_P1_TIMEOUT)s
set /ringpaxos/topology2/config/value_resend_time %(MRP_PROPOSER_TIMEOUT)s

set /ringpaxos/topology3/config/stable_storage %(MRP_STORAGE)s
set /ringpaxos/topology3/config/tcp_nodelay 1
set /ringpaxos/topology3/config/learner_recovery %(MRP_RECOVERY)s
set /ringpaxos/topology3/config/trim_modulo %(MRP_TRIM_MOD)s
set /ringpaxos/topology3/config/auto_trim %(MRP_TRIM_AUTO)s
set /ringpaxos/topology3/config/proposer_batch_policy %(MRP_BATCH)s
set /ringpaxos/topology3/config/p1_resend_time %(MRP_P1_TIMEOUT)s
set /ringpaxos/topology3/config/value_resend_time %(MRP_PROPOSER_TIMEOUT)s
"""


def setup_zookeeper():
    """Setup zookeeper with MRP parameters
    """
    with hide('stdout', 'stderr'):
        local('sudo service zookeeper restart')
        MRP_CONFIG['MRP_START_TIME'] = local('date +%s000', capture = True)
        time.sleep(3) # needed?
        # create URingPaxos default config on zookeeper
        local('dtach -n /tmp/zkcfg %s/ringpaxos.sh "0,0:L;1,0:L;2,0:L;3,0:L" %s' % (UPAXOSDIR, ZKHOST))
        time.sleep(3)
        with settings(warn_only = True): # pkill is not returning 0 even on success
            local('pkill --signal 9 -f TTYNode')
        # change the config we need
        local('echo "%s" | zkCli.sh -server %s' % (ZKCONFIG % MRP_CONFIG, ZKHOST))


def kill_and_clear():
    """Kill java processes and remove old data
    """
    with settings(warn_only = True):
        local('pkill --signal 9 -f TTYNode')
        local('pkill --signal 9 -f FSMain')
        local('pkill --signal 9 -f kvstore')
        local('sudo pkill --signal 9 -f PaxosFileSystem')
        local('sudo umount -l /tmp/fs*')
        local('sudo rm -f /tmp/*.vgc')
        local('sudo rm -f /tmp/replica*')
        local('sudo rm -f /tmp/acceptor*')
        local('sudo rm -f /tmp/storage*')
        local('sudo rm -rf /tmp/ringpaxos-db')


def paxos_on():
    """Wait for the system to be running
    """
    with lcd(FSDIR), settings(warn_only=True):
        cmd = 'java ch.usi.paxosfs.client.CheckIfRunning %s %s' % (REPLICA_CONFIG['NPARTITIONS'], ZKHOST)
        while local(cmd).return_code != 0:
            print 'PAXOS NOT YET RUNNING... :('
    print 'PAXOS OK!'


def start_http_storage(partition):
    """Start the simple, non-replicated, http storage
    """
    cmd = './kvstore-rocksdb storagecfg/kvstore%s.cfg 0 /tmp/kvstore%s' % (partition, partition)
    with hide('stdout', 'stderr'), lcd(FSDIR):
        local('dtach -n /tmp/storage_%s %s' % (partition, cmd))


def start_acceptor(partition, id):
    """Start a paxos acceptor
    """
    cmd = './ringpaxos.sh %s,%s:A %s' % (partition, id, ZKHOST)
    with hide('stdout', 'stderr'), lcd(UPAXOSDIR):
        local('dtach -n /tmp/acceptor%s-%s %s' % (partition, id, cmd))


def start_replica(partition, id, port=20000):
    """Start a paxos/replica node
    """
    cmd = 'java -ea -cp . %(JVMOPT)s %(GC)s -Djava.library.path=%(LIBPATH)s ch.usi.paxosfs.replica.FSMain %(NPARTITIONS)s %(partition)s %(id)s %(port)s %(ZKHOST)s'
    cmd = cmd % dict(REPLICA_CONFIG.items() + {
        'partition' : partition,
        'id' : id,
        'port' : port,
    }.items())
    with hide('stdout', 'stderr'), lcd(FSDIR):
        local('dtach -n /tmp/replica%s-%s %s' % (partition, id, cmd));


def start_servers():
    partitions = REPLICA_CONFIG['NPARTITIONS']
    port = 20000
    # start ring leaders
    execute(start_acceptor, 0, 0)
    for i in range(1,partitions+1):
        execute(start_replica, i, 0, port)
        port += 1
    # start other servers
    execute(start_acceptor, 0, 1)
    execute(start_acceptor, 0, 2)
    for i in range(1,partitions+1):
        execute(start_replica, i, 1, port)
        port += 1
        execute(start_replica, i, 2, port)
        port += 1


def mount_fs(mountpath, replica_id, closest_partition):
    """Mount the fuse filesystem
    """
    with settings(warn_only=True):
        local('sudo umount -l %s' % (mountpath))
        local('mkdir -p %s' % (mountpath))
    with lcd(FSDIR):
        local('dtach -n /tmp/sinergiafs-%(rid)s ./client-mount.sh %(npart)s %(zkhost)s storagecfg/storage.cfg %(rid)s %(closestp)s -f %(fuseopt)s %(mountpath)s' % {
            'rid': replica_id,
            'npart': REPLICA_CONFIG['NPARTITIONS'],
            'zkhost': ZKHOST,
            'closestp': closest_partition,
            'mountpath': mountpath,
            'fuseopt': FUSE_OPTIONS,
        })


def start_all(partitions):
    """Starts the whole system, replicas and clients (mountpoints). Be
    sure that mount_fs is using a storage config that supports the
    number of partitions.
    """
    partitions = int(partitions)
    REPLICA_CONFIG['NPARTITIONS'] = partitions
    execute(kill_and_clear)
    execute(setup_zookeeper)
    time.sleep(5)
    execute(start_servers)
    execute(paxos_on)
    for p in range(1, partitions + 1):
        execute(start_http_storage, p)
        execute(mount_fs, '/tmp/fs%s' % (p), p-1, p)
