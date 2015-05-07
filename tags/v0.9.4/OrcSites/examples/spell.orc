include "net.inc"
include "ui.inc"

def spellCheck(word:words, i) =
  GoogleSpellUnofficial(word) >(_:_) as suggs>
    (i, word, suggs)
  | spellCheck(words, i+1)  

Prompt("Enter one or more words:") >text>
text.split("\\s+") >words>
spellCheck(words, 1)
; "DONE"