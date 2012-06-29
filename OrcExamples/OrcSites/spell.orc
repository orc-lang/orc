{- spell.orc -- Orc program that spell checks words using Google's spell service
 -}

include "net.inc"
include "ui.inc"

def spellCheck([], i) = stop
def spellCheck(word:words, i) =
    GoogleSpellUnofficial(word) >(_:_) as suggs>
      ("G", i, word, suggs)
  --| YahooSpell(word) >(_:_) as suggs>
  --    ("Y", i, word, suggs)
  | spellCheck(words, i+1)  

Prompt("Enter one or more words:") >text>
text.split("\\s+") >words>
spellCheck(words, 1)
; "DONE"
