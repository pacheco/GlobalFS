from fabric.api import *
from deployments import *
import random
import time
import cluster


env.use_ssh_config = True
env.colorize_errors = True
env.disable_known_hosts = True
env.roledefs = None

@task
def set_roles_cluster(partitions):
    """Set fabric roles from cluster nodes
    """
    env.roledefs = cluster.get_roles_cluster(partitions)

@task
def set_roles_ec2(deployment):
    """Set fabric roles from running instances
    """
    env.roledefs = roledefs_from_instances(deployment) # get ips for the roledef lists from ec2 instances


def results_ok(results):
    """Returns True if all return values are for succeed"""
    for r in results.values():
        if r.failed:
            return False
    return True


def dtach_and_log(command, dtach_socket, logfile):
    """Generate a command to leave the program running in the background
    with its output copied to a logfile.

    """
    return 'dtach -n %s bash -c "%s | tee %s"' % (dtach_socket, command, logfile)


@task
@parallel
@roles('client', 'server')
def dstat():
    """Use dstat to start logging statistics
    """
    with settings(warn_only=True):
        run('pkill -f dstat')
        run('rm /tmp/dstat')
    run(dtach_and_log("dstat -C0,1 -cinmy --output /tmp/dstat.$(hostname)", "/tmp/dstat", "/dev/null"))


@task
@parallel
@roles('client', 'server')
def dstat_results(outdir):
    """Kill running dstat and copy results
    """
    with settings(warn_only=True):
        run('pkill -f dstat')
        run('rm /tmp/dstat')
    local('mkdir -p %s' % (outdir))
    hostname = run('hostname')
    get('/tmp/dstat.%s' % (hostname), outdir)


@parallel
@roles('client')
def clearresult():
    """Clear old results"""
    run('rm -f /tmp/cli*')


@task
@parallel
@roles('client')
def copyresult(outdir):
    """Copy results out"""
    local('mkdir -p %s' % (outdir))
    with settings(warn_only=True):
        local('rsync -avzr %s:/tmp/cli* %s' % (env.host_string, outdir))


@task
@roles('singleclient')
def ensuredirs():
    """Create benchmark directories
    """
    run('mkdir -p /tmp/fs/{1,2,3,4,5,6,7,8,9,10,11,12,g}')


@parallel
@roles('client')
def putfiles(opertype, bsize, bcount, nthreads):
    """Use dd to create files to be used by the read benchmark.
    """
    destfile = opertype_file(opertype)
    run('parallel -j %s dd if=/dev/urandom of=%s{} bs=%s count=%s ::: `seq 0 %s`' %
        (nthreads,
         '/tmp/fs/' + destfile,
         bsize,
         bcount,
         int(nthreads) - 1))


@parallel
@roles('client')
def populate(file, block_size, nblocks, nfiles):
    """Populate files for read bench
    """
    with settings(warn_only=True):
        return run('~/usr/sinergiafs-clients/populate %s %s %s %s' %
                   (file, block_size, nblocks, nfiles))


@parallel
@roles('client')
def create(file, filesize, threads, duration, log):
    """Run create file benchmark
    """
    with settings(warn_only=True):
        return run('~/usr/sinergiafs-clients/create %s %s %s %s %s %s' %
                   (file, filesize, threads, threads, duration, log))


@parallel
@roles('client')
def seqread(file, readsize, threads, duration, log):
    """Run sequential write benchmark
    """
    # TODO check file exists on remote
    with settings(warn_only=True):
        return run('~/usr/sinergiafs-clients/read %s %s %s %s %s %s' %
                   (file, readsize, threads, threads, duration, log))


@parallel
@roles('client')
def seqwrite(file, writesize, threads, duration, log):
    """Run sequential write benchmark
    """
    with settings(warn_only=True):
        return run('~/usr/sinergiafs-clients/write %s %s %s %s %s %s' %
                   (file, writesize, threads, threads, duration, log))


def opertype_file(opertype, name):
    """Get the filename path to be used for a given operation type"""
    if opertype == 'loc':
        filename = '${RING}/file%s' % (name)
    elif opertype == 'glob':
        filename = 'g/${RING}_file%s' % (name)
    elif opertype == 'rem':
        raise Exception("not implemente")
        # here we would need a n_partition parameter to do the modulo
        #filename = '$[(RING+1) % 3]/file%s' % (name)
    else:
        filename = None
    return filename


@task
def do_populate(opertype, name, block_size, nblocks, nfiles):
    """
    (loc|glob|rem, fname, block_size, nblocks, nfiles)
    """
    filename = opertype_file(opertype, name)
    if not filename:
        print "choose an operation type [loc | glob | rem]"
        return
    execute(populate, '/tmp/fs/' + filename, block_size, nblocks, nfiles)


@task
def do_seqread(opertype, name, readsize, threads, duration, outdir):
    """
    (loc|glob|rem, readsize, threads, duration, outdir)
    """
    filename = opertype_file(opertype, name)
    if not filename:
        print "choose an operation type [loc | glob | rem]"
        return
    execute(clearresult)
    execute(dstat)
    results = execute(seqread, '/tmp/fs/' + filename, readsize, threads, duration, '/tmp/cli${RING}_${ID}_')
    if results_ok(results):
        execute(copyresult, outdir)
    else:
        with open('./FAILED_RUNS', 'a') as f:
            f.write(outdir)
            f.write('\n')
    execute(dstat_results, outdir + '/dstat')


@task
def do_seqwrite(opertype, writesize, threads, duration, outdir):
    """
    (loc|glob|rem, writesize, threads, duration, outdir)
    """
    filename = opertype_file(opertype, str(random.randint(0,999999)))
    if not filename:
        print "choose an operation type [loc | glob | rem]"
        return
    execute(clearresult)
    execute(dstat)
    results = execute(seqwrite, '/tmp/fs/' + filename, writesize, threads, duration, '/tmp/cli${RING}_${ID}_')
    if results_ok(results):
        execute(copyresult, outdir)
    else:
        with open('./FAILED_RUNS', 'a') as f:
            f.write(outdir)
            f.write('\n')
    execute(dstat_results, outdir + '/dstat')


@task
def do_create(opertype, filesize, threads, duration, outdir):
    """
    (loc|glob|rem, filesize, threads, duration, outdir)
    """
    filename = opertype_file(opertype, str(random.randint(0,999999)))
    if not filename:
        print "choose an operation type [loc | glob | rem]"
        return
    execute(clearresult)
    execute(dstat)
    results = execute(create, '/tmp/fs/' + filename, filesize, threads, duration, '/tmp/cli${RING}_${ID}_')
    if results_ok(results):
        execute(copyresult, outdir)
    else:
        with open('./FAILED_RUNS', 'a') as f:
            f.write(outdir)
            f.write('\n')
    execute(dstat_results, outdir + '/dstat')
