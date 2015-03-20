#!/bin/bash

OUTDIR=~/out-sinergia
DURATION=20

SMALL=32
LARGE=$[4*1024]
THREADS=(1 4 16 64)


### WRITES ####################

# global small
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqwrite:glob,${SMALL},${T},${DURATION},${OUTDIR}/global-s${SMALL}-t${T}
done


# global large
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqwrite:glob,${LARGE},${T},${DURATION},${OUTDIR}/global-s${LARGE}-t${T}
done

# local small
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqwrite:loc,${SMALL},${T},${DURATION},${OUTDIR}/local-s${SMALL}-t${T}
done

# local large
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqwrite:loc,${LARGE},${T},${DURATION},${OUTDIR}/local-s${LARGE}-t${T}
done


### READS ####################

fab -f fabfile-ec2exp.py \
    putfiles:glob,~/linux.tar.xz

fab -f fabfile-ec2exp.py \
    putfiles:loc,~/linux.tar.xz

# global small
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqread:glob,${SMALL},${T},${DURATION},${OUTDIR}/global-s${SMALL}-t${T}-read
done


# global large
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqread:glob,${LARGE},${T},${DURATION},${OUTDIR}/global-s${LARGE}-t${T}-read
done

# local small
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqread:loc,${SMALL},${T},${DURATION},${OUTDIR}/local-s${SMALL}-t${T}-read
done

# local large
for T in ${THREADS[@]}; do
    fab -f fabfile-ec2exp.py \
        do_seqread:loc,${LARGE},${T},${DURATION},${OUTDIR}/local-s${LARGE}-t${T}-read
done
