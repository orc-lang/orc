include "date.inc"

def clock() =   
  val start = DateTime().getMillis()
  lambda () = DateTime().getMillis() - start

clock() >c> Rwait(1000) >> c()