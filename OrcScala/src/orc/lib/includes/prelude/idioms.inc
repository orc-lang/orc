--
-- idioms.inc -- Orc standard prelude include, idioms section
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
Higher-order Orc programming idioms.
Many of these are standard functional-programming
combinators borrowed from Haskell or Scheme.
--}

{--
@def curry[A,B,C](lambda (A,B) :: C) :: lambda(A) :: lambda(B) :: C
Curry a function of two arguments.

@implementation
--}
def curry[A,B,C](lambda (A,B) :: C) :: lambda(A) :: lambda(B) :: C
def curry(f) = def inner(x :: A) = { f(x,_ :: B) } # inner

{--
@def curry3[A,B,C,D](lambda (A,B,C) :: D) :: lambda(A) :: lambda(B) :: lambda(C) :: D
Curry a function of three arguments.

@implementation
--}
def curry3[A,B,C,D](lambda (A,B,C) :: D) :: lambda(A) :: lambda(B) :: lambda(C) :: D
def curry3(f) = 
  def innerX(x :: A) = 
    def innerY(y :: B) = 
      { f(x, y, _ :: C) }
    innerY
  innerX

{--
@def uncurry[A,B,C](lambda (A) :: lambda(B) :: C) :: lambda(A, B) :: C
Uncurry a function of two arguments.

@implementation
--}
def uncurry[A,B,C](lambda (A) :: lambda(B) :: C) :: lambda(A, B) :: C
def uncurry(f) = { f(_)(_) }

{--
@def uncurry3[A,B,C,D](lambda (A)(B)(C) :: D) :: lambda(A,B,C) :: D
Uncurry a function of three arguments.

@implementation
--}
def uncurry3[A,B,C,D](lambda (A) :: lambda(B) :: lambda(C) :: D) :: lambda(A,B,C) :: D
def uncurry3(f) = { f(_)(_)(_) }

{--
@def flip[A,B,C](lambda (A, B) :: C) :: lambda(B, A) :: C
Flip the order of parameters of a two-argument function.

@implementation
--}
def flip[A,B,C](lambda (A, B) :: C) :: lambda(B, A) :: C
def flip(f) = def inner(x,y) = f(y,x) # inner

{--
@def constant[A](A) :: lambda() :: A
Create a function which returns a constant value.

@implementation
--}
def constant[A](A) :: lambda() :: A
def constant(x) = { x }

{--
@def defer[A,B](lambda (A) :: B, A) :: lambda() :: B
Given a function and its argument, return a thunk which applies the function.

@implementation
--}
def defer[A,B](lambda (A) :: B, A) :: lambda() :: B
def defer(f, x) = { f(x) }

{--
@def defer2[A,B,C](lambda (A,B) :: C, A, B) :: lambda() :: C
Given a function and its arguments, return a thunk which applies the function.

@implementation
--}
def defer2[A,B,C](lambda (A,B) :: C, A, B) :: lambda() :: C
def defer2(f, x, y) = { f(x, y) }

{--
@def ignore[A](lambda () :: A) :: lambda(Top) :: B
From a function of no arguments, create a function
of one argument, which is ignored.

@implementation
--}
def ignore[A](lambda () :: A) :: lambda(Top) :: A
def ignore(f) = def inner(_) = f() # inner

{--
@def ignore2[A,B,C](lambda () :: C) :: lambda(A, B) :: C
From a function of no arguments, create a function
of two arguments, which are ignored.

@implementation
--}
def ignore2[A,B,C](lambda () :: C) :: lambda(A, B) :: C
def ignore2(f) = def inner(_, _) = f() # inner

{--
@def compose[A,B,C](lambda (B) :: C, lambda (A) :: B) :: lambda(A) :: C
Compose two single-argument functions.

@implementation
--}
def compose[A,B,C](lambda (B) :: C, lambda (A) :: B) :: lambda (A) :: C
def compose(f,g) = { f(g(_)) }

