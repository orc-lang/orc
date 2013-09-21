{-
Download Geoffrey Chaucer's "The Canterbury Tales"
from Project Gutenberg (cached at the Orc project)
unzips it, finds the first poem ("Whan that Aprille..."),
and send the first verse to Bing to spell check.
Prints out a list of corrections, with the number
of the word, the misspelled word, and a list of
suggested spellings.
-}
include "net.inc"

import class InputStreamReader = "java.io.InputStreamReader"
import class BufferedReader = "java.io.BufferedReader"
import class ZipInputStream = "java.util.zip.ZipInputStream"
import class URL = "java.net.URL"

val BingSpell = BingSpellFactoryPropertyFile("orc/orchard/orchard.properties")

def openURL(url) =
  URL(url) >url>
  url.openConnection().getInputStream()

def unzip(stream) =
  ZipInputStream(stream) >zip>
  zip.getNextEntry() >>
  zip

def skipto(reader, phrase) =
  val line = reader.readLine()
  if line = null then ""
  else if line.contains(phrase) then line
  else skipto(reader, phrase)

def spellCheck([], _) = stop
def spellCheck(word:words, i) =
  BingSpell(word) >(_:_) as suggs>
    (i, word, suggs)
  | spellCheck(words, i+1)

val url = "https://orc.csres.utexas.edu/test-data/Gutenberg_Canterbury-Tales.zip"
    -- Orc project's cached copy; Gutenberg doesn't like robots.
    -- Original URL: http://www.gutenberg.org/files/22120/22120-8.zip

BufferedReader(InputStreamReader(unzip(openURL(url)))) >reader>
skipto(reader, "HERE BIGINNETH THE BOOK OF THE TALES OF CAUNTERBURY.") >>
reader.readLine() >>
map(lambda (_) = reader.readLine(), range(1, 19)) >lines>
unlines(lines).trim().split("\\s+") >words>
spellCheck(arrayToList(words), 1)
