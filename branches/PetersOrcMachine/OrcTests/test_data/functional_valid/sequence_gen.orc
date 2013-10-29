def class gen_seq(init :: Integer) =
  val n = Ref[Integer](init)
  val s = Semaphore(1)
  def next() = s.acquire() >> n? >x>
    (n := n?+1 >> stop; s.release()) >> x
  signal

val sequence_generator = gen_seq(0)

sequence_generator.next() | sequence_generator.next()
{-
OUTPUT:PERMUTABLE:
0
1
-}
