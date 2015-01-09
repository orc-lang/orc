--
-- util.inc -- Orc standard prelude include, utility section
-- Project OrcScala
--
-- $Id$
--
-- Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
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


import class JavaList = "java.util.List"
import class JavaLinkedList = "java.util.LinkedList"
{--
@def listToJavaList[A](List[A]) :: JavaList[A]
Given an Orc <link linkend="ref.data.list">list</link>,
create a java.util.List with the same elements.

@implementation
--}
def listToJavaList[A](List[A]) :: JavaList[A]
def listToJavaList(list) =
  val javalist = JavaLinkedList[A]()
  def copy(List[A]) :: Bot
  def copy(x:xs) = javalist.add(x) >> copy(xs)
  copy(list) ; javalist

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
val a = fillArray(Array(3), constant(0))
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
  {|
    f() >x>
    Ift(c.dec() >> out.put(x) >> false
       ; out.closeD() >> true)
  |} >> stop | repeat(out.get)

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
    {| f() |} >x>
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
def synchronized(s,f) = { withLock(s, f) }

{--
@site InvokeExecutable(command :: List[String])

Run an executable file in the host OS, with input/output addressable from
Orc.

The executable file is invoked in a new OS process, and is passed the command
arguments given to <code>InvokeExecutable</code>.  The process is created with
an input character stream, to which the Orc program can send strings.  The
process is also created with two output character strings, one for conventional
output and one for error/diagnostic output, from which the Orc program can
read.  Upon process termination, the Orc program can read the exit status
value.

Output streams from the process can be read while the process is running.  The
"chunk" of the stream that is returned from any one read operation is of an
unspecified length, because of buffer sizes and operation granularity between
the sender and receiver.  Callers may need to reassemble output segments prior
to further processing.  If this is inconvenient, there are "read all" methods
that wait for process termination, and then return the output as a single
character string.

Assumptions:
<ul>
<li>The supplied "command" string refers to an executable file that is
understandable by the OS hosting the Orc runtime engine.</li>
<li>Input and output to the executable program are character strings encoded
in the host OS's default character encoding.</li>
</ul>

@method invokeexecutable.writeIn(String) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Send the given string to the process's standard input stream. If the process
has completed, <link linkend="ref.concepts.states.halt">halt</link>
<link linkend="ref.concepts.silent">silently</link>.

@method invokeexecutable.closeIn(String) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="idempotent"/>
</od:sitepropset>

Close the process's standard input stream.

@method invokeexecutable.readOut() :: String

Receive a "chunk" of available characters from the process's standard output
stream.  The chunk size is not specified.  If there is no output available
from the process, wait for some.  If there is no output available and the
process has closed its output stream,
<link linkend="ref.concepts.states.halt">halt</link>
<link linkend="ref.concepts.silent">silently</link>.

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

@method invokeexecutable.readOutD() :: String

Receive a "chunk" of available characters from the process's standard output
stream.  The chunk size is not specified.  If there is no output available
from the process,
<link linkend="ref.concepts.states.halt">halt</link>
<link linkend="ref.concepts.silent">silently</link>.

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

@method invokeexecutable.readOutAll() :: String

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

If the process is running, wait for it to complete; then, receive all remaining
standard output stream contents from the process as a single string. If
there is no further output, <link linkend="ref.concepts.states.halt">halt</link>
<link linkend="ref.concepts.silent">silently</link>.

@method invokeexecutable.readErr() :: String

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

Receive a "chunk" of available characters from the process's standard error
(diagnostic) stream.  The chunk size is not specified.  If there is no output
available from the process, wait for some.  If there is no output available
and the process has closed its output stream,
<link linkend="ref.concepts.states.halt">halt</link>
<link linkend="ref.concepts.silent">silently</link>.

@method invokeexecutable.readErrD() :: String

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Receive a "chunk" of available characters from the process's standard error
(diagnostic) stream.  The chunk size is not specified.  If there is no output
available from the process,
<link linkend="ref.concepts.states.halt">halt</link>
<link linkend="ref.concepts.silent">silently</link>.

@method invokeexecutable.readErrAll() :: String

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

If the process is running, wait for it to complete; then, in any case, receive
all remaining standard error (diagnostic) output from the process as a single
string. If there is no further output,
<link linkend="ref.concepts.states.halt">halt</link>
<link linkend="ref.concepts.silent">silently</link>.

@method invokeexecutable.exitValue() :: Integer

<od:sitepropset>
   <od:siteprop propname="definite"/>
</od:sitepropset>

Returns the exit status byte from the process running the executable.  If the
process has not completed, <link linkend="ref.concepts.states.halt">halt</link>
<link linkend="ref.concepts.silent">silently</link>.  Conventionally, a zero
exit status value indicates a successful termination.

@method invokeexecutable.terminate() :: Signal

<od:sitepropset>
   <od:siteprop propname="indefinite"/>
</od:sitepropset>

Send a termination signal (for example, SIGTERM) to the process running the
executable.  If the process has completed, then this method has no effect.

--}

