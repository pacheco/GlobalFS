from kazoo.client import KazooClient
from kazoo.client import KazooState
from collections import defaultdict
import threading
import sys
import json
import logging
import time
import subprocess
import atexit

CONTROLLER = '_controller'

ZK_TIMEOUT=15
ZK_BARRIERS='/barriers'
ZK_ERRORS='/errors'
ZK_STATUS='/status'

# this node
node = CONTROLLER
# all nodes, node->roles
node_roles = None
# all roles, role->nodes
role_nodes = None

# exp config
config = None
# zk client
zk = None

def _exit_error(msg, exception=None):
    print msg, ':',
    if exception:
        print exception,
    print
    sys.exit(1)

def _load_cfg(path):
    global config
    try:
        with open(path) as f:
            config = json.load(f)
    except IOError as e:
        _exit_error('Error reading config file!', e)

def _load_nodes(path):
    global node_roles, role_nodes
    # load nodes from node file
    try:
        with open(path) as f:
            node_roles = json.load(f)
    except IOError as e:
        _exit_error('Error reading nodes file!', e)
    role_nodes = defaultdict(lambda: [])
    # reverse mapping from role to nodes
    for n,rs in node_roles.iteritems():
        for r in rs:
            role_nodes[r.split(':')[0]].append(n)

def _node_check(node):
    if node == CONTROLLER:
        return
    if node not in node_roles:
        _exit_error('Node not in nodes file!', node)

def _connect_zk(host):
    c = KazooClient(host)
    c.start(timeout=ZK_TIMEOUT)
    return c

def _init_zk():
    # close zookeeper connection on exit
    atexit.register(lambda: zk.stop())
    if node_is_controller():
        try:
            zk.delete(config['zookeeper_root'] + ZK_BARRIERS, recursive=True)
        except NoNodeError:
            pass
        zk.ensure_path(config['zookeeper_root'])
        zk.ensure_path(config['zookeeper_root'] + ZK_BARRIERS)
        zk.ensure_path(config['zookeeper_root'] + ZK_ERRORS)
        zk.ensure_path(config['zookeeper_root'] + ZK_STATUS)
        # ephemeral node used to signal other nodes that controller is up
        zk.create(config['zookeeper_root'] + '/' + CONTROLLER, ephemeral=True)
    else:
        # not a controller, wait for controller to be up
        lock = threading.Semaphore(0)
        def _controller_watch(event):
            lock.release()
        if not zk.exists(config['zookeeper_root'] + '/' + CONTROLLER, watch=_controller_watch):
            lock.acquire()

def ssh(host, cmd, exit_on_error=False):
    """Run the cmd on the given host using ssh. Returns None on success or
    the command return code on error.  If exit_on_error is True,
    exists the script

    """
    ssh = subprocess.Popen(["ssh", "%s" % host, cmd],
                           shell=False,
                           stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE)
    ssh.wait()
    if ssh.returncode and ssh.returncode != 0:
        if ssh.returncode == 255: # ssh error!
            msg = 'Error while sshing into %s' % (host)
        else:
            msg = 'Error(%s) executing "%s" on "%s" (ssh)!' % (ssh.returncode, cmd, host)
        if exit_on_error:
            _exit_error(msg)
        else:
            print msg
            return ssh.returncode
    return None

def parallel_ssh(hosts, cmd, exit_on_error=False):
    """Run the cmd on the given host using ssh. Returns an empty list on
    success. On error(s) will return a list of (host, exitcode) tuples
    for each host where the command failed.

    """
    procs = dict()
    for h in hosts:
        procs[h] = subprocess.Popen(["ssh", "%s" % h, cmd],
                                    shell=False,
                                    stdout=subprocess.PIPE,
                                    stderr=subprocess.PIPE)
    error = []
    for h,p in procs.iteritems():
        p.wait()
        if p.returncode and p.returncode != 0:
            if p.returncode == 255: # ssh error!
                msg = 'Error while sshing into %s' % (h)
            else:
                msg = 'Error(%s) executing "%s" on "%s" (ssh)!' % (p.returncode, cmd, h)
            if exit_on_error:
                _exit_error(msg)
            else:
                print msg
                error.append((h,p.returncode))
    return error

def node_has_role(role, id=None):
    """True if this node has the given role, False otherwise"""
    if id:
        return role+str(id) in node_roles[node]
    return node in role_nodes[role]

def node_is_controller():
    """True if this node is the controller"""
    return node == CONTROLLER

def init(config_file, this_node=CONTROLLER):
    """Initialize the expcontrol system. Setup zookeeper nodes and other things.
    this_node should be one of the nodes in the nodes_file referenced in the config"""
    global node, zk
    # load config
    _load_cfg(config_file)
    # load nodes
    _load_nodes(config['nodes_file'])
    # set own address / name
    node = this_node
    _node_check(node)
    if node == CONTROLLER:
        print "Starting as the controller..."
    else:
        print "Starting %s as %s" % (node, node_roles[node])
    # connect to zookeeper
    zk = _connect_zk(config['zookeeper'])
    _init_zk()

def barrier(id):
    """Returns when all nodes reach this barrier"""
    print 'Waiting at barrier', id
    count = len(node_roles.keys()) + 1
    path = config['zookeeper_root'] + ZK_BARRIERS + '/' + str(id)
    b = zk.DoubleBarrier(path, count, identifier=node)
    b.enter()
    b.leave()

if __name__ == '__main__':
    logging.basicConfig()
    if len(sys.argv) > 2:
        init(sys.argv[1], sys.argv[2])
    else:
        init(sys.argv[1])
    for i in range(1,10):
        barrier(i)
