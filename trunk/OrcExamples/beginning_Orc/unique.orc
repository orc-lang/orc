{--
Write a function which, given a channel, publishes
every value received over the channel, omitting
repeated values (so each value published is unique).
Hint: use a Set to record the values seen in the
channel.
--}
import class Set = "scala.collection.mutable.HashSet"

def unique[X](Channel[X]) :: X
def unique(c) =
  val seen = Set[X]()
  def loop() :: X
  def loop() =
    val x = c.get()
    if seen.contains(x) then 
      loop()
    else 
      x | seen.add(x) >> loop()
      
  loop()

val c = Channel[Number]()
  unique(c)
| (upto(50) >n> c.put(n % 10) >> stop; c.close()) >> stop

{-
OUTPUT:PERMUTABLE:
0
1
2
3
4
5
6
7
8
9
-}