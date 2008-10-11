{--
site cat(Top, ...) :: String
Concatenate one or more values as strings.
For Java objects, this will call <code>toString()</code> to convert
the object to a String.
--}
site cat = orc.lib.str.Cat

{--
site print(Top, ...) :: Signal
Print one or more values as strings, concatenated,
to standard output.
For Java objects, this will call <code>toString()</code> to convert
the object to a String.
--}
site print = orc.lib.str.Print

{--
site println(Top, ...) :: Signal
Print one or more values as strings, concatenated,
to standard output, with each value followed by a newline.
For Java objects, this will call <code>toString()</code> to convert
the object to a String.
--}
site println = orc.lib.str.Println

{--
site parseInt(String) :: BigInteger
Parse a string as a BigInteger.
--}
site parseInt = orc.lib.str.AtoI

{--
site parseBool(String) :: Boolean
Parse a string as a Boolean (true/false).
--}
site parseBool = orc.lib.str.AtoB

{--
def lines(String) :: [String]
Split a string into lines, which are substrings
terminated by an endline or the end of the string.
DOS, Mac, and Unix endline conventions are all accepted.
Endline characters are not included in the result.
--}
def lines(text) =
  val out = text.split("\n|\r\n|\r")
  if out.get(out.length()-1) = "" then
    out.split(0, out.length()-1)
  else out
  
{--
def unlines([String]) :: String
Append a linefeed, "\n", to each string in the sequence
and concatenate the results.
--}
def unlines(line:lines) = cat(line, "\n", unlines(lines))
def unlines([]) = ""
  
{--
def words(String) :: [String]
Split a string into words, which are sequences of non-whitespace characters separated by whitespace.
--}
def words(text) = text.trim().split("\\s+")

{--
def unwords([String]) :: String
Concatenate a sequence of strings with a single space between
each string.
--}
def unwords([]) = ""
def unwords([word]) = word
def unwords(word:words) = cat(word, " ", unwords(words))
