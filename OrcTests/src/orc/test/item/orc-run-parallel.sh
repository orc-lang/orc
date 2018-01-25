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

substitute_orc_keywords ()
{
  result="$@"
  result="${result//#Orc:timestamp#/${job_status_timestamp}}"
  printf "%s\n" "$result"
}

get_job_attr_substituted ()
{
  result="$(get_job_attr $@)"
  result="$(substitute_orc_keywords $result)"
  printf "%s\n" "$result"
}

main ()
{

  ${CONDOR_CHIRP} ulog "Greetings and felicitations from node ${_CONDOR_PROCNO} of ${_CONDOR_NPROCS}, running $0 on $(uname -n)"

  job_status_timestamp="$(get_job_attr_at_submit EnteredCurrentStatus)"

  listen_sockaddr="$(get_job_attr_substituted ListenerSockAddrFile)"
  max_wait_listeners="$(get_job_attr ListenerWaitTimeout)"

  JavaCmd="$(get_job_attr JavaCmd)"
  JavaClassPath="$(get_job_attr JavaClassPath)"
  JavaVMArguments="$(get_job_attr JavaVMArguments)"
  JavaMainClassNode0="$(get_job_attr JavaMainClassNode0)"
  JavaMainClassOtherNodes="$(get_job_attr JavaMainClassOtherNodes)"
  JavaMainArgumentsNode0="$(get_job_attr_substituted JavaMainArgumentsNode0)"
  JavaMainArgumentsOtherNodes="$(get_job_attr_substituted JavaMainArgumentsOtherNodes)"

  if [[ ${_CONDOR_PROCNO} -ne 0 ]]; then

    # We're a follower

    started_waiting=$(date +"%s")

    while [[ ! -e ${listen_sockaddr} || $(grep -c '$' ${listen_sockaddr}) -ne 1 ]]; do
      sleep 1
      if [[ $(( $(date +"%s") - ${started_waiting} )) -gt ${max_wait_listeners} ]]; then
        errmsg="Timed out waiting for leader to write its listen socket address.  Waited ${max_wait_listeners} seconds for one line in ${listen_sockaddr}."
        echo "${errmsg}" 1>&2
        ${CONDOR_CHIRP} ulog "${errmsg}"
        exit 1
      fi
    done

    leaderListenAddress=$(cat ${listen_sockaddr})

    ${CONDOR_CHIRP} ulog "Starting Orc follower node ${_CONDOR_PROCNO}"

    echo ${JavaCmd} -cp ${JavaClassPath} ${JavaVMArguments} ${JavaMainClassOtherNodes} ${JavaMainArgumentsOtherNodes//#Orc:leaderListenAddress#/${leaderListenAddress}} 1>&2
    exec ${JavaCmd} -cp ${JavaClassPath} ${JavaVMArguments} ${JavaMainClassOtherNodes} ${JavaMainArgumentsOtherNodes//#Orc:leaderListenAddress#/${leaderListenAddress}}

  else

    # We're the leader (node 0)

    ${CONDOR_CHIRP} ulog "Starting Orc leader"

    echo ${JavaCmd} -cp ${JavaClassPath} ${JavaVMArguments} ${JavaMainClassNode0} ${JavaMainArgumentsNode0} 1>&2
    exec ${JavaCmd} -cp ${JavaClassPath} ${JavaVMArguments} ${JavaMainClassNode0} ${JavaMainArgumentsNode0}

  fi
}

main "$@"
