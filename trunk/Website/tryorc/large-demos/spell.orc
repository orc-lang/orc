{-
Spell-check the first 20 words of a text file
which you will be prompted to upload. We'll try
both Google and Yahoo spell-checkers.
-}
include "net.inc"
include "forms.inc"

val YahooSpell = YahooSpellFactory("orc/orchard/orchard.properties")

def spellCheck([], _) = stop
def spellCheck(word:words, i) =
    GoogleSpellUnofficial(word) >(_:_) as suggs>
      ("G", i, word, suggs)
  | YahooSpell(word) >(_:_) as suggs>
      ("Y", i, word, suggs)
  | spellCheck(words, i+1)  

WebPrompt("File Upload", [
  FormInstructions("instructions",
    "Choose a text file to upload; I will spell-check the first 20 words."),
  UploadField("file", "Text File"),
  Button("upload", "Upload") ]) >data>
data.get("file").getString() >text>
spellCheck(take(20, text.split("\\s+")), 1)
