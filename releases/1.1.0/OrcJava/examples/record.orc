{--
Factory method; returns a new counter object initially at value 0. Calling inc
increments the value, dec decrements it, and read returns it.
--}
def Counter() =
  -- value of the counter
  val n = Ref[Integer](0)
  -- lock used to serialize access to the counter
  val lock = Semaphore(1)

  -- counter methods
  def inc() = n := n? + 1
  def dec() = n := n? - 1
  def read() = n?

  -- Return a record of synchronized methods
  -- Note: This style of record construction is inherently not type safe.
  Record(
    "inc", synchronized(lock, inc),
    "dec", synchronized(lock, dec),
    "read", synchronized(lock, read) )

-- Example of using the counter
val c = Counter()
c.inc() >> signals(10) >> (c.inc(), c.dec()) >> stop
; c.read()

{-
OUTPUT:
1
-}
