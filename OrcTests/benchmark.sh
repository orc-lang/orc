#! /bin/bash
ROOTDIR="$(readlink -f "$(dirname "$0")/..")"

SCALALIBS=$ROOTDIR/OrcScala/lib/scala-library.jar:$ROOTDIR/OrcScala/lib/scala-parser-combinators_2.12-1.0.5.jar:$ROOTDIR/OrcScala/lib/scala-reflect.jar:$ROOTDIR/OrcScala/lib/scala-xml_2.12-1.0.6.jar
ORCSCALALIBS=$ROOTDIR/OrcScala/build/orc-2.1.1.jar:$ROOTDIR/OrcScala/lib/swivel_2.12-0.0.1.jar
ORCTESTSLIBS=$ROOTDIR/OrcTests/build:$ROOTDIR/OrcTests/tools/junit-4.5.jar
PORCELIBS=$ROOTDIR/PorcE/bin:$ROOTDIR/PorcE/lib/truffle-api.jar

GRAALJAVA=""
. $ROOTDIR/OrcTests/benchmark-localconfig.sh
if [ "$GRAALJAVA" = "" ]; then
    echo "ERROR: benchmark-localconfig.sh (in $ROOTDIR/OrcTests) must set GRAALJAVA to the absolute path to the GraalVM java executable."
    exit 1
fi

NORMALJAVA=java

CPUS=8
NRUNS=2
TIMEOUT=60

BATCHNAME="$(hostname)_$(date +"%Y%m%d-%H%M")"

echo "NOTE: Don't forget to build everything in eclipse first. Ideally with a clean git status."
echo "NOTE: Don't forget to set CPUs to a fixed frequency!"
sleep 2

# exec > $ROOTDIR/benchmark_run_$BATCHNAME.log

for config in "porceinterp:$NORMALJAVA:orc:porc:2"   \
#                  "porcegraal:$GRAALJAVA:orc:porc:2" \
                  "token:$NORMALJAVA:orc:token:2"    \
                  "scala:$NORMALJAVA:scala:token:2"; do
    IFS=':' read -r -a configs <<< "$config"
    NAME="${configs[0]}"
    JAVA="${configs[1]}"
    SET="${configs[2]}"
    BACKEND="${configs[3]}"
    OPT="${configs[4]}"

    cd $ROOTDIR/OrcTests
    echo "Starting $config"
    time "$JAVA" -classpath "$SCALALIBS:$ORCSCALALIBS:$ORCTESTSLIBS:$PORCELIBS" orc.test.BenchmarkTest \
            -O $OPT -S $SET -B $BACKEND -t $TIMEOUT -c $CPUS -r $NRUNS \
            -o $ROOTDIR/benchmark_data_${BATCHNAME}_${NAME}.tsv \
            || exit 2
    echo "Finished $config"
done



