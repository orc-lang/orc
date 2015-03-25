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

class def Group() :: Group extends SupervisableGroup {
  val elementIDs = ["Server", "DB"]
  val server = Ref()
  val db = Ref()
  val proxy = Ref()
  def setElement("Server", s) = server := s
  def setElement("DB", s) = db := s
  def setElement("Proxy", s) = proxy := s
  def builder(id) = ignore({ Proc(id) })
}

{|
val s = AllForOneSupervisor(Group(), 1000)
Println("==== Test All for One") >>
Rwait(1000) >> Println("Stopping one Proc") >> s.group.db?().justStop() >> 
Rwait(2000) >> s.shutdown() >> Println("Shutdown done") 
|} >>
{|
val s = OneForOneSupervisor(Group(), 1000)
Println("==== Test One for One") >>
Rwait(1000) >> Println("Stopping one Proc") >> s.group.db?().justStop() >> 
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
