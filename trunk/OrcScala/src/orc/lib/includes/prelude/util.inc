--
-- util.inc -- Orc standard prelude include, utility section
-- Project OrcScala
--
-- $Id$
--
-- Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
--
-- Use and redistribution of this file is governed by the license terms in
-- the LICENSE file found in the project's top-level directory and also found at
-- URL: http://orc.csres.utexas.edu/license.shtml .
--

{--
Miscellaneous utility functions.
--}

{--
@site Random() :: Integer
Return a random <link linkend="ref.data.numeric">integer</link>
chosen from the range of all possible 32-bit integers.

@site Random(Integer) :: Integer
Return a pseudorandom, uniformly distributed <link linkend="ref.data.numeric">integer</link>
between 0 (inclusive) and the specified value (exclusive).
If the argument is 0, <link linkend="ref.concepts.states.halt">halt</link> <link linkend="ref.concepts.silent">silently</link>.
--}
import site Random = "orc.lib.util.Random"

{--
@site URandom() :: Number
Return a pseudorandom, uniformly distributed <link linkend="ref.data.numeric">number</link>
between 0.0 (inclusive) and 1.0 (exclusive).
--}
import site URandom = "orc.lib.util.URandom"

{--
@site UUID() :: String
Return a random (type 4) UUID represented as a <link linkend="ref.data.string">string</link>.
--}
import site UUID = "orc.lib.util.UUID"

{--
@site Prompt(String) :: String

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

Prompt the user for some input. The user may cancel the prompt,
in which case the site fails <link linkend="ref.concepts.silent">silently</link>. Otherwise their response
is returned as soon as it is received.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes the user's name
Prompt("What is your name?")]]></programlisting>

The user response is always taken to be a string. Thus, integer 
3 as a response will be treated as "3". To convert the response 
to its appropriate data type, use the library function
<link linkend="ref.stdlib.text.Read"><code>Read</code></link>:

<programlisting language="orc-demo"><![CDATA[
-- Prompts the user to enter an integer, then parses the response.
Prompt("Enter an integer:") >r> Read(r)]]></programlisting>
--}
import site Prompt = "orc.lib.util.Prompt"

{--
@def signals(Integer) :: Signal
<link linkend="ref.concepts.publish">Publish</link> the given number of <link linkend="ref.data.signal">signals</link>, simultaneously.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes five signals
signals(5)]]></programlisting>

@implementation
--}
def signals(Integer) :: Signal
def signals(n) = if n :> 0 then (signal | signals(n-1)) else stop

{--
@def for(Integer, Integer) :: Integer
<link linkend="ref.concepts.publish">Publish</link> all values in the given half-open range, simultaneously.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 1 2 3 4 5
for(1,6)]]></programlisting>

@implementation
--}
def for(Integer, Integer) :: Integer
def for(low, high) =
  if low >= high then stop
  else ( low | for(low+1, high) )

{--
@def upto(Integer) :: Integer
<code>upto(n)</code> <link linkend="ref.concepts.publish">publishes</link> all values in the range <code>(0..n-1)</code>
simultaneously.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 0 1 2 3 4
upto(5)]]></programlisting>

@implementation
--}
def upto(Integer) :: Integer
def upto(high) = for(0, high)



import class Iterable = "java.lang.Iterable"

{--
@site IterableToStream[A](Iterable[A]) :: lambda () :: A 

Converts a Java object implementing the Iterable interface
into an Orc stream backed by the object's iterator. 
When the site is called, if the iterator has items remaining,
the next item is returned. If the iterator has no items
remaining, the call <link linkend="ref.concepts.states.halt">halts</link>.
--}
import site IterableToStream = "orc.lib.util.IterableToStream"

{--
@def iterableToList[A](Iterable[A]) :: List[A]
Given a Java object implementing the Iterable interface, 
create an Orc <link linkend="ref.data.list">list</link> whose elements are the values produced by
the object's iterator, in the same order. 

@implementation
--}
def iterableToList[A](Iterable[A]) :: List[A]
def iterableToList(iterable) = 
  val s = IterableToStream[A](iterable)
  def walk(List[A]) :: List[A]
  def walk(l) = s():l >m> walk(m) ; reverse(l) 
  walk([]) 

