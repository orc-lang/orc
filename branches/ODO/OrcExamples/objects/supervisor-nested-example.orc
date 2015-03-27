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

class Group extends StaticSupervisor {
  val killTime = 2000
  val managers = [servers, db]
  val servers = Manager({ 
    new (StaticSupervisor with OneForOneSupervisor) {
      val killTime = 1000
      def managerBuilder(i) = Manager({ Proc("server " + i) })
      val managers = map(managerBuilder, [1, 2])
      val [server1, server2] = managers
    }
  })
  val db = Manager({ Proc("DB") })
}

{|
val s = new Group with AllForOneSupervisor

Rwait(2000) >> Println("= Stopping DB") >> s.db().justStop() >>
Rwait(5000) >> Println("= Stopping one server") >> s.servers().server1().justStop() >> 
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
