{- webregistration.orc -- Orc program for "Execution Migration" experiments in "Cohesive Programming for Distributed Systems" paper
 -
 - Created by jthywiss on Sep 4, 2014 1:18:20 PM
 -}

{- Run with --noprelude.  We're keeping environments minimal until we 
 - implement pruning. -}

include "prelude/core.inc"
import site Prompt = "orc.lib.util.Prompt"
import type List = "orc.lib.builtin.structured.ListType"

import class ConcurrentSkipListSet = "java.util.concurrent.ConcurrentSkipListSet"
val userDatabase = ConcurrentSkipListSet()
val bannedDatabase = ConcurrentSkipListSet()

def initDatabases() = 
  -- Copied from Orc standard prelude
  def each[A](List[A]) :: A
  def each([]) = stop
  def each(h:t) = h | each(t)
  def for(Integer, Integer) :: Integer
  def for(low, high) =
    if low >= high then stop
    else ( low | for(low+1, high) )
  def upto(Integer) :: Integer
  def upto(high) = for(0, high)

  userDatabase.clear() >> bannedDatabase.clear() >> ((upto(10000) >n> userDatabase.add("user."+n+"@example.com") >> stop) | (upto(10000) >n> bannedDatabase.add("banned.user."+n+"@example.com") >> stop)) ; signal #

( {- Init user and banned address databases -} 
  userDatabase.clear()  >>
  bannedDatabase.clear()  >>
  initDatabases()
) >>
  
{- Simulate Prompt response: -} Prompt("Ready?") >> "bugs.bunny@example.com" >username>
(
  --val username = Prompt("E-mail address:")
  if (username.matches("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$") && ~bannedDatabase.contains(username))
  then
    userDatabase.add(username) >> "Added "+username
  else
    Error("\"" + username + "\" is not a valid e-mail address, or has been banned.")
)
