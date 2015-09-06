{- eliza.orc -- Orc program that invokes the ELIZA site
 -
 - Joseph Weizenbaum. 1966. ELIZA--a computer program for the study of natural
 - language communication between man and machine. Commun. ACM 9, 1, 36-45. 
 -}

include "ui.inc"
include "fun.inc"

def ElizaPrompt(init) =
    val eliza = Eliza()
    def loop(message) =
        eliza(Prompt(message)) >response>
        loop(response)
    loop(init)

ElizaPrompt("How do you do.  Please tell me your problem.")
