include "date.inc"

val short = DateTimeFormat.forStyle("SS")
LocalDateTime(2008, 9, 17) >d>
short.print(d)