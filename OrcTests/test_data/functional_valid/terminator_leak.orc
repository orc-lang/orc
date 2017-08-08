-- This will test creates an expression which halts without publishing inside a terminator. 
-- The optimizer will remove this for statically known cases, so we need to trick it.

val N = 16

val chan = Channel()

def haltSilently() = chan.getD()

upto(2 ** N) >> {| haltSilently() |} ; "Done!"

{-
This test cannot be automatically checked (it would require introspecting into the Terminator 
tree of the PorcE interpreter).

To run this test:
* Edit PorcE/src/orc/run/porce/runtime/Terminator.scala by uncommenting the block marked with 
  "This commented code should be enabled to run terminator_leak.orc"
* Run this program with the modified runtime.
* The test succeeds if you get "Done!" without receiving any warning level log messages saying
  "You may be leaking Terminatables".
-}  
