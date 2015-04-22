#!/bin/bash

SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPTDIR/const.sh

java -Xms1G -Xmx3G ch.usi.paxosfs.replica.FSMain $@
