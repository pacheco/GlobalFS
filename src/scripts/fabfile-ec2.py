#!/usr/bin/env python
from fabric.api import *
import ec2helper as ec2
import time
from deployments import *
from pprint import pprint

env.use_ssh_config = True
env.colorize_errors = True
env.disable_known_hosts = True
env.roledefs = None


def set_roles(deployment):
    """Set fabric roles from running instances
    """
    env.roledefs = roledefs_from_instances(deployment) # get ips for the roledef lists from ec2 instances


@task
def image_list(deployment):
    """List images available on each region
    """
    dep = deployments[deployment]
    connections = ec2.connect_all(*[x.region for x in dep.regions])
    for region, conn in connections.iteritems():
        images = ec2.list_images(conn)
        print '%s:' % (region)
        for img in images:
            print ' %s %s - %s' % (img.name, img.id, img.state)


@task
def head_stop(deployment):
    """Stop head instance
    """
    dep = deployments[deployment]
    connections = ec2.connect_all(*[dep.head.region])
    instances = ec2.list_instances(*connections.values())
    for worker in instances:
        # ignore nodes named 'head'
        if 'Name' in worker.tags and worker.tags['Name'] == 'head':
            worker.stop()
            break


@task
def head_start(deployment, image_name=None):
    """Start/restart head instance
    """
    dep = deployments[deployment]
    connections = ec2.connect_all(*[dep.head.region])
    instances = ec2.list_instances(*connections.values())
    exists=False
    for worker in instances:
        # ignore nodes named 'head'
        if 'Name' in worker.tags and worker.tags['Name'] == 'head':
            if worker.state_code == 16:
                print '* Head already running:', worker.region.name, worker.id
            else:
                worker.start()
                exists=True
                break
    # create new instance
    if not exists and image_name:
        conn = connections[dep.head.region]
        imageid=None
        for img in ec2.list_images(conn, name=image_name):
            if img.state == u'available':
                imageid=img.id
                break
        if not imageid:
            raise Exception('image not available in region ' + dep.head.region)
        reserv = conn.run_instances(image[dep.head.region],
                                    instance_type = dep.head.type,
                                    placement = dep.head.region + dep.head.zone,
                                    key_name = 'macubuntu',
                                    security_groups = ['default'],
                                    client_token = dep.head.name + token,
                                    dry_run = False)
        instance = reserv.instances[0]
        status = instance.update()
        while status == 'pending':
            time.sleep(5)
            status = instance.update()
        if status == 'running':
            print '* Tagging instance: ', dep.head.name
            instance.add_tag('Name', dep.head.name)
        else:
            print '* ERROR: starting node', dep.head, status


@task
def inst_list(deployment):
    """List instances in each region
    """
    dep = deployments[deployment]
    connections = ec2.connect_all(*[x.region for x in dep.regions])
    instances = ec2.list_instances(*connections.values())
    count = 0
    for worker in instances:
        # ignore nodes named 'head'
        print '%s: %s at %s is %s(%d)' % ((worker.tags['Name'] if 'Name' in worker.tags else 'unnamed'),
                                          worker.id,
                                          worker.region.name,
                                          worker.state, worker.state_code)
        if worker.state_code == 16:
            count += 1
    print '* Total running instances = ', count


@task
def inst_terminate(deployment):
    """Terminate all instances in each region (except those named 'head')
    """
    dep = deployments[deployment]
    connections = ec2.connect_all(*[x.region for x in dep.regions])
    instances = ec2.list_instances(*connections.values())
    for worker in instances:
        # ignore nodes named 'head'
        if 'Name' in worker.tags and worker.tags['Name'] == 'head':
            continue
        print '* Terminating instance', worker.region.name, worker.id, worker.state
        worker.terminate()


@task
def inst_start(deployment, image_name, token='A'):
    """Start and configure instances
    """
    dep = deployments[deployment]
    regions = [x.region for x in dep.regions]
    connections = ec2.connect_all(*regions)
    images = {}
    reservations = {}

    # first check that image is available everywhere
    for region, conn in connections.items():
        imageid=None
        for img in ec2.list_images(conn, name=image_name):
            if img.state == u'available':
                imageid=img.id
                images[region] = imageid
        if not imageid:
            raise Exception('image not available in region ' + region)
    # start instances
    for dc in dep.regions:
        conn = connections[dc.region]
        for node in dc.nodes:
            print '* Starting node', node.name, ':', node
            reservation = conn.run_instances(images[node.region],
                                             instance_type = node.type,
                                             placement = node.region + node.zone,
                                             key_name = 'macubuntu',
                                             security_groups = ['default'],
                                             client_token = node.name + token,
                                             # user_data =
                                             dry_run = False)
            reservations[node.name] = (node, reservation)
    # wait for instances to be running and tag them
    print '**********************'
    for node, reserv in reservations.values():
        instance = reserv.instances[0] # each reservation is for a single instance
        status = instance.update()
        while status == 'pending':
            time.sleep(5)
            status = instance.update()
        if status == 'running':
            print '* Tagging instance: ', node.name
            instance.add_tag('Name', node.name)
        else:
            print '* ERROR: starting node', node, status
    print '* DONE!'


