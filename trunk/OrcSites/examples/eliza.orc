include "ui.inc"
include "fun.inc"

def ElizaPrompt(init) =
    val eliza = Eliza()
    def loop(message) =
        eliza(Prompt(message)) >response>
        loop(response)
    loop(init)

ElizaPrompt("How do you do.  Please tell me your problem.")
