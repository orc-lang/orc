{-
   Make a request to the Fourmilab HotBits service 
   to produce a sequence of 4 random bytes in hex.
   
   See http://www.fourmilab.ch/hotbits/ for more info.
-}

{- Returns a string of n random hexadecimal bytes -}
def randombytes(n) =
  val location = "https://www.fourmilab.ch/cgi-bin/Hotbits"
  val query = {. nbytes = n, fmt = "xml" .} 
  val response = HTTP(location, query).get()
  val xml("hotbits", xml("random-data", data)) = ReadXML(response) 
  data.replaceAll("\\s+"," ")

randombytes(4)