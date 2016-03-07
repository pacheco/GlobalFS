import re
from collections import namedtuple, defaultdict
import ec2helper as ec2
from pprint import pprint

# TYPES
# -------------
EC2Region = namedtuple('EC2Region', ['region',
                                     'id',
                                     'nodes'])

EC2Node = namedtuple('EC2Node', ['region',
                                 'zone',
                                 'type',
                                 'name'])

EC2Deployment = namedtuple('EC2Deployment', ['regions',
                                             'head'])

deployments = {}

coordinators_re = re.compile('^(rep.*_0$|acc1_.*)')

def roledefs_from_instances(deployment):
    """Return instance ips, grouped in roles, as used by fabric 'env.roledefs'
    """
    roles = defaultdict(list)
    dep = deployments[deployment]
    connections = ec2.connect_all(*[x.region for x in dep.regions])
    instances = ec2.list_instances(*connections.values())

    for instance in instances:
        if instance.state_code != 16: ## only running instances
            continue
        # head node
        if 'Name' in instance.tags:
            name = instance.tags['Name']
            if name == 'head': # head node
                roles['head'].append(instance.dns_name)
                continue
            elif name.startswith('rep'):
                roles['server'].append(instance.dns_name)
                roles['storage'].append(instance.dns_name)
                if coordinators_re.match(name):
                    roles['paxos_coordinator'].append(instance.dns_name)
                else:
                    roles['paxos_rest'].append(instance.dns_name)
            elif name.startswith('acc'):
                roles['server'].append(instance.dns_name)
                if coordinators_re.match(name):
                    roles['paxos_coordinator'].append(instance.dns_name)
                else:
                    roles['paxos_rest'].append(instance.dns_name)
            elif name.startswith('cli'):
                roles['client'].append(instance.dns_name)
        roles['singleclient'] = roles['client'][0:1]
    return roles


# DEPLOYMENTS
# -------------

# 3 replicas and 3 clients per DC (1 of each in a diff avzone)
# ------------------------------------------------------------
dep_3 = EC2Deployment([], EC2Node('us-west-2', 'a', 'c3.large', 'head'))
dc = 1

region_zones = {
    'us-west-2' : ['a', 'b', 'c'],
    'us-east-1' : ['a', 'b', 'e'],
    'eu-west-1' : ['a', 'b', 'c'],
}

for region in region_zones.keys():
    rep = 0
    reg = EC2Region(region, dc, [])
    # headnode
    # if region == 'us-west-2':
    #     reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'head'))
    # replicas and clients
    reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'acc%s_0' % (dc)))
    for av in region_zones[region]:
        reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_3.regions.append(reg)
    dc += 1
deployments['d3'] = dep_3


# 6 replicas and 6 clients per DC
#----------------------------------------------------------
dep_6 = EC2Deployment([], EC2Node('us-west-2', 'a', 'c3.large', 'head'))
dc = 1

region_zones = {
    'us-west-2' : ['a', 'b', 'c'],
    'us-east-1' : ['a', 'b', 'e'],
    'eu-west-1' : ['a', 'b', 'c'],
}

for region in region_zones.keys():
    reg = EC2Region(region, dc, [])
    reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'acc%s_0' % (dc)))

    rep = 0
    for av in region_zones[region]:
        reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_6.regions.append(reg)
    dc += 1

    reg = EC2Region(region, dc, [])
    rep = 0
    for av in region_zones[region]:
        reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_6.regions.append(reg)
    dc += 1
deployments['d6'] = dep_6


# 9 replicas and 9 clients per DC
#----------------------------------------------------------
dep_9 = EC2Deployment([], EC2Node('us-west-2', 'a', 'c3.large', 'head'))
dc = 1

region_zones = {
    'us-west-2' : ['a', 'b', 'c'],
    'us-east-1' : ['a', 'b', 'e'],
    'eu-west-1' : ['a', 'b', 'c'],
}

for region in region_zones.keys():
    reg = EC2Region(region, dc, [])
    reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'acc%s_0' % (dc)))

    rep = 0
    for av in region_zones[region]:
        reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_9.regions.append(reg)
    dc += 1

    reg = EC2Region(region, dc, [])
    rep = 0
    for av in region_zones[region]:
        reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_9.regions.append(reg)
    dc += 1

    reg = EC2Region(region, dc, [])
    rep = 0
    for av in region_zones[region]:
        reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_9.regions.append(reg)
    dc += 1
deployments['d9'] = dep_9


# 12 replicas and 12 clients per DC
#----------------------------------------------------------
dep_12 = EC2Deployment([], EC2Node('us-west-2', 'a', 'c3.large', 'head'))
dc = 1

region_zones = {
    'us-west-2' : ['a', 'b', 'c'],
    'us-east-1' : ['a', 'b', 'e'],
    'eu-west-1' : ['a', 'b', 'c'],
}

