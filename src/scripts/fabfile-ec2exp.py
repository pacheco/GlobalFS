from fabric.api import *
from ec2config import roledefs_from_instances
import time


env.use_ssh_config = True
env.colorize_errors = True
env.disable_known_hosts = True
env.roledefs = roledefs_from_instances() # get ips for the roledef lists from ec2 instances


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


@parallel
@roles('client')
def clearresult():
    """Clear old results"""
    run('rm -f /tmp/cli*')


@parallel
@roles('client')
def copyresult(outdir):
    """Copy results out"""
    local('mkdir -p %s' % (outdir))
    get('/tmp/cli*', outdir)


@task
@roles('singleclient')
def ensuredirs():
    """Create benchmark directories
    """
    run('mkdir -p /tmp/fs/{1,2,3,g}')


@task
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
def seqwrite(file, writesize, threads, duration, log):
    """Run sequential write benchmark
    """
    with settings(warn_only=True):
        return run('~/usr/sinergiafs-clients/seq-write %s %s %s %s %s' %
                   (file, writesize, threads, duration, log))


@parallel
@roles('client')
def create(file, filesize, threads, duration, log):
    """Run create file benchmark
    """
    with settings(warn_only=True):
        return run('~/usr/sinergiafs-clients/create %s %s %s %s %s' %
                   (file, filesize, threads, duration, log))


@parallel
@roles('client')
def randwrite(file, writesize, filesize, threads, duration, log):
    """Run sequential write benchmark
    """
    with settings(warn_only=True):
        return run('~/usr/sinergiafs-clients/rand-write %s %s %s %s %s %s' %
                   (file, writesize, filesize, threads, duration, log))


@parallel
@roles('client')
def seqread(file, readsize, threads, duration, log):
    """Run sequential write benchmark
    """
    # TODO check file exists on remote
    with settings(warn_only=True):
        return run('~/usr/sinergiafs-clients/seq-read %s %s %s %s %s' %
                   (file, readsize, threads, duration, log))


def opertype_file(opertype):
    """Get the filename to be used for a given operation type"""
    if opertype == 'loc':
        filename = '${RING}/file'
    elif opertype == 'glob':
        filename = 'g/file${RING}_'
    elif opertype == 'rem':
        filename = '$[(RING+1) % 3]/file'
    else:
        filename = None
    return filename

@task
def do_seqwrite(opertype, writesize, threads, duration, outdir):
    """
    (loc|glob|rem, writesize, threads, duration, outdir)
    """
    filename = opertype_file(opertype)
    if not filename:
        print "choose an operation type [loc | glob | rem]"
        return
    execute(clearresult)
    results = execute(seqwrite, '/tmp/fs/' + filename, writesize, threads, duration, '/tmp/${NAME}_')
    if results_ok(results):
        execute(copyresult, outdir)
    else:
        with open('./FAILED_RUNS', 'a') as f:
            f.write(outdir)
            f.write('\n')


@task
def do_create(opertype, filesize, threads, duration, outdir):
    """
    (loc|glob|rem, filesize, threads, duration, outdir)
    """
    filename = opertype_file(opertype)
    if not filename:
        print "choose an operation type [loc | glob | rem]"
        return
    execute(clearresult)
    results = execute(create, '/tmp/fs/' + filename, filesize, threads, duration, '/tmp/${NAME}_')
    if results_ok(results):
        execute(copyresult, outdir)
    else:
        with open('./FAILED_RUNS', 'a') as f:
            f.write(outdir)
            f.write('\n')


@task
def do_randwrite(opertype, writesize, filesize, threads, duration, outdir):
    """
    (loc|glob|rem, writesize, threads, duration, outdir)
    """
    filename = opertype_file(opertype)
    if not filename:
        print "choose an operation type [loc | glob | rem]"
        return
    execute(clearresult)
    results = execute(randwrite, '/tmp/fs/' + filename, writesize, filesize, threads, duration, '/tmp/${NAME}_')
    if results_ok(results):
        execute(copyresult, outdir)
    else:
        with open('./FAILED_RUNS', 'a') as f:
            f.write(outdir)
            f.write('\n')


@task
def do_seqread(opertype, readsize, threads, duration, outdir):
    """
    (loc|glob|rem, readsize, threads, duration, outdir)
    """
    filename = opertype_file(opertype)
    if not filename:
        print "choose an operation type [loc | glob | rem]"
        return
    execute(clearresult)
    results = execute(seqread, '/tmp/fs/' + filename, readsize, threads, duration, '/tmp/${NAME}_')
    if results_ok(results):
        execute(copyresult, outdir)
    else:
        with open('./FAILED_RUNS', 'a') as f:
            f.write(outdir)
            f.write('\n')
