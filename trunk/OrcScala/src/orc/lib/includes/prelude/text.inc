--
-- text.inc -- Orc standard prelude include, strings section
-- Project OrcScala
--
-- $Id$
--
-- Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
--
-- Use and redistribution of this file is governed by the license terms in
-- the LICENSE file found in the project's top-level directory and also found at
-- URL: http://orc.csres.utexas.edu/license.shtml .
--

{--
Operations on strings.
--}

{--
@site String

<link linkend="ref.data.string">Strings</link> themselves have a set of methods associated with them. These methods can
be invoked on any string literal or any variable bound to a string.

The methods documented here are only a subset of those available in the Java
implementation. In practice, strings in the Java implementation support all 
methods provided by Java's <code>String</code> class.

@method string.length() :: Integer
Return the length of the string.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 4
"four".length()]]></programlisting>

@method string.substring(Integer, Integer) :: String
Return the substring of this string covered by the given half-open range.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: "orc"
val s = "apple orchard"
s.substring(6,9)]]></programlisting>

@method string.indexOf(String) :: Integer
Return the starting index of the first occurrence of the given string.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 6
"apple orchard".indexOf("orc")]]></programlisting>
--}

{--
@site Print(Top) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Print a value as a string to standard output.
For Java objects, this will call <code>toString()</code> to convert
the object to a String.

@site Println(Top) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Same as <code>Print</code>, appending a newline.
--}
import site Print = "orc.lib.str.Print"
import site Println = "orc.lib.str.Println"

{--
@site Read[A](String) :: A

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="pure"/>
</od:sitepropset>

Given a string representing an Orc value (using standard
Orc literal syntax), return the corresponding value. If
the argument does not conform to Orc literal syntax,
<link linkend="ref.concepts.states.halt">halt</link> with an error.

Example:
<programlisting language="orc-demo"><![CDATA[
  Read("true") -- publishes the boolean true
| Read("1") -- publishes the integer 1
| Read("(3.0, [])") -- publishes the tuple (3.0, [])
| Read("\"hi\"") -- publishes the string "hi"]]></programlisting>
--}
import site Read = "orc.lib.str.Read"

{--
@site Write(Top) :: String

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="pure"/>
</od:sitepropset>

Given an Orc value, return its string representation
using standard Orc literal syntax.  If the value is
of a type with no literal syntax,
(for example, it is a site), return an arbitrary string
representation which is intended to be human-readable.

Example:
<programlisting language="orc-demo"><![CDATA[
  Write(true) -- publishes "true"
| Write(1) -- publishes "1"
| Write((3.0, [])) -- publishes "(3.0, [])"
| Write("hi") -- publishes "\"hi\""]]></programlisting>
--}
import site Write = "orc.lib.str.Write"

{--
@def lines(String) :: List[String]
Split a string into lines, which are substrings
terminated by an endline or the end of the string.
DOS, Mac, and Unix endline conventions are all accepted.
Endline characters are not included in the result.

@implementation
--}
def lines(String) :: List[String]
def lines(text) = arrayToList(text.split("\n|\r\n|\r"))

{--
@def unlines(List[String]) :: String
Append a linefeed, "\n", to each string in the sequence
and concatenate the results.

@implementation
--}
def unlines(List[String]) :: String
def unlines(line:lines) = line + "\n" + unlines(lines)
def unlines([]) = ""
  
{--
@def words(String) :: List[String]
Split a string into words, which are sequences of non-whitespace characters separated by whitespace.

@implementation
--}

def words(String) :: List[String]
def words(text) = arrayToList(text.trim().split("\\s+"))


{--
@def unwords(List[String]) :: String
Concatenate a sequence of strings with a single space between
each string.

@implementation
--}
def unwords(List[String]) :: String
def unwords([]) = ""
def unwords([word]) = word
def unwords(word:words) = word + " " + unwords(words)

{--
@def characters(String) :: List[String]
Convert a string to a list of strings of length 1,
where the list items are the characters of the given string.

Example:
characters("Hello") = ["H", "e", "l", "l", "o"]

@implementation
--}
def characters(String) :: List[String]
def characters(s) =
  def rest(Integer) :: List[String]
  def rest(i) =
      Ift (i >= s.length()) >> []
    | Ift (i <: s.length()) >> s.substring(i,i+1):rest(i+1)
  rest(0)