def gen_nodes(deployment):
    """Generate nodes.sh contents
    """
    dep = deployments[deployment]
    connections = ec2.connect_all(*[x.region for x in dep.regions])
    instances = ec2.list_instances(*connections.values())

    servers = defaultdict(list)
    clients = defaultdict(list)
    result = []
    for worker in instances:
        if worker.state_code != 16:
            continue
        if 'Name' in worker.tags:
            name = worker.tags['Name']
            if name == 'head':
                result.append('export ZKHOST=%s' % (worker.dns_name))
                continue
            elif name.startswith('rep') or name.startswith('acc'):
                servers[name[3]].append(worker)
            elif name.startswith('cli'):
                clients[name[3]].append(worker)
            result.append('export DC%s_%s_%s=%s' % (name[3],
                                                    name[:3].upper(),
                                                    name[5],
                                                    worker.dns_name))
    for dc,serv in servers.items():
        result.append('export DC%s_SERVERS=(' % (dc))
        for s in serv:
            result.append(s.dns_name)
        result.append(')')
    for dc,cli in clients.items():
        result.append('export DC%s_CLIENTS=(' % (dc))
        for c in cli:
            result.append(c.dns_name)
        result.append(')')
    result.append('\n')
    return '\n'.join(result)


@parallel
@roles(['server', 'client'])
def whoami_create():
    """Create a whoami.sh file in the ~ of the remote machine.
    Contains variables regarding the local machine
    """
    run('rm -f ~/whoami.sh')
    run('echo export ZKHOST=%s >> ~/whoami.sh' % env.roledefs['head'][0])
    run('echo -n export NAME= >> ~/whoami.sh')
    run('ec2din --region \$REGION \$INSTANCE | grep Name | cut -f 5 >> ~/whoami.sh')

    run('echo -n export ID= >> ~/whoami.sh')
    run('echo \\\`echo \\\$NAME \| cut -c6\\\` >> ~/whoami.sh')

    run('echo -n export RING= >> ~/whoami.sh')
    run('echo \\\`echo \\\$NAME \| cut -c4\\\` >> ~/whoami.sh')


@task
@parallel
@roles(['server', 'client'])
def config_put():
    """Push dht and storage config files to ec2 instances"""
    put('*.config', '/home/ubuntu/')


def create_dht_config():
    """Generate dht and storage config files"""
    # create dht config files
    local('echo ch.usi.paxosfs.storage.HttpStorage > storage.config')
    for dc in range(1, 4):
        # hosts and ports
        local('cat nodes.sh | grep DC%s_REP | sort | cut -d= -f2 > dhthosts' % (dc))
        local('seq 15100 100 15300 > dhtports')
        # REPLICATION LEVEL
        local('echo replication = 1 > dht%s.config' % dc)
        local('paste -d" " dhthosts dhtports >> dht%s.config' % (dc)) # create final dhtN.config
        # storage cfg
        local('seq 15101 100 15301 > dhtports') # http ports are +1
        local('paste -d":" dhthosts dhtports >> storage')
        local('sed -e "s/\(.*\)/%s http:\/\/\\1/g" storage >> storage.config' % (dc))
        local('rm -f dhtports dhthosts storage')


@parallel
@roles(['server', 'client'])
def hostname_set():
    sudo('[[ -f ~/whoami.sh ]] && hostname `echo $NAME | tr _ -`')
    sudo('[[ -f ~/whoami.sh ]] && echo 127.0.1.1 `echo $NAME | tr _ -` >> /etc/hosts')


@task
def inst_config(deployment):
    """Generate config files and deploy to running instances
    """
    execute(set_roles, deployment)
    with open('nodes.sh', 'w') as f:
        f.write(gen_nodes(deployment))
    with settings(roles = ['head']):
        execute(lambda: put('nodes.sh', '~/'))
    pprint(dict(env.roledefs))
    execute(whoami_create)
    execute(create_dht_config)
    execute(config_put)
    execute(hostname_set)