{--
@def while[A](lambda (A) :: Boolean, lambda (A) :: A) :: lambda(A) :: A

Iterate a function while a predicate is satisfied, <link linkend="ref.concepts.publish">publishing</link>
each value passed to the function.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 0 1 2 3 4 5
while(
  lambda (n) = (n <= 5),
  lambda (n) = n+1
)(0)]]></programlisting>

@implementation
--}
def while[A](lambda (A) :: Boolean, lambda (A) :: A) :: lambda(A) :: A
def while(p,f) =
  def loop(A) :: A
  def loop(x) = Ift(p(x)) >> ( x | loop(f(x)) )
  loop

{--
@def repeat[A](lambda () :: A) :: A
Call a function sequentially, <link linkend="ref.concepts.publish">publishing</link> each value returned by the function.
The expression <code>repeat(f)</code> is equivalent to
the infinite expression <code>f() >x> ( x | f() >x> ( x | f() >x> ... ) )</code>

@implementation
--}
def repeat[A](lambda () :: A) :: A
def repeat(f) = f() >x> (x | repeat(f))

{--
@def fork[A](List[lambda () :: A]) :: A
Call a list of functions in parallel, <link linkend="ref.concepts.publish">publishing</link>
all values published by the functions.

The expression <code>fork([f,g,h])</code> is equivalent to
the expression <code>f() | g() | h()</code>

@implementation
--}
def fork[A](List[lambda () :: A]) :: A
def fork([]) = stop
def fork(p:ps) = p() | fork(ps)

{--
@def forkMap[A,B](lambda (A) :: B, List[A]) :: B
Apply a function to a list in parallel, <link linkend="ref.concepts.publish">publishing</link> all values published
by the applications.

The expression <code>forkMap(f, [a,b,c])</code> is equivalent to
the expression <code>f(a) | f(b) | f(c)</code>

This function can be thought of as the following composition
<code>fork(map(curry(defer)(f), xs))</code> (hence the name <code>forkMap</code>).
It maps <code>f</code> over the list and then forks each of the
resulting computations.

@implementation
--}
def forkMap[A,B](lambda (A) :: B, List[A]) :: B
def forkMap(f, []) = stop
def forkMap(f, x:xs) = f(x) | forkMap(f, xs)

{--
@def seq[A](List[lambda () :: A]) :: Signal
Call a list of functions in sequence, <link linkend="ref.concepts.publish">publishing</link>
a <link linkend="ref.data.signal">signal</link> whenever the last function publishes. The
actual publications of the given functions are not
published.

The expression <code>seq([f,g,h])</code> is equivalent to
the expression <code>f() >> g() >> h() >> signal</code>

@implementation
--}
def seq[A](List[lambda () :: A]) :: Signal
def seq([]) = signal
def seq(p:ps) = p() >> seq(ps)

{--
@def seqMap[A,B](lambda (A) :: B, List[A]) :: Signal
Apply a function to a list in sequence, <link linkend="ref.concepts.publish">publishing</link>
a <link linkend="ref.data.signal">signal</link> whenever the last application publishes. The
actual publications of the given functions are not
<link linkend="ref.concepts.publish">published</link>.

The expression <code>seqMap(f, [a,b,c])</code> is equivalent to
the expression <code>f(a) >> f(b) >> f(c) >> signal</code>

This function can be thought of as the following composition
<code>seq(map(curry(defer)(f), xs))</code> (hence the name <code>seqMap</code>).
It maps <code>f</code> over the list and then runs each of the
resulting computations in sequence. This is analogous to <code>foreach</code> in Scala
or <code>iterate</code> in OCaml.

@implementation
--}
def seqMap[A,B](lambda (A) :: B, List[A]) :: Signal
def seqMap(f, []) = signal
def seqMap(f, x:xs) = f(x) >> seqMap(f, xs)

