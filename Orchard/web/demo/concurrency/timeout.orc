-- If the answer is not received within 5
-- seconds, use the default answer "timed out"
"Answer: " + answer

  <answer<

        Prompt("What is your favorite food?")
	    | Rtimer(5000) >> "timed out"