{-
def class InvokeExecutable(command :: List[String]) =
  	import class ProcessBuilder = "java.lang.ProcessBuilder"
	  import class Reader = "java.io.Reader"
	  import class InputStreamReader = "java.io.InputStreamReader"
	  import class Writer = "java.io.Writer"
	  import class OutputStreamWriter = "java.io.OutputStreamWriter"
	  import class JavaCharacter = "java.lang.Character"
	  import class JavaString = "java.lang.String"

  -- Note: Inheriting current processes's working directory and environment.
  val p = ProcessBuilder(listToJavaList[String](command)).start()

  val execInChan = Channel[String]()
  val execInWriter = OutputStreamWriter(p.getOutputStream())
  val execOutChan = Channel[String]()
  val execOutReader = InputStreamReader(p.getInputStream())
  val execOutEOF = Cell[Signal]()
  val execErrChan = Channel[String]()
  val execErrReader = InputStreamReader(p.getErrorStream())
  val execErrEOF = Cell[Signal]()

  def writeIn(s :: String) = execInChan.put(s)
  def closeIn() = execInChan.closeD()
  def readOut() = execOutChan.get()
  def readOutD() = execOutChan.getD()
  def readOutAll() = execOutEOF.read() >> execOutChan.getAll() >ss> foldl1({ (_ :: String) + (_ :: String) }, ss)
  def readErr() = execErrChan.get()
  def readErrD() = execErrChan.getD()
  def readErrAll() = execErrEOF.read() >> execErrChan.getAll() >ss> foldl1({ (_ :: String) + (_ :: String) }, ss)
  def exitValue() = p.exitValue()
  def terminate() = p.destroy()
  val buff1 = Array[JavaCharacter](8192, "char")
  val buff2 = Array[JavaCharacter](8192, "char")
  def copyToChan(javaReader :: Reader, buff :: Array[JavaCharacter], orcChan :: Channel[String]) :: Bot  =
    javaReader.read(buff)  >charsRead>
    ( Ift(charsRead :> 0) >> orcChan.put(JavaString(buff, 0, charsRead)) >> copyToChan(javaReader, buff, orcChan)
    | Ift(charsRead = 0) >> {- not expected, busy wait -} Rwait(1) >> copyToChan(javaReader, buff, orcChan)
      {- charsRead < 0 means EOF, so we will halt silently -}
    )
  def copyFromChan(orcChan :: Channel[String], javaWriter :: Writer) :: Bot =
    javaWriter.write(orcChan.get()) >> javaWriter.flush() >> copyFromChan(orcChan, javaWriter) #

  ( p.waitFor() >> closeIn() >> {-discard unwritten input-} execInChan.getAll()
  | (copyFromChan(execInChan, execInWriter) ; (execInChan.closeD() >> execInChan.getAll() | execInWriter.close()))
  | (copyToChan(execOutReader, buff1, execOutChan) ; execOutChan.closeD() >> execOutEOF.write(signal) >> execOutReader.close())
  | (copyToChan(execErrReader, buff2, execErrChan) ; execErrChan.closeD() >> execErrEOF.write(signal) >> execErrReader.close())
  )
  -}

{--
@site OrcVersion() :: String
Return a name, version, URL, and copyright string for Orc.
--}
import site OrcVersion = "orc.lib.util.OrcVersion"
