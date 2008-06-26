-- Rtimer together with asymmetric composition imposes a timeout.

cat("Got answer ", answer)
	<answer< David("What is your favorite food?")
	         | Rtimer(2000) >> "Timed out"