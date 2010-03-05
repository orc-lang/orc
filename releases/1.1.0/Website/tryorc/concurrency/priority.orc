-- If a "high priority" response is
-- received within 8 seconds, use its
-- answer, otherwise use whichever response
-- is received first.

response <response< (

    Prompt("High priority response: ")
  | Rtimer(8000) >> low

    <low< Prompt("Low priority response: ")
)

-- Source:
-- Computation Orchestration: A Basis for Wide-Area Computing (DOI)
-- Jayadev Misra and William R. Cook
-- Journal of Software and Systems Modeling, May 2006.
