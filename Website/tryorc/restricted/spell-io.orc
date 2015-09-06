{-
Download Lewis Carroll's "Alice Through the Looking Glass"
from Project Gutenberg http://www.gutenberg.org,
unzips it, finds the "JABBERWOCKY" poem, and sends
the first few lines to Google to spell check.
Prints out a list of corrections, with the number
of the word, the misspelled word, and a list of
suggested spellings.
-}
include "net.inc"

import class InputStreamReader = "java.io.InputStreamReader"
import class BufferedReader = "java.io.BufferedReader"
import class ZipInputStream = "java.util.zip.ZipInputStream"
import class URL = "java.net.URL"

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
  GoogleSpellUnofficial(word) >(_:_) as suggs>
  (i, word, suggs)
  | spellCheck(words, i+1)  
  
val url = "http://orc.csres.utexas.edu/test-data/Gutenberg_Through-the-Looking-Glass.zip"
    -- Orc project's cached copy; Gutenberg doesn't like robots.
    -- Original URL: http://www.gutenberg.org/files/12/12.zip

BufferedReader(InputStreamReader(unzip(openURL(url)))) >reader>
skipto(reader, "JABBERWOCKY") >>
reader.readLine() >>
map(lambda (_) = reader.readLine(), range(1, 5)) >lines>
unlines(lines).trim().split("\\s+") >words>
spellCheck(arrayToList(words), 1)