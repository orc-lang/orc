{- date.orc -- Orc program that formats a date-time
 -}

include "date.inc"

val short = DateTimeFormat.forStyle("SS")
LocalDateTime(2008, 9, 17, 12, 34) >d>
short.print(d)
