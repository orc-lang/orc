{--
Write a function which, given a channel, publishes
every value received over the channel, omitting
repeated values (so each value published is unique).
Hint: use a Set to record the values seen in the
channel.
--}

class Object = java.lang.Object

def unique[X](Buffer[X]) :: X
def unique(c) =
  val seen = Set[X]()
  def loop() :: X
  def loop() =
    c.get() >x>
    {- 
      Orc Set proxy is not really generic;
      the contains method takes an Object argument.
      So we can't check the call seen.contains
      without the bound X <: Object, but the
      typechecker does not currently implement
      bounded polymorphism, so we tell it not
      to check the method call.
    -}
    if (seen.contains(x) :!: Boolean)  
    then loop()
    else x | seen.add(x) >> loop()
  loop()

val c = Buffer[Number]()
  unique(c)
| (upto(50) >n> c.put(n % 10) >> stop; c.close()) >> stop


{-
OUTPUT:
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