include "net.inc"
include "forms.inc"

def spellCheck(word:words, i) =
  def process([]) =
    spellCheck(words, i+1)
  def process(suggestions) =
    (i, word, suggestions) | process([])
  GoogleSpellUnofficial(word) >words>
  process(words)

WebPrompt("File Upload", [
  FormInstructions("instructions",
    "Choose a text file to upload; I will spell-check the first 20 words."),
  UploadField("file", "Text File"),
  Button("upload", "Upload") ]) >data>
data.get("file").getString() >text>
spellCheck(take(20, text.split("\\s+")), 1)
