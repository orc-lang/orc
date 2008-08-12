def Countdown(i) = if(i > 0) >> 
				   ( i 
				   | Rtimer(500) >> Countdown(i-1) 
				   )

Countdown(10)