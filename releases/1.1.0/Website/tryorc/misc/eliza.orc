include "fun.inc"

{-
Weizenbaum's Rogerian therapist "ELIZA",
based on an implementation by Charles Hayden
found at http://chayden.net/eliza/Eliza.html
-}

-- Prompt the user, feed their response to
-- Eliza, and repeat with her response.
def ElizaPrompt(init) =
    val eliza = Eliza()
    def loop(message) =
        eliza(Prompt(message)) >response>
        loop(response)
    loop(init)

ElizaPrompt("How do you do.  Please tell me your problem.")