{--
@def arrayToList[A](Array[A]) :: List[A]
Given an array, create an Orc <link linkend="ref.data.list">list</link> whose elements are exactly
the contents of the array, in the same order. 

@implementation
--}
def arrayToList[A](Array[A]) :: List[A]
def arrayToList(a) = 
  def walk(Integer, List[A]) :: List[A]
  def walk(0,acc) = acc
  def walk(i,acc) = walk(i-1, a(i-1)? : acc)
  walk(a.length?, [])

{--
@def listToArray[A](List[A]) :: Array[A]
Given an Orc <link linkend="ref.data.list">list</link>, create an array whose elements are exactly
the contents of the list, in the same order. The size of the array is exactly the list length.

@implementation
--}
def listToArray[A](List[A]) :: Array[A]
def listToArray(list) =
  val array = Array[A](length(list))
  def copy(List[A], Integer) :: Bot 
  def copy(x:xs, i) = (array(i) := x) >> copy(xs, i+1)
  copy(list, 0) ; array  

{--
@def fillArray[A](Array[A], lambda (Integer) :: A) :: Array[A]
Given an array and a function from indices to values, populate the array
by calling the function for each index in the array. <link linkend="ref.concepts.publish">Publish</link> the array
once it has been populated.

For example, to set all elements of an array to zero:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 0 0 0
val a = fillArray(Array(3), lambda (_) = 0)
a(0)? | a(1)? | a(2)?]]></programlisting>

@implementation
--}
def fillArray[A](Array[A], lambda (Integer) :: A) :: Array[A]
def fillArray(a, f) =
  val n = a.length?
  def fill(Integer, lambda(Integer) :: A) :: Bot
  def fill(i, f) =
    if i = n then stop
    else ( a(i) := f(i) >> stop
         | fill(i+1, f) )
  fill(0, f) ; a

{--
@def sliceArray[A](Array[A], Integer, Integer) :: Array[A]

Given an array and a half-open index range, create a new array
which contains the elements of the original array in that index
range.

@implementation
--}
def sliceArray[A](Array[A], Integer, Integer) :: Array[A]
def sliceArray(orig, from, until) =
  val size = until - from
  val a = Array[A](size)
  def copy(i :: Integer) :: Bot =
    Ift(i <: size) >> 
    a(i) := orig(from + i)? >> 
    copy(i+1)
  copy(0) ; a

{--
@def takePubs[A](Integer, lambda () :: A) :: A
<code>takePubs(n, f)</code> calls <code>f()</code>,
<link linkend="ref.concepts.publish">publishes</link> the first <code>n</code> values published
by <code>f()</code> (as they are published), and then
<link linkend="ref.concepts.states.halt">halts</link>.

@implementation
--}

def takePubs[A](Integer, lambda () :: A) :: A
def takePubs(n, f) =
  val out = Channel[A]()
  val c = Counter(n)
  Let(
    f() >x>
    Ift(c.dec() >> out.put(x) >> false
       ; out.closeD() >> true)
  ) >> stop | repeat(out.get)

{--
@def withLock[A](Semaphore, lambda () :: A) :: A
Acquire the semaphore and run a thunk which is expected to <link linkend="ref.concepts.publish">publish</link>
no more than one value. Publishes the value published by the
thunk and releases the semaphore.

@implementation
--}

def withLock[A](Semaphore, lambda () :: A) :: A
def withLock(s, f) =
  s.acquire() >> (
    Let(f()) >x>
    s.release() >>
    x
    ; s.release() >> stop
  )


{--
@def synchronized[A](Semaphore, lambda () :: A)() :: A
Given a lock and thunk, return a new thunk which is serialized
on the lock. Similar to Java's synchronized keyword.

@implementation
--}

