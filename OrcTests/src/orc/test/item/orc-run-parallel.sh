#! /bin/bash

# orc-run-parallel.sh -- Bash shell script to run Orc in the HTCondor batch system's Parallel universe
# Project OrcTest
#
# Created by jthywiss on Sep 8, 2017.
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

set -o nounset

CONDOR_CHIRP="$(condor_config_val libexec)/condor_chirp"

cleanfilename ()
{
  ( shopt -s extglob
    clean="${1//+([!A-Za-z0-9._-])/_}"
    trimfront="${clean/#+(_)}"
    trimback="${trimfront/%+(_)}"
    printf "%s\n" "$trimback"
  )
}

get_job_attr ()
{
  awk "/^[ \\t]*$@[ \\t]*=[ \\t]*/ { sub(\"^[ \\t]*$@[ \\t]*=[ \\t]*\\\"?\",\"\"); sub(\"\\\"?[ \\t]*\$\",\"\"); print \$0 }" <${_CONDOR_JOB_AD}
}

get_job_attr_at_submit ()
{
  quoted="$(${CONDOR_CHIRP} get_job_attr $@)"
  trimfront="${quoted/#\"}"
  trimback="${trimfront/%\"}"
  printf "%s\n" "$trimback"
}


main ()
{

  ${CONDOR_CHIRP} ulog "Greetings and felicitations from node ${_CONDOR_PROCNO} of ${_CONDOR_NPROCS}, running $0 on $(uname -n)"

  clusterid="$(get_job_attr ClusterId)"
  job_status_timestamp="$(get_job_attr_at_submit EnteredCurrentStatus)"

  listen_sockaddr="$(get_job_attr ListenerSockAddrFile)"
  max_wait_listeners="$(get_job_attr ListenerWaitTimeout)"
  base_port_number="$(get_job_attr OrcPortBase)"

  JavaCmd="$(get_job_attr JavaCmd)"
  JavaClassPath="$(get_job_attr JavaClassPath)"
  JavaVMArguments="$(get_job_attr JavaVMArguments)"
  JavaMainClassNode0="$(get_job_attr JavaMainClassNode0)"
  JavaMainClassOtherNodes="$(get_job_attr JavaMainClassOtherNodes)"
  JavaMainArgumentsNode0="$(get_job_attr JavaMainArgumentsNode0)"
  JavaMainArgumentsOtherNodes="$(get_job_attr JavaMainArgumentsOtherNodes)"

  host_fqdn="$(hostname -f)"
  port=$(( ${base_port_number} + ${_CONDOR_PROCNO} ))

  echo "${clusterid} ${job_status_timestamp} ${_CONDOR_PROCNO} ${host_fqdn} ${port}" | ${CONDOR_CHIRP} put -mode cwa - ${listen_sockaddr}

  if [[ ${_CONDOR_PROCNO} -ne 0 ]]; then

    # We're a follower

    ${CONDOR_CHIRP} ulog "Starting Orc follower node ${_CONDOR_PROCNO}"

    echo ${JavaCmd} -cp ${JavaClassPath} ${JavaVMArguments} ${JavaMainClassOtherNodes} ${JavaMainArgumentsOtherNodes//#Orc:port#/${port}} 1>&2
    exec ${JavaCmd} -cp ${JavaClassPath} ${JavaVMArguments} ${JavaMainClassOtherNodes} ${JavaMainArgumentsOtherNodes//#Orc:port#/${port}}


  else

    # We're the leader (node 0)

    # Wait for all followers to post their locations
    # This listener-locations file poll loop is based on the idea in HTCondor's condor_scripts/sshd.sh

    started_waiting=$(date +"%s")

    while [[ $(grep -c "^${clusterid} ${job_status_timestamp} " ${listen_sockaddr}) -ne ${_CONDOR_NPROCS} ]]; do
      sleep 1
      if [[ $(( $(date +"%s") - ${started_waiting} )) -gt ${max_wait_listeners} ]]; then
        errmsg="Timed out waiting for all listeners to post their locations.  Found $(grep -c "^${clusterid} ${job_status_timestamp} " ${listen_sockaddr}) of ${_CONDOR_NPROCS} nodes, waited ${max_wait_listeners} seconds."
        echo "${errmsg}" 1>&2
        ${CONDOR_CHIRP} ulog "${errmsg}"
        exit 1
      fi
    done

    follower_sockets=$(grep "^${clusterid} ${job_status_timestamp} " ${listen_sockaddr} | sed "s/^${clusterid} ${job_status_timestamp} //" | sort -n | awk ' NR > 2 { printf(",") } NR > 1 { printf("%s:%s", $2, $3) } END { printf("\n") }')

    ${CONDOR_CHIRP} ulog "All $(( ${_CONDOR_NPROCS} - 1 )) followers found, Starting Orc leader"

    rm ${listen_sockaddr}

    echo ${JavaCmd} -cp ${JavaClassPath} ${JavaVMArguments} ${JavaMainClassNode0} ${JavaMainArgumentsNode0//#Orc:followerSockets#/${follower_sockets}} 1>&2
    exec ${JavaCmd} -cp ${JavaClassPath} ${JavaVMArguments} ${JavaMainClassNode0} ${JavaMainArgumentsNode0//#Orc:followerSockets#/${follower_sockets}}

  fi
}

main "$@"
