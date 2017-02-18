{- webtext.inc -- MapReduce readers for text on the web.
 -
 - All I/O code based on spell-web.orc
 -}

include "net.inc"

import class InputStreamReader = "java.io.InputStreamReader"
import class BufferedReader = "java.io.BufferedReader"
import class ZipInputStream = "java.util.zip.ZipInputStream"
import class URL = "java.net.URL"

def stopAfterNPublications[T](f :: lambda() :: T, n :: Integer) =
  -- TODO: This should be an unordered channel.
  val chan = Channel()
  val counter = Counter(n) 
  {| 
    (f() >v> counter.dec() >> chan.put(v) >> stop ; signal) |
    counter.onZero()
  |} >> chan.closeD() >> stop |
  repeat(chan.get)

def stopOnSentinal[T](f :: lambda() :: T, sentinal :: T) =
  -- TODO: This should be an unordered channel.
  val chan = Channel()
  {| 
    f() >v> (
      if v = sentinal then
        signal
      else
        chan.put(v) >> stop
    )
  |} >> chan.closeD() >> stop |
  repeat(chan.get)

class WebUtilities {
  def openURL(url) =
    URL(url) >url>
    url.openConnection().getInputStream()
  
  def unzipStream(stream) =
    ZipInputStream(stream) >zip>
    zip.getNextEntry() >>
    zip
  
  def skipto(reader, phrase) =
    val line = reader.readLine()
    if line = null then ""
    else if line.contains(phrase) then line
    else skipto(reader, phrase)
}

class WebZippedLineReader extends WebUtilities {
  val url :: String
  
  def read() = 
    val reader = BufferedReader(InputStreamReader(unzipStream(openURL(url))))
    repeat({ reader.readLine() })
}

class WebLineReader extends WebUtilities {
  val url :: String
  
  def read() = 
    val reader = BufferedReader(InputStreamReader(openURL(url)))
    stopOnSentinal({ repeat({ reader.readLine() }) }, null)
}

class LimitedReader {
  val itemLimit :: Integer
  def read() = 
    stopAfterNPublications({ super.read() }, itemLimit)
}
