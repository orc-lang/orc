#! /bin/sh

# get-new-run-seq-num.sh -- Shell script to atomically get a new run sequence number
# Project OrcTest
#
# Created by jthywiss on Sep 16, 2017.
#
# Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
#
# Use and redistribution of this file is governed by the license terms in
# the LICENSE file found in the project's top-level directory and also found at
# URL: http://orc.csres.utexas.edu/license.shtml .
#

set -u
set +C

progname=$(basename "$0")

today=$(date "+%Y%m%d")
# epoch year "a" = 2004, year of first Orc paper
yearLetter=$(printf "\x$(printf %x $(( 97 + $(date -u +%Y) - 2004 )) )")
dayOfYear=$(date "+%j")

userInitial=$(printf "%.1s" "${LOGNAME}")

if [ $# -gt 1 ] ; then
    echo "usage: ${progname} [run_counter_filename]" 1>&2
    echo '    where run_counter_filename defaults to ${HOME}/.orc-test-run-counter' 1>&2
    exit 64 # usage
fi

filename=${1:-${HOME}/.orc-test-run-counter}

# Only symlink() is specified as atomic on both POSIX and NFSv3.
# Note that shell noclobber and mkdir are NOT necessarily atomic.
while ! ln -s "${filename}" "${filename}.lock" 2>/dev/null; do
    sleep 1
done

trap "mv \"${filename}.lock\" \"${filename}.deleteme\" && rm -f \"${filename}.deleteme\"" 0

if [ -e "${filename}" ]; then
    read runDate runNum <"${filename}"
    if [ "X${runDate}" = "X${today}" ]; then
        runNum=$((runNum + 1))
    else
        runNum=1
    fi
else
    runNum=1
fi

paddedRunNum=$(printf "%03d" "${runNum}")

echo ${today} ${runNum} >"${filename}"

mv "${filename}.lock" "${filename}.deleteme" && rm -f "${filename}.deleteme"

trap - 0

#echo "${yearLetter}${dayOfYear}${userInitial}${paddedRunNum}"
echo "${today}-${userInitial}${paddedRunNum}"
