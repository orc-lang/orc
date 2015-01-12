class gen_seq {
  val n = Ref[Integer](0)
  val s = Semaphore(1)
  def next() = this.s.acquire() >> this.n? >x>
    (this.n := this.n?+1 >> stop; this.s.release()) >> x
}

val sequence_generator = new gen_seq

sequence_generator.next() | sequence_generator.next()
{-
OUTPUT:PERMUTABLE:
0
1
-}
