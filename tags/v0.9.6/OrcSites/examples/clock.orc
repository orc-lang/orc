include "date.inc"

def clock() =   
  val start = DateTime().getMillis()
  lambda () = DateTime().getMillis() - start

clock() >c> Rtimer(1000) >> c()