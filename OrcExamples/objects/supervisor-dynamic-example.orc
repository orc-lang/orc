{- supervisor-example.orc -- Supervisor example
 -
 - $Id$
 -
 - Created by amp on Mar 24, 2015 8:23:14 PM
 -}

include "debug.inc"
include "supervisor.inc"

--def procName(name) = name
def procName(name) = "Proc"

class def Proc(name :: String) :: Proc extends Supervisable {
  val running = Ref(true)

  site monitorUsefulness() = {| repeat({ Rwait(100) >> running? }) >false> true |} 
  def shutdown() = (running := false, Println("Shutting down " + procName(name))) >> signal
  
  def justStop() = running := false
  
  val _ = Println("Starting " + procName(name)) >> repeat({ Rwait(10) >> Ift(running?) })
}

class Group extends Supervisor {
  val killTime = 1000
  def addServer(i) = add(Manager({ Proc("Server " + i) }))
}

{|
val s = new Group with AllForOneSupervisor
Println("==== Test All for One") >> 
map(s.addServer, [1, 2, 3]) >[s1, s2, s3]>
Rwait(500) >> Println("Stopping one Proc") >> s1().justStop() >>
Rwait(500) >> Println("= Starting more") >> map(s.addServer, [4, 5]) >[s4, s5]>
Rwait(500) >> Println("Stopping one Proc") >> s3().justStop() >> 
Rwait(500) >> Println("= Removing") >> map(s.remove, [s1, s5]) >>
Rwait(500) >> Println("Stopping one Proc") >> s4().justStop() >> 
Rwait(500) >> s.shutdown() >> Println("Shutdown done") >> (
Iff(s1.isRunning()) >> Println("s1 was stopped even though it was removed") |
Iff(s5.isRunning()) >> Println("s5 was stopped even though it was removed")
; signal)
|} >>
{|
val s = new Group with OneForOneSupervisor
Println("==== Test One for One") >> 
map(s.addServer, [1, 2, 3]) >[s1, s2, s3]>
Rwait(500) >> Println("Stopping one Proc") >> s1().justStop() >>
Rwait(500) >> Println("= Starting more") >>
map(s.addServer, [4, 5]) >[s4, s5]>
Rwait(500) >> Println("Stopping two Proc") >> (s3().justStop(), s4().justStop()) >> 
Rwait(500) >> s.shutdown() >> Println("Shutdown done") 
|} >> stop

{-
OUTPUT:
==== Test All for One
Starting Proc
Starting Proc
Starting Proc
Stopping one Proc
Shutting down Proc
Shutting down Proc
Starting Proc
Starting Proc
Starting Proc
= Starting more
Starting Proc
Starting Proc
Stopping one Proc
Shutting down Proc
Shutting down Proc
Shutting down Proc
Shutting down Proc
Starting Proc
Starting Proc
Starting Proc
Starting Proc
Starting Proc
= Removing
Stopping one Proc
Shutting down Proc
Shutting down Proc
Starting Proc
Starting Proc
Starting Proc
Shutting down Proc
Shutting down Proc
Shutting down Proc
Shutdown done
==== Test One for One
Starting Proc
Starting Proc
Starting Proc
Stopping one Proc
Starting Proc
= Starting more
Starting Proc
Starting Proc
Stopping two Proc
Starting Proc
Starting Proc
Shutting down Proc
Shutting down Proc
Shutting down Proc
Shutting down Proc
Shutting down Proc
Shutdown done
-}
