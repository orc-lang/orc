#! /bin/bash

# Notes: (from jthywiss)
# The follower port numbers (second command-line arg) are hard-coded 
# until I get around to a discovery mechanism.
# At the moment, the value location policy is hard-coded in 
# orc/run/distrib/DOrcExecution.scala: ConcurrentSkipList operations 
# run on follower 1.

ROOT="$(readlink -f "$(dirname "$(readlink -f $0)")/../../..")"
echo "Found orc repo root at: $ROOT"

export CLASSPATH="$ROOT/OrcScala/build/orc-2.1.1.jar:$ROOT/OrcScala/lib/*"
ORCFILE="$ROOT/OrcTests/test_data/distrib/webregistration.orc"

echo "Using class path: $CLASSPATH"
echo "Using orc file: $ORCFILE"

# Follower 1:
java orc.run.distrib.FollowerRuntime 1 36721 &
FOLLOWER1=$!
# Follower 2:
java orc.run.distrib.FollowerRuntime 2 36722 &
FOLLOWER2=$!

# Leader:
java orc.Main --backend=distrib --java-stack-trace --noprelude $ORCFILE

echo "Killing followers... (TERM followed 2 seconds later by KILL)"
kill -TERM $FOLLOWER1 $FOLLOWER2
sleep 2
kill -KILL $FOLLOWER1 $FOLLOWER2 2> /dev/null
echo "Done."