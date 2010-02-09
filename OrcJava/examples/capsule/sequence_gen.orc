def capsule gen_seq(init) =
  val n = Ref(init)
  val s = Semaphore(1)
  def next() = s.acquire() >> n? >x> 
    (n := n?+1 >> stop; s.release()) >> x
  signal
  
val sequence_generator = gen_seq(0)

sequence_generator.next() | sequence_generator.next() 
