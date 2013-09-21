{-
Spell-check the first 20 words of a text file
which you will be prompted to upload.
-}
include "net.inc"
include "forms.inc"

val BingSpell = BingSpellFactoryPropertyFile("orc/orchard/orchard.properties")

def spellCheck([], _) = stop
def spellCheck(word:words, i) =
  BingSpell(word) >(_:_) as suggs> (i, word, suggs)
  | spellCheck(words, i+1)

WebPrompt("File Upload", [
  FormInstructions("instructions",
    "Choose a text file to upload; I will spell-check the first 20 words."),
  UploadField("file", "Text File"),
  Button("upload", "Upload") ]) >data>
data.get("file").getString() >text>
arrayToList(text.split("\\s+")) >words>
spellCheck(take(20, words), 1)
