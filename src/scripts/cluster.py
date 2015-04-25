from collections import defaultdict

# zookeeper node: 1
HEAD_NODE=[
    'node249'
]

# global acceptor nodes: 3
GLOBAL_NODES=[
    'node41',
    'node42',
    'node43',
]

# replicas: 3 per partition
REPLICA_NODES=[
    'node44',
    'node45',
    'node46',
    'node47',
    'node48',
    'node49',
    'node50',
    'node51',
    'node52',
]

# clients: 3 per partition (in other words, one per replica)
CLIENT_NODES=[
    'node54',
    'node55',
    'node56',
    'node57',
    'node58',
    'node59',
    'node60',
    'node62',
    'node63',
]


def get_roles_cluster(partitions):
    """Set fabric roles from nodes given the number of partitions
    """
    partitions=int(partitions)
    roles = defaultdict(list)

    if len(HEAD_NODE) != 1:
        raise Exception("HEAD_NODE should be a list of 1 node")
    roles['head'] = HEAD_NODE

    if len(GLOBAL_NODES) != 3:
        raise Exception("GLOBAL_NODES should be a list of 3 nodes")
    roles['server'] += GLOBAL_NODES
    roles['paxos_coordinator'].append(GLOBAL_NODES[0])
    roles['paxos_rest'] += (GLOBAL_NODES[1:])

    if len(REPLICA_NODES) < 3*partitions:
        raise Exception("REPLICA_NODES should be a list of > 3*partitions nodes per partition")
    roles['server'] += REPLICA_NODES[0:3*partitions]
    for p in range(0, partitions):
        roles['paxos_coordinator'].append(REPLICA_NODES[p*3])
        roles['paxos_rest'] += (REPLICA_NODES[p*3+1:p*3+3])

    if len(CLIENT_NODES) < 3*partitions:
        raise Exception("CLIENT_NODES should be a list of > 3*partitions nodes per partition")
    roles['client'] += CLIENT_NODES[0:3*partitions]
    roles['singleclient'].append(CLIENT_NODES[0])

    return roles


def gen_whoami(partitions):
    """Generate whoami.sh scripts to be copied the nodes
    """
    partitions = int(partitions)
    get_roles_cluster(partitions) # just to check for the correct number of nodes
    script = ""

    script += "export ZKHOST=%s\n" % HEAD_NODE[0]

    script += "[[ `hostname` == %s ]] && export RING=1 ID=0 NAME=acc1_0\n" % (GLOBAL_NODES[0])
    script += "[[ `hostname` == %s ]] && export RING=2 ID=0 NAME=acc2_0\n" % (GLOBAL_NODES[1])
    script += "[[ `hostname` == %s ]] && export RING=3 ID=0 NAME=acc3_0\n" % (GLOBAL_NODES[2])

    for p in range(0, partitions):
        script += "[[ `hostname` == %s ]] && export RING=%s ID=0 NAME=rep%s_0\n" % (
            REPLICA_NODES[p*3], p+1, p+1)
        script += "[[ `hostname` == %s ]] && export RING=%s ID=1 NAME=rep%s_1\n" % (
            REPLICA_NODES[p*3+1], p+1, p+1)
        script += "[[ `hostname` == %s ]] && export RING=%s ID=2 NAME=rep%s_2\n" % (
            REPLICA_NODES[p*3+2], p+1, p+1)

    for p in range(0, partitions):
        script += "[[ `hostname` == %s ]] && export RING=%s ID=0 NAME=cli%s_0\n" % (
            CLIENT_NODES[p*3], p+1, p+1)
        script += "[[ `hostname` == %s ]] && export RING=%s ID=1 NAME=cli%s_1\n" % (
            CLIENT_NODES[p*3+1], p+1, p+1)
        script += "[[ `hostname` == %s ]] && export RING=%s ID=2 NAME=cli%s_2\n" % (
            CLIENT_NODES[p*3+2], p+1, p+1)

    return script


def gen_dhtcfg(partition):
    """Generate dht.config for the given partition"""
    partition = int(partition)
    get_roles_cluster(partition) # just to check for the correct number of nodes
    cfg = "replication = 2\n"
    cfg += "%s 30000\n" % (REPLICA_NODES[(partition-1)*3])
    cfg += "%s 30000\n" % (REPLICA_NODES[(partition-1)*3 + 1])
    cfg += "%s 30000\n" % (REPLICA_NODES[(partition-1)*3 + 2])
    return cfg


def gen_storagecfg(partitions):
    """Generate storage.config for the given number of partitions"""
    partitions = int(partitions)
    get_roles_cluster(partitions) # just to check for the correct number of nodes
    cfg = "ch.usi.paxosfs.storage.HttpStorage\n"
    for p in range(0, partitions):
        cfg += "%s http://%s:30000\n" % (p+1, REPLICA_NODES[p*3])
        cfg += "%s http://%s:30000\n" % (p+1, REPLICA_NODES[p*3 + 1])
        cfg += "%s http://%s:30000\n" % (p+1, REPLICA_NODES[p*3 + 2])
    return cfg
