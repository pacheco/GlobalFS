#!/usr/bin/env python
import ec2helper
import itertools
import pprint
from collections import defaultdict

# EC2 constants
# -----------------------------
regions = ['us-west-2', 'us-east-1', 'eu-west-1']
regions_zones=['us-west-1c', 'us-east-1a', 'eu-west-1a']
regions_prices=[0.03, 0.045, 0.03]

# 5 REGION
# regions = ['us-west-1', 'us-east-1', 'eu-west-1', 'ap-southeast-2', 'ap-southeast-1']
# regions_zones=['us-west-1c', 'us-east-1a', 'eu-west-1a', 'ap-southeast-2b', 'ap-southeast-1b']
# regions_prices=[0.03, 0.045, 0.03, 0.03, 0.04]

head_region='us-west-1'
head_zone='us-west-1a'
head_price=0.05
head_type='r3.large'

instance_type='r3.large'
instances_per_region=7 # should be at least 5 for the spot_tag() to work


# SinergiaFS stuff
# -----------------------------
coordinators = [ # 'Name' of the ring coordinators
    'acc1_0',
    'rep1_0',
    'rep2_0',
    'rep3_0',
    'rep4_0',
    'rep5_0',
]

def roledefs_from_instances():
    """Return instance ips, grouped in roles, as used by fabric 'env.roledefs'
    """
    roles = defaultdict(list)
    connections = ec2helper.connect_all(*regions)
    instances = ec2helper.list_instances(*connections.values())

    for instance in instances:
        if instance.state_code != 16: ## only running instances
            continue
        if 'Name' in instance.tags and instance.tags['Name'] == 'head':
            roles['head'].append(instance.dns_name)
            continue
        if instance.tags['Type'] == 'server':
            roles['replica'].append(instance.dns_name)
            if instance.tags['Name'].startswith("rep"):
                roles['dht'].append(instance.dns_name)
            if instance.tags['Name'] in coordinators: ## instances of id 0 are coordinators
                roles['paxos_coordinator'].append(instance.dns_name)
            else: ## others are grouped in rest
                roles['paxos_rest'].append(instance.dns_name)
        else:
            roles['client'].append(instance.dns_name)

    roles['singleclient'] = roles['client'][0:1]
    return roles

if __name__ == '__main__':
    pprint.pprint(dict(grouped_instances()))
