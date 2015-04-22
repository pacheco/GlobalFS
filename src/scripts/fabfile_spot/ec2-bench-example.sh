#!/bin/bash

OUTDIR=$1

if [[ -z "$OUTDIR" ]]; then
    >&2 echo "script.sh <output_dir>"
    exit 1
fi

DURATION=30
SMALL=32
LARGE=$[4*1024]
THREADS=(1 4 16 64)


### WRITES ####################

# global small
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqwrite:glob,${SMALL},${T},${DURATION},${OUTDIR}/write-global-s${SMALL}-t${T}
done


# global large
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqwrite:glob,${LARGE},${T},${DURATION},${OUTDIR}/write-global-s${LARGE}-t${T}
done

# local small
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqwrite:loc,${SMALL},${T},${DURATION},${OUTDIR}/write-local-s${SMALL}-t${T}
done

# local large
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqwrite:loc,${LARGE},${T},${DURATION},${OUTDIR}/write-local-s${LARGE}-t${T}
done


### READS ####################

#
# Create files used by the read benchmarks
#

fab -f fabfile-ec2exp.py \
    putfiles:glob,1024,100,64

fab -f fabfile-ec2exp.py \
    putfiles:loc,1024,1024,64

# global small
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqread:glob,${SMALL},${T},${DURATION},${OUTDIR}/read-global-s${SMALL}-t${T}
done


# global large
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqread:glob,${LARGE},${T},${DURATION},${OUTDIR}/read-global-s${LARGE}-t${T}
done

# local small
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqread:loc,${SMALL},${T},${DURATION},${OUTDIR}/read-local-s${SMALL}-t${T}
done

# local large
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqread:loc,${LARGE},${T},${DURATION},${OUTDIR}/read-local-s${LARGE}-t${T}
done
