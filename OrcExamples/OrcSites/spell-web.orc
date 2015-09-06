{- spell-web.orc -- Orc program that fetches the Jabberwocky poem, then spell checks it using Google's spell service
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
  reader.readLine() >line>
  (
    if line = null then ""
    else if line.contains(phrase) then line
    else skipto(reader, phrase)
  )

def spellCheck([], i) = stop
def spellCheck(word:words, i) =
  GoogleSpellUnofficial(word) >(_:_) as suggs>
    (i, word, suggs)
  | spellCheck(words, i+1)

val url = "http://www.gutenberg.org/files/12/12.zip"

BufferedReader(InputStreamReader(unzip(openURL(url)))) >reader>
skipto(reader, "JABBERWOCKY") >>
reader.readLine() >>
map(lambda (_) = reader.readLine(), range(1, 5)) >lines>
unlines(lines).split("\\s+") >words>
spellCheck(words, 1)
