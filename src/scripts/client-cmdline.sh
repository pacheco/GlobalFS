#!/bin/bash

SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPTDIR/const.sh

java ch.usi.paxosfs.client.CommandLineClient $@
