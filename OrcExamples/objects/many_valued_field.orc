

class AllValues {
  val f

  val lock = Semaphore(1)
  val buffer = Ref([])
  val _ = f() >x> withLock(lock, { buffer := x : buffer? })
  def read() = each(buffer?)
}
def AllValues(f_) = new AllValues { val f = f_ }

val o = new { 
  val x = AllValues({ 1 | 2 | Rwait(1000) >> 3 })
}

o.x.read()
