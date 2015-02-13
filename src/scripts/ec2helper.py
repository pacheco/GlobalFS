#!/usr/bin/env python

import boto.ec2
import itertools
import pprint
from collections import defaultdict

regions = ['us-west-1', 'us-west-2', 'eu-west-1']

coordinators = [
    'acc1_0',
    'rep1_0',
    'rep2_0',
    'rep3_0',
]


def list_instances(*ec2conn):
    """list instance through the connections passed as argument"""
    result = []
    for conn in ec2conn:
        instances = conn.get_all_instances()
        result+= itertools.chain(*[i.instances for i in instances])
    return result


def connect_all(*regions):
    """connect to multiple regions by name. Returns a dict of name to connection"""
    return {r: boto.ec2.connect_to_region(r) for r in regions}


def grouped_instances():
    """Return instance ips, grouped in roles, as used by the fabfile
    """
    roles = defaultdict(list)
    connections = connect_all(*regions)
    instances = list_instances(*connections.values())

    for instance in instances:
        if instance.state_code != 16: ## only running instances
            continue
        if 'Name' in instance.tags and instance.tags['Name'] == 'head':
            roles['head'].append(instance.dns_name)
            continue
        if instance.tags['Type'] == 'server':
            roles['replica'].append(instance.dns_name)
            if instance.tags['Name'] in coordinators: ## instances of id 0 are coordinators
                roles['paxos_coordinator'].append(instance.dns_name)
            else: ## others are grouped in rest
                roles['paxos_rest'].append(instance.dns_name)
        else:
            roles['client'].append(instance.dns_name)
    return roles

if __name__ == '__main__':
    pprint.pprint(dict(grouped_instances()))
