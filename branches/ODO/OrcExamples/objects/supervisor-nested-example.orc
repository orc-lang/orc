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
  
  def justStop() = (running := false, Println("Stopping " + procName(name))) >> signal
  
  val _ = Println("Starting " + procName(name)) >> repeat({ Rwait(100) >> Ift(running?) })
}

class def Group() :: Group extends SupervisableGroup {
  val managers = [servers, db]
  val servers = Manager({ 
    OneForOneSupervisor({ new SupervisableGroup {
      def managerBuilder(i) = Manager({ Proc("server " + i) })
      val managers = map(managerBuilder, [1, 2])
      val [server1, server2] = managers
    } }, 1000)
  })
  val db = Manager({ Proc("DB") })
}

{|
val s = AllForOneSupervisor(Group, 2000)

Rwait(2000) >> Println("= Stopping DB") >> s.group.db().justStop() >>
Rwait(5000) >> Println("= Stopping one server") >> s.group >g> g.servers() >servers> servers.group >g> g.server1() >s1> s1.justStop() >> 
Rwait(2000) >> Println("= Shutting down") >> s.shutdown() >> Println("= Shutdown done") 
|} >> stop
--| Rwait(10000) >> DumpState()

{-
OUTPUT:
Starting Proc
Starting Proc
Starting Proc
= Stopping DB
Stopping Proc
Shutting down Proc
Shutting down Proc
Starting Proc
Starting Proc
Starting Proc
= Stopping one server
Stopping Proc
Starting Proc
= Shutting down
Shutting down Proc
Shutting down Proc
Shutting down Proc
= Shutdown done
-}