for region in region_zones.keys():
    reg = EC2Region(region, dc, [])
    reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'acc%s_0' % (dc)))

    rep = 0
    for av in region_zones[region]:
        reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_12.regions.append(reg)
    dc += 1

    reg = EC2Region(region, dc, [])
    rep = 0
    for av in region_zones[region]:
        reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_12.regions.append(reg)
    dc += 1

    reg = EC2Region(region, dc, [])
    rep = 0
    for av in region_zones[region]:
        reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_12.regions.append(reg)
    dc += 1

    reg = EC2Region(region, dc, [])
    rep = 0
    for av in region_zones[region]:
        reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_12.regions.append(reg)
    dc += 1
deployments['d12'] = dep_12


# USED for GIT-Workload
# 4 partitions and 4 regions (DCs)
#----------------------------------------------------------
dep_4 = EC2Deployment([], EC2Node('us-west-2', 'a', 'c3.large', 'head'))
dc = 1

region_zones = {
    'us-west-2' : ['a', 'b', 'c'],
    'us-east-1' : ['a', 'c', 'e'],
    'eu-west-1' : ['a', 'b', 'c'],
    'sa-east-1' : ['a', 'b', 'c'],
}

for region in region_zones.keys():
    rep = 0
    reg = EC2Region(region, dc, [])
    reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'acc%s_0' % (dc)))
    for av in region_zones[region]:
        #should be r3, not available in sa-east-1
        reg.nodes.append(EC2Node(region, av, 'm3.large', 'rep%s_%s' % (dc, rep)))
        #should be c3, not available in sa-east-1b
        reg.nodes.append(EC2Node(region, av, 'm3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_4.regions.append(reg)
    dc += 1
deployments['d4'] = dep_4


# NEW DEPLOYMENTS ----------------------------------------------------
# --------------------------------------------------------------------

# one (1) regions, single partition (3 replicas and 3 clients)
# ------------------------------------------------------------

region = 'us-west-2'
dep_1 = EC2Deployment([], EC2Node(region, 'a', 'c3.large', 'head'))
dc = 1

region_zones = {
    region : ['a', 'b', 'c'],
}

rep = 0
reg = EC2Region(region, dc, [])
for av in region_zones[region]:
    reg.nodes.append(EC2Node(region, av, 'c3.large', 'acc%s_0' % (rep+1)))
    reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
    reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
    rep += 1
dep_1.regions.append(reg)

deployments['dep1'] = dep_1


# three (3) regions, 1 partition per region (3 replicas and 3 clients)
# ------------------------------------------------------------

dep_3 = EC2Deployment([], EC2Node('us-west-2', 'a', 'c3.large', 'head'))
dc = 1

region_zones = {
    'us-west-2' : ['a', 'b', 'c'],
    'us-west-1' : ['a', 'c', 'c'], # not enough regions!
    'us-east-1' : ['a', 'b', 'e']
}

region_order = [
    'us-west-2',
    'us-west-1',
    'us-east-1'
]

for region in region_order:
    rep = 0
    reg = EC2Region(region, dc, [])
    # headnode
    # if region == 'us-west-2':
    #     reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'head'))
    # replicas and clients
    if dc <= 3: # only 3 global acceptors
        reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'acc%s_0' % (dc)))
    for av in region_zones[region]:
        if region == 'sa-east-1': # doesnt have r3.large
            reg.nodes.append(EC2Node(region, av, 'c3.large', 'rep%s_%s' % (dc, rep)))
        else:
            reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_3.regions.append(reg)
    dc += 1
deployments['dep3'] = dep_3

# six (6) regions, 1 partition per region (3 replicas and 3 clients)
# ------------------------------------------------------------

dep_6 = EC2Deployment([], EC2Node('us-west-2', 'a', 'c3.large', 'head'))
dc = 1

region_zones = {
    'us-west-2' : ['a', 'b', 'c'],
    'us-west-1' : ['a', 'c', 'c'], # not enough regions!
    'us-east-1' : ['a', 'b', 'e'],
    'eu-west-1' : ['a', 'b', 'c'],
    'eu-central-1': ['a', 'b', 'b'], # not enough regions!
    'ap-northeast-1': ['a', 'c', 'c'], # northeast-1b not working...
}

region_order = [
    'us-west-2',
    'us-west-1',
    'us-east-1',
    'eu-west-1',
    'eu-central-1',
    'ap-northeast-1',
]

for region in region_order:
    rep = 0
    reg = EC2Region(region, dc, [])
    # headnode
    # if region == 'us-west-2':
    #     reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'head'))
    # replicas and clients
    if dc <= 3: # only 3 global acceptors
        reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'acc%s_0' % (dc)))
    for av in region_zones[region]:
        if region == 'sa-east-1': # doesnt have r3.large
            reg.nodes.append(EC2Node(region, av, 'c3.large', 'rep%s_%s' % (dc, rep)))
        else:
            reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_6.regions.append(reg)
    dc += 1
