

class def AllValues(f) :: AllValues {
  val lock = Semaphore(1)
  val buffer = Ref([])
  val _ = f() >x> withLock(lock, { buffer := x : buffer? })
  def read() = each(buffer?)
}

val o = new { 
  val x = AllValues({ 1 | 2 | Rwait(1000) >> 3 })
}

o.x.read()