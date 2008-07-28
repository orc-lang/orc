-- If a "high priority" response is
-- received within 8 seconds, use its
-- answer, otherwise use whichever response
-- is received first.
z <z<

    ( high

      <high<   Prompt("High priority response: ")
             | Rtimer(8000) >> low

      <low< Prompt("Low priority response: ") )