{--
@def join[A](List[lambda () :: A]) :: Signal
Call a list of functions in parallel and <link linkend="ref.concepts.publish">publish</link>
a <link linkend="ref.data.signal">signal</link> once all functions have completed.

The expression <code>join([f,g,h])</code> is equivalent to
the expression <code>(f(), g(), h()) >> signal</code>

@implementation
--}
def join[A](List[lambda () :: A]) :: Signal
def join([]) = signal
def join(p:ps) = (p(), join(ps)) >> signal

{--
@def joinMap[A](lambda (A) :: Top, List[A]) :: Signal
Apply a function to a list in parallel and <link linkend="ref.concepts.publish">publish</link>
a <link linkend="ref.data.signal">signal</link> once all applications have completed.

The expression <code>joinMap(f, [a,b,c])</code> is equivalent to
the expression <code>(f(a), f(b), f(c)) >> signal</code>

This function can be thought of as the following composition
<code>join(map(curry(defer)(f), xs))</code> (hence the name <code>joinMap</code>).

@implementation
--}
def joinMap[A](lambda (A) :: Top, List[A]) :: Signal
def joinMap(f, []) = signal
def joinMap(f, x:xs) = (f(x), joinMap(f, xs)) >> signal

{--
@def alt[A](List[lambda () :: A]) :: A
Call each function in the list until one of them <link linkend="ref.concepts.publish">publishes</link>
(choosing the publishing alternative).

The expression <code>alt([f,g,h])</code> is equivalent to
the expression <code>f() ; g() ; h()</code>

@implementation
--}
def alt[A](List[lambda () :: A]) :: A
def alt([]) = stop
def alt(p:ps) = p() ; alt(ps)

{--
@def altMap[A,B](lambda (A) :: B, List[A]) :: B
Apply the function to each element in the list until one <link linkend="ref.concepts.publish">publishes</link>.

The expression <code>altMap(f, [a,b,c])</code> is equivalent to
the expression <code>f(a) ; f(b) ; f(c)</code>

This function can be thought of as the following composition
<code>alt(map(curry(defer)(f), xs))</code> (hence the name <code>altMap</code>).

@implementation
--}
def altMap[A,B](lambda (A) :: B, List[A]) :: B
def altMap(f, []) = stop
def altMap(f, x:xs) = f(x) ; altMap(f, xs)

{--
@def por(List[lambda () :: Boolean]) :: Boolean
Parallel or. Execute a list of boolean functions in parallel, <link linkend="ref.concepts.publish">publishing</link>
a value as soon as possible, and <link linkend="ref.concepts.states.kill">killing</link> any unnecessary ongoing
computation.

@implementation
--}
def por(List[lambda () :: Boolean]) :: Boolean
def por([]) = false
def por(p:ps) =
  {|
    val b1 = p()
    val b2 = por(ps)
    Ift(b1) >> true | Ift(b2) >> true | (b1 || b2)
  |}

{--
@def pand(List[lambda () :: Boolean]) :: Boolean
Parallel and. Execute a list of boolean functions in parallel, <link linkend="ref.concepts.publish">publishing</link>
a value as soon as possible, and <link linkend="ref.concepts.states.kill">killing</link> any unnecessary ongoing
computation.

@implementation
--}
def pand(List[lambda () :: Boolean]) :: Boolean
def pand([]) = true
def pand(p:ps) =
  {|
    val b1 = p()
    val b2 = pand(ps)
    Iff(b1) >> false | Iff(b2) >> false | (b1 && b2)
  |}

{--
@def collect[A](lambda () :: A) :: List[A]
Run a function, collecting all publications in a list.
<link linkend="ref.concepts.publish">Publish</link> the list when the function <link linkend="ref.concepts.states.halt">halts</link>.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [signal, signal, signal, signal, signal]
collect(defer(signals, 5))]]></programlisting>

@implementation
--}
def collect[A](lambda () :: A) :: List[A]
def collect(p) =
  val b = Channel[A]()
  p() >x> b.put(x) >> stop
  ; b.getAll()
