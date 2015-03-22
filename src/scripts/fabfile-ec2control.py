from fabric.api import *
from ec2helper import *
from ec2config import *
from pprint import pprint
import sys


env.use_ssh_config = True
env.colorize_errors = True
env.disable_known_hosts = True
env.roledefs = None

@task
def set_roles():
    env.roledefs = roledefs_from_instances() # get ips for the roledef lists from ec2 instances

@task
def print_roles():
    """Print instances grouped by roles
    """
    pprint(dict(env.roledefs))


@task
def head_start(image_name):
    """Start a machine with the given image
    """
    conn = connect(head_region)
    req = request_spot_instances(conn, image_name, head_type, head_price,
                                 count=1,
                                 availability_zone=head_zone)


def get_head_instance():
    """Return the head instance or None if it is not running
    """
    connections = connect_all(*regions)
    instances = list_instances(*connections.values())
    for worker in instances:
        # ignore head node
        if 'Name' in worker.tags and worker.tags['Name'] == 'head':
            return worker
    return None


@task
def head_status():
    """Check if the is a machine with name 'head' running
    """
    head = get_head_instance()
    if head:
        print "%s (%s) at %s: %s" % (head.id, head.instance_type, head.region.name, head.state)


@task
def image_delete(image_name):
    """List images available on each region
    """
    connections = connect_all(*regions)
    for region, conn in connections.iteritems():
        images = list_images(conn)
        print '%s:' % (region)
        for img in images:
            if img.name == image_name:
                print "removing", img
                img.deregister()


@task
def image_distribute(image_name):
    """Distribute an image availabe on the main (head) region to the other regions
    """
    region_origin = head_region
    other_regions = [r for r in regions if r != region_origin]
    name = 'head'

    connections = connect_all(*other_regions)
    for conn in connections.values():
        copy_image(conn, image_name, region_origin)


@task
def image_from_head(image_name):
    """Create a new snapshot image from the 'head' instance
    """
    connection = connect(head_region)
    create_snapshot_image(connection, 'head', image_name)


@task
def image_list():
    """List images available on each region
    """
    connections = connect_all(*regions)
    for region, conn in connections.iteritems():
        images = list_images(conn)
        print '%s:' % (region)
        for img in images:
            print ' %s %s - %s' % (img.name, img.id, img.state)


