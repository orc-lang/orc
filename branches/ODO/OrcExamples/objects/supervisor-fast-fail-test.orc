{- supervisor-example.orc -- Supervisor example
 -
 - $Id$
 -
 - Created by amp on Mar 24, 2015 8:23:14 PM
 -}

include "debug.inc"
include "supervisor.inc"

def procName(name) = name
--def procName(name) = "Proc"

class def ProcFastFail(name :: String) :: ProcFastFail extends Supervisable {
  def shutdown() = signal
}

class Group extends StaticSupervisor {
  val killTime = 1000
  val managers = [server]
  val server = Manager({ ProcFastFail("Server") })
}

{|
val s = new Group with OneForOneSupervisor
Rwait(1000) >> (s.server() >> true | Rwait(1000) >> false)
|}

{-
OUTPUT:
true
-}