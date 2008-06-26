-- Rtimer together with asymmetric composition imposes a timeout.

cat("Answer: ", answer)
	<answer< David("What is your favorite food?")
	         | Rtimer(5000) >> "timed out"
