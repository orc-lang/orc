def timeIt[A](f :: lambda() :: A) =
  val c = Channel[A]()
  repeat(c.get) |
  (
    Rtime() >s>
    (f() >x> c.put(x) >> stop ; Rtime()) >e> 
    c.close() >>
    Println("Time used: " + (e-s) + "ms") >> stop
  )
