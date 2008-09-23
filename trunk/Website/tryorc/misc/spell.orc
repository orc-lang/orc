include "net.inc"
include "forms.inc"

val YahooSpell = YahooSpellFactory("orc/orchard/yahoo.properties")

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
