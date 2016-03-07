#!/bin/bash

unset DB
unset LAT
unset EC2
unset ZOO

exit_usage() {
    >&2 echo "mrpnode.sh --zoo ZKHOST [--lat LAT] [--db DB_PATH] [--ip IP] ring,id:roles[;ring,id:roles]"
    exit 1
}

while [[ $# > 1 ]]
do
key="$1"

case $key in
    --zoo)
        ZOO="$2"
        shift
    ;;
   --lat)
        LAT="$2"
        shift
    ;;
    --db)
        DB="$2"
        shift
    ;;
    --ip)
        EC2="$2"
        shift
    ;;
    *)
        # unknown argument
        exit_usage
    ;;
esac
shift
done

if [[ -z $1 ]] || [[ -z $ZOO ]]; then
    # mrp args not passed
    exit_usage
fi

PRGDIR=`dirname "$0"`
CLASSPATH="$PRGDIR":"$CLASSPATH"
JVM_OPTS="$JVM_OPTS -XX:+UseParallelGC -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/tmp/java-$$.vgc"
CMD="java -cp $CLASSPATH -Xms2G -Xmx2G $JVM_OPTS ch.usi.da.paxos.TTYNode $1 $ZOO"

# add the env variables if the parameters were passed
if [[ -n $EC2 ]]; then
    CMD="EC2=$EC2 $CMD"
fi
if [[ -n $LAT ]]; then
    CMD="LAT=$LAT $CMD"
fi
if [[ -n $DB ]]; then
    CMD="DB=$DB $CMD"
fi

# run MRP node
echo $CMD
eval $CMD
