#!/usr/bin/env python

import boto.ec2
import itertools
from collections import defaultdict

def connect(region):
    """connect to a region by name"""
    return boto.ec2.connect_to_region(region)


def connect_all(*regions):
    """connect to multiple regions by name. Returns a dict of name to connection"""
    return {r: connect(r) for r in regions}


def list_instances(*ec2conn):
    """list instance through the connections passed as argument"""
    result = []
    for conn in ec2conn:
        instances = conn.get_all_instances()
        result+= itertools.chain(*[i.instances for i in instances])
    return result


def find_instance_by_name(conn, name):
    """return the instance with tag 'Name' == name or None"""
    instances = list_instances(conn)
    try:
        return next(i for i in instances if ('Name' in i.tags) and (i.tags['Name'] == name))
    except StopIteration:
        return None


def list_images(conn, name=None):
    """get image id by its name"""
    if name:
        images = conn.get_all_images(owners = ['self'], filters = {'name': name})
    else:
        images = conn.get_all_images(owners = ['self'])

    return images


def create_snapshot_image(conn, instance_name, image_name):
    """create a snapshot from some running instance with the given 'Name' instance_name"""
    instance = find_instance_by_name(conn, instance_name)
    instance.create_image(image_name)


def copy_image(conn, image_name, from_region):
    """copy an image from the given region"""
    fromconn = connect(from_region)
    image = list_images(fromconn, image_name)[0]
    conn.copy_image(from_region, image.id, name=image.name)
    fromconn.close()


def list_region_zones(*region_names):
    """list availability zones for the given regions"""
    connections = connect_all(*region_names)
    for region, conn in connections.iteritems():
        print '%s:' % (region)
        for zone in conn.get_all_zones():
            print '\t%s' % (zone.name)


def request_instances(conn, image_name, instance_type, price,
                           count=1,
                           availability_zone=None,
                           key_name='macubuntu',
                           security_groups=['default'],
                           placement_group=None):
    """request spot instances using the given connection"""
    # if availability_zone:
    #     zones = conn.get_all_zones(zones=[availability_zone])
    #     placement = zones[0]
    placement = availability_zone
    imageid=None
    for img in list_images(conn, name=image_name):
        if img.state == u'available':
            imageid=img.id
    if not imageid:
        raise Exception('no image available')
    return conn.request_spot_instances(price, imageid, count=count,
                                       placement = placement,
                                       placement_group = placement_group,
                                       key_name=key_name,
                                       instance_type = instance_type,
                                       security_groups = security_groups,
                                       dry_run=False)

