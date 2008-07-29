include "fun.inc"

-- Weizenbaum's Regerian therapist "ELIZA":
-- Prompt the user, feed their response to
-- Eliza, and repeat with her response.
def ElizaPrompt(init) =
    val eliza = Eliza()
    def loop(message) =
        eliza(Prompt(message)) >response>
        loop(response)
    loop(init)

ElizaPrompt("How do you do.  Please tell me your problem.")