def gen_nodes():
    """Generate code for creating bash variables with the ips of the instances on ec2
    """
    result = []
    connections = connect_all(*regions)
    instances = list_instances(*connections.values())
    servers = defaultdict(list)
    clients = defaultdict(list)
    for worker in instances:
        if worker.state_code != 16:
            continue
        if 'Name' in worker.tags and worker.tags['Name'] == 'head':
            result.append('export ZKHOST=%s' % (worker.dns_name))
            continue
        if worker.tags['Type'] == 'server':
            servers[worker.tags['DC']].append(worker)
        else:
            clients[worker.tags['DC']].append(worker)

        result.append('export DC%s_%s_%s=%s' % (worker.tags['DC'],
                                        worker.tags['Name'][:3].upper(),
                                        worker.tags['Name'][-1:],
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


@task
def spot_terminate_all():
    """Terminate all worker spot instances (leaving the 'head' running)
    """
    connections = connect_all(*regions)
    instances = list_instances(*connections.values())
    for worker in instances:
        # ignore head node
        if 'Name' in worker.tags and worker.tags['Name'] == 'head':
            continue
        worker.terminate()


@task
def spot_list():
    """List running spot instances
    """
    connections = connect_all(*regions)
    instances = list_instances(*connections.values())
    for worker in instances:
        # ignore head node
        if 'Name' in worker.tags and worker.tags['Name'] == 'head':
            continue
        print '%s: %s at %s is %s(%d)' % ((worker.tags['Name'] if 'Name' in worker.tags else 'unnamed'),
                                          worker.id,
                                          worker.region.name,
                                          worker.state, worker.state_code)


@task
def spot_request_list():
    """List spot requests and their status
    """
    connections = connect_all(*regions)
    requests = list_spot_requests(*connections.values())
    for req in requests:
        print '%s at %s: %s' % (req.id, req.region.name, req.status.message)


def count_spot(conn, only_running=True):
    """Returns the number of non-head instances in this region. If
    only_running is True only counts instances that are in the running
    state.

    """
    count = 0
    instances = list_instances(conn)
    for i in instances:
        # ignore head node
        if 'Name' in i.tags and i.tags['Name'] == 'head':
            continue
        elif only_running and i.state_code != 16:
            continue
        count += 1
    return count


@task
def spot_start(image_name):
    """Spin up all spot instances with the given image
    """
    connections = connect_all(*regions)
    for region, zone, price in zip(regions, regions_zones, regions_prices):
        conn = connections[region]
        to_start = instances_per_region - count_spot(conn)
        print region, to_start
        if to_start > 0:
            request_spot_instances(conn, image_name, instance_type, price,
                                   count=to_start,
                                   availability_zone=zone)
    for conn in connections.values():
        conn.close()


def spot_tag():
    """Tags the instances given the required roles.
    Should be called after spot_start and only after all instances are up/running
    """
    connections = connect_all(*regions)
    for rid,r in enumerate(regions):
        instances = list_instances(connections[r])
        instances = [i for i in instances if
                     ('Name' not in i.tags or i.tags['Name'] != 'head') and (i.state_code == 16)]
        instances[0].add_tag('Name', 'acc%s_0' % (rid+1))
        for i, inst in enumerate(instances[1:4]):
            inst.add_tag('Name', 'rep%s_%s' % (rid+1, i))
        for i, inst in enumerate(instances[4:]):
            inst.add_tag('Name', 'cli%s_%s' % (rid+1, i))

        connections[r].create_tags([i.id for i in instances[0:4]], {'Type': 'server', 'DC': rid+1})
        connections[r].create_tags([i.id for i in instances[4:]], {'Type': 'client', 'DC': rid+1})


@task
@parallel
@roles(['replica', 'client'])
def whoami_create():
    """Create a whoami.sh file in the ~ of the remote machine.
    Contains variables regarding the local machine
    """
    run('rm -f ~/whoami.sh')
    head = get_head_instance()
    if not head:
        raise "'head' instance not running!"
    run('echo export ZKHOST=%s >> ~/whoami.sh' % head.dns_name)
    run('echo -n export NAME= >> ~/whoami.sh')
    run('ec2din --region \$REGION \$INSTANCE | grep Name | cut -f 5 >> ~/whoami.sh')

    run('echo -n export ID= >> ~/whoami.sh')
    run('echo \\\`echo \\\$NAME \| cut -c6\\\` >> ~/whoami.sh')

    run('echo -n export RING= >> ~/whoami.sh')
    run('echo \\\`echo \\\$NAME \| cut -c4\\\` >> ~/whoami.sh')


@task
@parallel
@roles(['replica', 'client'])
def hostname_set():
    sudo('[[ -f ~/whoami.sh ]] && hostname `echo $NAME | tr _ -`')
    sudo('[[ -f ~/whoami.sh ]] && echo 127.0.1.1 `echo $NAME | tr _ -` >> /etc/hosts')


def mount_ssd():
    """Mount instance SSD drives at /ssd/storage
    """
    sudo('echo -e "o\nn\np\n1\n\n\nw" | fdisk /dev/xvdb')
    sudo('mkfs.ext4 /dev/xvdb1')
    sudo('mkdir -p /ssd')
    sudo('mount -text4 /dev/xvdb1 /ssd')
    sudo('mkdir -p /ssd/storage')
    sudo('chmod 777 /ssd/storage')


@task
@parallel
@roles(['replica', 'client'])
def put_dht_config():
    """Push dht and storage config files to ec2 instances"""
    put('*.config', '/home/ubuntu/')


@task
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


@task
def spot_setup_all():
    """Setup instances after they are running:
    - Tags them
    - Generates 'nodes.sh' with the instance ips. Copy it to 'head'
    - Generates 'whoami.sh' scripts on the instances
    - Mounts ssd drives on the instances
    """
    spot_tag()
    execute(set_roles)
    with open('nodes.sh', 'w') as f:
        f.write(gen_nodes())
    with settings(roles = ['head']):
        execute(lambda: put('nodes.sh', 'out/'))
    pprint(dict(env.roledefs))
    execute(create_dht_config)
    execute(put_dht_config)
    execute(whoami_create)
    execute(hostname_set)
    #execute(mount_ssd)
