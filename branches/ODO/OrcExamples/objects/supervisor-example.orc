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

  def shutdown() = (running := false, Println("Shutting down " + procName(name))) >> signal
  
  def justStop() = running := false
  
  val _ = Println("Starting " + procName(name)) >> repeat({ Rwait(100) >> Ift(running?) })
}

class Group extends StaticSupervisor {
  val killTime = 1000
  val managers = [server, db]
  val server = Manager({ Proc("Server") })
  val db = Manager({ Proc("DB") })
}

{|
val s = new Group with AllForOneSupervisor
Println("==== Test All for One") >>
Rwait(1000) >> Println("Stopping one Proc") >> s.db().justStop() >> 
Rwait(2000) >> s.shutdown() >> Println("Shutdown done") 
|} >>
{|
val s = new Group with OneForOneSupervisor
Println("==== Test One for One") >>
Rwait(1000) >> Println("Stopping one Proc") >> s.db().justStop() >> 
Rwait(2000) >> s.shutdown() >> Println("Shutdown done") 
|} >> stop

{-
OUTPUT:
==== Test All for One
Starting Proc
Starting Proc
Stopping one Proc
Shutting down Proc
Starting Proc
Starting Proc
Shutting down Proc
Shutting down Proc
Shutdown done
==== Test One for One
Starting Proc
Starting Proc
Stopping one Proc
Starting Proc
Shutting down Proc
Shutting down Proc
Shutdown done
-}
