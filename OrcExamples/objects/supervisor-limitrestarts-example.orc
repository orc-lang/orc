{- supervisor-example.orc -- Supervisor example
 -
 - Created by amp on Mar 24, 2015 8:23:14 PM
 -}

include "debug.inc"
include "supervisor.inc"

def procName(name) = name
--def procName(name) = "Proc"

class Proc extends Supervisable {
  val name :: String

  val working = URandom() <: 0.1
  val running = Ref(true)

  site monitorUsefulness() = {| repeat({ Rwait(100) >> running? }) >false> true |} 
  def shutdown() = running := false
  
  val _ = repeat({ Rwait(100) >> Ift(running?) })
}

class Monitor extends Supervisable {
  val name :: String
  val p :: Manager

  site monitorUsefulness() = {| repeat({ Rwait(10) >> p().working }) >false> true |} 
  def shutdown() = signal
  val _ = Block()
}

class Group extends StaticSupervisor {
  val killTime = 2000
  val managers = [servers]
  val servers = Manager({ 
    new (StaticSupervisor with OneForOneSupervisor with LimitStarts) {
      val killTime = 1000
      val startsLimit = 5
      val timePeriod = 10000
      val managers = [monitor, server]
	  val server = Manager({ new Proc { val name = "Server" } })
	  val monitor = Manager({ new Monitor { val name = "Mon" # val p = server } })
    }
  })
}

{|
val s = new Group with AllForOneSupervisor

Rwait(5000) >> (
val servers = s.servers 
Println((servers().server() = (Rwait(500) >> servers().server()), servers().monitor() = (Rwait(500) >> servers().monitor()))) 
) >> 
Rwait(300) 
|} >> stop
--| Rwait(10000) >> DumpState()

{-
OUTPUT:
(true, true)
-}
