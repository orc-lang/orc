-- If the answer is not received within 5
-- seconds, use the default answer "timed out"
"Answer: " + answer

  <answer<

        Prompt("What is your favorite food?")
      | Rtimer(5000) >> "timed out"

-- Source:
-- Computation Orchestration: A Basis for Wide-Area Computing (DOI)
-- Jayadev Misra and William R. Cook
-- Journal of Software and Systems Modeling, May 2006.
