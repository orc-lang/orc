

class def AllValues(f) :: AllValues {
  val chan = Channel()
  val buffer = Ref([])
  val _ = f() >x> chan.put(x)
  def read() = chan.
}

val o = new { 
  val x = AllValues({ 1 | 2 | Rwait(1000) >> 3 })
}

o.x.read()