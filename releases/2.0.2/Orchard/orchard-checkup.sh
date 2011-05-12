#!/bin/bash

# This program runs a simple test of an Orchard server to
# make sure it is running and compiling programs correctly.
#
# Place it somewhere like /usr/local/bin
# Then add the following to /etc/cron.d/orchard:
# 0,15,30,45 * * * * root /usr/local/bin/orchard-checkup

URL="http://orc.csres.utexas.edu/orchard/json/executor"
EMAIL="oncall@orc.csres.utexas.edu"
LOCK="/var/run/orchard-checkup.lock"

# report an error and halt
error() {
	if ! [ -f $LOCK ]; then
		touch $LOCK
		echo "$1" | mail -s "orc.csres.utexas.edu problem" "$EMAIL"
	fi
	exit 1
}

# post a request
post() {
        curl -s -d "$1" "$URL"
}

# post a request, compare against a response glob,
# and report an error if it fails
check() {
        [[ "$1" == $2 ]] || error "$3: $1"
}

JOBQ="$(post "{compileAndSubmit:{program:'1'}}")"
check "$JOBQ" "\"*-*\"" "Could not create job"

JOB="$(eval echo $JOBQ)"
check "$(post "{startJob:{job:'$JOB'}}")" "null" "Could not start job"
check "$(post "{jobEvents:{job:'$JOB'}}")" "*value\":{\"@xsi.type\":\"xs:integer\",\"$\":\"1\"}*" "Unexpected events"
check "$(post "{finishJob:{job:'$JOB'}}")" "null" "Could not halt job"