deployments['dep6'] = dep_6


# All regions (9), 1 partition per region (3 replicas and 3 clients)
# ------------------------------------------------------------

dep_9 = EC2Deployment([], EC2Node('us-west-2', 'a', 'c3.large', 'head'))
dc = 1

region_zones = {
    'us-west-2' : ['a', 'b', 'c'],
    'us-west-1' : ['a', 'c', 'c'], # not enough regions!
    'us-east-1' : ['a', 'b', 'e'],
    'eu-west-1' : ['a', 'b', 'c'],
    'eu-central-1': ['a', 'b', 'b'], # not enough regions!
    'ap-northeast-1': ['a', 'c', 'c'], # northeast-1b not working...
    'ap-southeast-1': ['a', 'b', 'b'], # not enough regions!
    'ap-southeast-2': ['a', 'b', 'b'], # not enough regions!
    'sa-east-1': ['a', 'c', 'c'] # sa-east-1b does not have c3.large
}

region_order = [
    'us-west-2',
    'us-west-1',
    'us-east-1',
    'eu-west-1',
    'eu-central-1',
    'ap-northeast-1',
    'ap-southeast-1',
    'ap-southeast-2',
    'sa-east-1'
]

for region in region_order:
    rep = 0
    reg = EC2Region(region, dc, [])
    # headnode
    # if region == 'us-west-2':
    #     reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'head'))
    # replicas and clients
    if dc <= 3: # only 3 global acceptors
        reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'acc%s_0' % (dc)))
    for av in region_zones[region]:
        if region == 'sa-east-1': # doesnt have r3.large
            reg.nodes.append(EC2Node(region, av, 'c3.large', 'rep%s_%s' % (dc, rep)))
        else:
            reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_9.regions.append(reg)
    dc += 1
deployments['dep9'] = dep_9


# GLUSTERFS EXPERIMENTS
# one region (us-west-2, Oregon) with only two zones
# ------------------------------------------------------------
dep_1_gluster = EC2Deployment([], EC2Node('us-west-2', 'a', 'c3.large', 'head'))
dc = 1

region_zones = {
    'us-west-2': ['a', 'b', 'c'],
}

for region in region_zones.keys():
    rep = 0
    reg = EC2Region(region, dc, [])
    # headnode
    # if region == 'us-west-2':
    #     reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'head'))
    # replicas and clients
    #reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'acc%s_0' % (dc)))
    for av in region_zones[region]:
        reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        #reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_1_gluster.regions.append(reg)
    dc += 1
deployments['d1_gluster'] = dep_1_gluster

# two regions (us-west-2 and us-west-1) with only two zones
# ------------------------------------------------------------
dep_2_gluster = EC2Deployment([], EC2Node('us-west-2', 'a', 'c3.large', 'head'))
dc = 1

region_zones = {
    'us-west-2': ['a', 'b', 'b'], # not enough regions!
    'us-west-1': ['a', 'b', 'b'] # not enough regions!
}

for region in region_zones.keys():
    rep = 0
    reg = EC2Region(region, dc, [])
    # headnode
    # if region == 'us-west-2':
    #     reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'head'))
    # replicas and clients
    #reg.nodes.append(EC2Node(region, region_zones[region][0], 'c3.large', 'acc%s_0' % (dc)))
    for av in region_zones[region]:
        reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        #reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    dep_2_gluster.regions.append(reg)
    dc += 1
deployments['d2_gluster'] = dep_2_gluster

# All regions (9), 1 partition per region (3 replicas and 3 clients)
# ------------------------------------------------------------

d3_gluster = EC2Deployment([], EC2Node('us-west-2', 'a', 'c3.large', 'head'))
dc = 1

region_zones = {
    'us-west-2' : ['a', 'b', 'c'],
    'us-west-1' : ['b', 'b', 'b'], # not enough regions!
    'eu-west-1' : ['a', 'b', 'c'],
}

region_order = [
    'us-west-2',
    'us-west-1',
    'eu-west-1',
]

for region in region_order:
    rep = 0
    reg = EC2Region(region, dc, [])
    # replicas and clients
    for av in region_zones[region]:
            #if region == 'sa-east-1': # doesnt have r3.large
        #    reg.nodes.append(EC2Node(region, av, 'c3.large', 'rep%s_%s' % (dc, rep)))
        #else:
        reg.nodes.append(EC2Node(region, av, 'r3.large', 'rep%s_%s' % (dc, rep)))
        reg.nodes.append(EC2Node(region, av, 'c3.large', 'cli%s_%s' % (dc, rep)))
        rep += 1
    d3_gluster.regions.append(reg)
    dc += 1
deployments['d3_gluster'] = d3_gluster

if __name__ == '__main__':
    for k, v in deployments.items():
        pprint(k)
        for r in v.regions:
            pprint(r.id)
            for n in r.nodes:
                pprint(n)
