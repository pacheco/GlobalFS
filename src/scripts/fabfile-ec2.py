from fabric.api import *
from ec2config import roledefs_from_instances
import time


env.use_ssh_config = True
env.colorize_errors = True
env.disable_known_hosts = True
env.roledefs = roledefs_from_instances() # get ips for the roledef lists from ec2 instances

MRP_CONFIG = {
    'MRP_START_TIME' : 0,
    'MRP_DELTA' : 10,
    'MRP_LAMBDA' : 100000,
    'MRP_M' : 1,
    'MRP_REF_RING' : 0,
    'MRP_STORAGE' : 'ch.usi.da.paxos.storage.BufferArray',
    'MRP_BATCH' : 'none',
    'MRP_RECOVERY' : 1,
    'MRP_TRIM_MOD' : 0,
    'MRP_TRIM_AUTO' : 0,
    'MRP_P1_TIMEOUT' : 10000,
    'MRP_PROPOSER_TIMEOUT' : 10000,
}

# FUSE mount options
FUSE_OPTIONS = " ".join([
    '-o direct_io',
    #'-o noauto_cache',
    '-o entry_timeout=10s',
    '-o negative_timeout=10s',
    '-o attr_timeout=10s',
    '-o ac_attr_timeout=10s',
])


# note the MRP_CONFIG interpolation at the end
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


def dtach_and_log(command, dtach_socket, logfile):
    """Generate a command to leave the program running in the background
    with its output copied to a logfile.

    """
    return 'dtach -n %s bash -c "%s | tee %s"' % (dtach_socket, command, logfile)


@parallel
@roles('replica', 'client')
def rsync_from_head():
    """Synchronize code from headnode
    """
    with hide('stdout', 'stderr'):
        HEADNODE = env.roledefs['head'][0]
        run('rsync -azr --delete %s:.bashrc ~' % (HEADNODE))
        run('rsync -azr --delete %s:.ssh ~' % (HEADNODE))
        run('rsync -azr --delete %s:usr ~' % (HEADNODE))


@parallel
@roles('replica', 'client')
def ntpsync():
    """Synchronize NTP with remote server
    """
    with hide('stdout', 'stderr'):
        sudo('service ntp stop')
        sudo('ntpdate -b pool.ntp.org')


@parallel
@roles('replica', 'client')
def kill_and_clear():
    """Kill server processes and remove old data
    """
    with settings(warn_only=True):
        run('pkill --signal 9 -f TTYNode')
        run('pkill --signal 9 -f FSMain')
        run('pkill --signal 9 -f dht.lua')
        sudo('pkill --signal 9 -f PaxosFileSystem')
        sudo('umount -l /tmp/fs*')
        to_rm = ['/tmp/*.vgc',
                 '/tmp/sinergia*',
                 '/tmp/replica*',
                 '/tmp/dht*',
                 '/tmp/acceptor*',
                 '/tmp/storage*',
                 '/tmp/ringpaxos-db',
                 '/ssd/storage/ringpaxos-db',
                 '/tmp/*.log']
        sudo('rm -rf ' + ' '.join(to_rm))

@roles('head')
def setup_zookeeper():
    """Setup zookeeper with MRP parameters
    """
    with hide('stdout', 'stderr'):
        sudo('service zookeeper restart')
        MRP_CONFIG['MRP_START_TIME'] = run('date +%s000')
        time.sleep(5) # needed?
        run(dtach_and_log("~/usr/Paxos-trunk/ringpaxos.sh '0,0:L;1,0:L;2,0:L;3,0:L' localhost:2182",
                          '/tmp/zkcfg',
                          '/tmp/trash'))
        time.sleep(10)
        with settings(warn_only = True): # pkill is not returning 0 even on success
            run('pkill --signal 9 -f TTYNode')
        # set zookeeper MRP variables
        run('echo "%s" | zkCli.sh -server localhost:2182' % (ZKCONFIG % MRP_CONFIG))


@roles('head')        
def paxos_on():
    """
    """
    with cd('usr/sinergiafs'), settings(warn_only=True):
        while run('java ch.usi.paxosfs.client.CheckIfRunning 3 localhost:2182').return_code != 0:
            print 'NOT YET :('
    print 'OK!'


@parallel
def start_node():
    """Start the paxos/replica node
    """
    with hide('stdout', 'stderr'):
        with cd('usr/sinergiafs/'):
            run(dtach_and_log('./node-ec2.sh',
                              '/tmp/nodeec2',
                              '/tmp/nodeec2.log'))


@parallel
@roles('dht')
def start_dht():
    """Start the dht
    """
    with cd('usr/sinergiafs-dht/'):
        run(dtach_and_log(
            'lua ./dht.lua /home/ubuntu/dht${RING}.config $[ID + 1] /tmp/dhtstorage dht',
            '/tmp/dht',
            '/tmp/dht.log'))

            
def start_servers():
    """Start the sinergiafs servers
    """
    env.roles = ['paxos_coordinator']
    execute(start_node)
    time.sleep(5)
    env.roles = ['paxos_rest']
    execute(start_node)


@parallel
@roles('client')
def mount_fs():
    """Mount the fuse filesystem
    """
    with settings(warn_only=True):
        sudo('umount -l /tmp/fs')
        run('mkdir -p /tmp/fs')
        HEADNODE = env.roledefs['head'][0]
        with cd('usr/sinergiafs'):
            cmd = './client-mount.sh 3 %s:2182 ~/storage.config $ID $RING -f %s /tmp/fs' % (HEADNODE, FUSE_OPTIONS)
            run(dtach_and_log(cmd, '/tmp/sinergiafs', '/tmp/sinergiafs.log'))


def start_all():
    """Starts the whole system, replicas and clients (mountpoints)
    """
    execute(kill_and_clear)
    execute(ntpsync)
    execute(setup_zookeeper)
    execute(start_servers)
    execute(start_dht)
    time.sleep(5)
    execute(paxos_on)
    execute(mount_fs)
