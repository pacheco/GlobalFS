#!/bin/bash

SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPTDIR/const.sh

java -Djava.library.path=$LIBPATH ch.usi.paxosfs.client.PaxosFileSystem $@
