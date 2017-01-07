{- supervisor-example.orc -- Supervisor example
 -
 - Created by amp on Mar 24, 2015 8:23:14 PM
 -}

include "debug.inc"
include "supervisor.inc"

def procName(name) = name
--def procName(name) = "Proc"

class def ProcFastFail(name :: String) :: ProcFastFail extends Supervisable {
  site monitorUsefulness() = Rwait(10) >> true
  def shutdown() = signal
}

class Group extends StaticSupervisor with SupervisorBase {
  val killTime = 1000
  val managers = [server]
  val server = Manager({ ProcFastFail("Server") })
  def shutdown() = Println("Shutting down group") >> super.shutdown()
}

{|
val s = new Group with OneForOneSupervisor
s >> Rwait(1000) >> (s.server() >> true | Rwait(1000) >> false)
|}

{-
OUTPUT:
true
-}
