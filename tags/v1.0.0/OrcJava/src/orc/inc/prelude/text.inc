{--
Operations on strings.
--}

{--
@site String

Strings themselves have a set of methods associated with them. These methods can
be invoked on any string literal or any variable bound to a string.

The methods documented here are only a subset of those available in the Java
implementation. In practice, strings in the Java implementation support all 
methods provided by Java's <code>String</code> class.

@method String.length() :: Integer
Return the length of the string.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 4
"four".length()]]></programlisting>

@method String.substring(Integer, Integer) :: String
Return the substring of this string covered by the given half-open range.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: "orc"
val s = "apple orchard"
s.substring(6,9)]]></programlisting>

@method String.indexOf(String) :: Integer
Return the starting index of the first occurrence of the given string.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 6
"apple orchard".indexOf("orc")]]></programlisting>
--}

{--
@site cat(Top, ...) :: String
Return the string representation of one or more values, concatenated.
For Java objects, this will call <code>toString()</code> to convert
the object to a String.
--}
site cat = orc.lib.str.Cat

{--
@site print(Top, ...) :: Top
Print one or more values as strings, concatenated, to standard output.
For Java objects, this will call <code>toString()</code> to convert
the object to a String.
--}
site print = orc.lib.str.Print

{--
@site println(Top, ...) :: Top
Print one or more values as strings, concatenated,
to standard output, with each value followed by a newline.
For Java objects, this will call <code>toString()</code> to convert
the object to a String.
--}
site println = orc.lib.str.Println

{--
@site read[A](String) :: A
Given a string representing an Orc value (using standard
Orc literal syntax), return the corresponding value. If
the argument does not conform to Orc literal syntax,
halt with an error.

Example:
<programlisting language="orc-demo"><![CDATA[
  read("true") -- publishes the boolean true
| read("1") -- publishes the integer 1
| read("(3.0, [])") -- publishes the tuple (3.0, [])
| read("\"hi\"") -- publishes the string "hi"]]></programlisting>
--}
site read = orc.lib.str.Read

{--
@site write(Top) :: String
Given an Orc value, return its string representation
using standard Orc literal syntax.  If the value is
of a type with no literal syntax,
(for example, it is a site), return an arbitrary string
representation which is intended to be human-readable.

Example:
<programlisting language="orc-demo"><![CDATA[
  write(true) -- publishes "true"
| write(1) -- publishes "1"
| write((3.0, [])) -- publishes "(3.0, [])"
| write("hi") -- publishes "\"hi\""]]></programlisting>
--}
site write = orc.lib.str.Write

{--
@def lines(String) :: List[String]
Split a string into lines, which are substrings
terminated by an endline or the end of the string.
DOS, Mac, and Unix endline conventions are all accepted.
Endline characters are not included in the result.

@implementation
--}
def lines(String) :: List[String]
def lines(text) =
  (
  val out = text.split("\n|\r\n|\r")
  if out.get(out.length()-1) = "" then
    out.split(0, out.length()-1)
  else out
  ) :!: List[String]

{--
@def unlines(List[String]) :: String
Append a linefeed, "\n", to each string in the sequence
and concatenate the results.

@implementation
--}
def unlines(List[String]) :: String
def unlines(line:lines) = cat(line, "\n", unlines(lines))
def unlines([]) = ""
  
{--
@def words(String) :: List[String]
Split a string into words, which are sequences of non-whitespace characters separated by whitespace.

@implementation
--}

def words(String) :: List[String]
def words(text) = (text.trim().split("\\s+")) :!: List[String]


{--
@def unwords(List[String]) :: String
Concatenate a sequence of strings with a single space between
each string.

@implementation
--}
def unwords(List[String]) :: String
def unwords([]) = ""
def unwords([word]) = word
def unwords(word:words) = cat(word, " ", unwords(words))