def synchronized[A](Semaphore, lambda () :: A) :: lambda() :: A
def synchronized(s,f) = lambda() = withLock(s, f)

{--
@def def class InvokeExecutable(command :: List[String])

Run an executable command in the host OS, with input/output addressable from
Orc.

Assumptions:
<ul>
<li>The supplied "command" string refers to an executable file that is
understandable by the OS hosting the JVM.</li>
<li>Input and output to the executable program are character strings encoded
in the host OS's default character encoding.</li>
</ul>

@method invokeexecutable.writeIn(String) :: Signal

@method invokeexecutable.readOut() :: String

@method invokeexecutable.readOutD() :: String

@method invokeexecutable.readOutAll() :: String

@method invokeexecutable.readErr() :: String

@method invokeexecutable.readErrD() :: String

@method invokeexecutable.readErrAll() :: String

@method invokeexecutable.exitValue() :: Integer

@method invokeexecutable.kill() :: Signal

--}
 
import class ProcessBuilder = "java.lang.ProcessBuilder"
import class Reader = "java.io.Reader"
import class InputStreamReader = "java.io.InputStreamReader"
import class Writer = "java.io.Writer"
import class OutputStreamWriter = "java.io.OutputStreamWriter"
import class JavaConversions = "scala.collection.JavaConversions"
import class JavaCharacter = "java.lang.Character"
import class JavaString = "java.lang.String"
import class JavaList = "java.util.List"

def class InvokeExecutable(command :: List[String]) =
  -- Note: Inheriting current processes's working directory and environment.
  val p = ProcessBuilder(JavaConversions.asJavaList(command)).start()
  -- FIXME: Orc typechecker problems here

  val execInChan = Channel[String]()
  val execInWriter = OutputStreamWriter(p.getOutputStream())
  val execOutChan = Channel[String]()
  val execOutReader = InputStreamReader(p.getInputStream())
  val execOutEOF = Cell[Signal]()
  val execErrChan = Channel[String]()
  val execErrReader = InputStreamReader(p.getErrorStream())
  val execErrEOF = Cell[Signal]()
  def writeIn(s :: String) = execInChan.put(s)
  def readOut() = execOutChan.get()
  def readOutD() = execOutChan.getD()
  def readOutAll() = execOutEOF.read() >> execOutChan.getAll() >ss> foldl1(lambda(l :: String, r :: String) = l + r, ss)
  def readErr() = execErrChan.get()
  def readErrD() = execErrChan.getD()
  def readErrAll() = execErrEOF.read() >> execErrChan.getAll() >ss> foldl1(lambda(l :: String, r :: String) = l + r, ss)
  def exitValue() = p.exitValue()
  def kill() = p.destroy()
  val buff1 = Array[JavaCharacter](8192, "char")
  val buff2 = Array[JavaCharacter](8192, "char")
  def copyToChan(javaReader :: Reader, buff :: Array[JavaCharacter], orcChan :: Channel[String]) :: Bot  =
    javaReader.read(buff)  >charsRead>
    ( Ift(charsRead :> 0) >> orcChan.put(JavaString(buff, 0, charsRead)) >> copyToChan(javaReader, buff, orcChan)
    | Ift(charsRead = 0) >> {- not expected, busy wait -} Rwait(1) >> copyToChan(javaReader, buff, orcChan)
      {- charsRead < 0 means EOF, so we will halt silently -}
    )
  def copyFromChan(orcChan :: Channel[String], javaWriter :: Writer) :: Bot =
    javaWriter.write(orcChan.get()) >> copyFromChan(orcChan, javaWriter) #

  ( p.waitFor() >> execInChan.closeD() >> execInWriter.close() 
  | (copyFromChan(execInChan, execInWriter))
  | (copyToChan(execOutReader, buff1, execOutChan) ; execOutChan.closeD() >> execOutEOF.write(signal) >> execOutReader.close())
  | (copyToChan(execErrReader, buff2, execErrChan) ; execErrChan.closeD() >> execErrEOF.write(signal) >> execErrReader.close())
  )
