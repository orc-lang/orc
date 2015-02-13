{- debug.orc -- Orc program debug
 -
 - $Id$
 -
 - Created by amp on Feb 11, 2015 8:49:22 PM
 -}

import site DumpState = "orc.lib.util.DumpState"

def dumpStatePeriodically(interval :: Integer) = repeat({Rwait(interval) >> DumpState()}) >> stop 