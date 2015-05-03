import re
from collections import namedtuple, defaultdict
import ec2helper as ec2

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
