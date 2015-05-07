{--
Higher-order Orc programming idioms.
Many of these are standard functional-programming
combinators borrowed from Haskell or Scheme.
--}

{--
* site apply(lambda (A, ...) :: B, [A]) :: B
Apply a function to a list of arguments.
--}
site apply = orc.lib.util.Apply

{--
* def curry(lambda (A,B) :: C)(A)(B) :: C
Curry a function of two arguments.
--}
def curry(f)(x)(y) = f(x,y)

{--
* def curry3(lambda (A,B,C) :: D)(A)(B)(C) :: D
Curry a function of three arguments.
--}
def curry3(f)(x)(y)(z) = f(x,y,z)

{--
* def uncurry(lambda (A)(B) :: C)(A, B) :: C
Uncurry a function of two arguments.
--}
def uncurry(f)(x,y) = f(x)(y)

{--
* def uncurry3(lambda (A)(B)(C) :: D)(A,B,C) :: D
Uncurry a function of three arguments.
--}
def uncurry3(f)(x,y,z) = f(x)(y)(z)

{--
* def flip(lambda (A, B) :: C)(B, A) :: C
Flip the order of parameters of a two-argument function.
--}
def flip(f)(x,y) = f(y,x)

{--
* def constant(A)() :: A
Create a function which returns a constant value.
--}
def constant(x)() = x

{--
* def defer(lambda (A) :: B, A)() :: B
Given a function and its argument, return a thunk which applies the function.
--}
def defer(f, x)() = f(x)

{--
* def defer2(lambda (A,B) :: C, A, B)() :: C
Given a function and its arguments, return a thunk which applies the function.
--}
def defer2(f, x, y)() = f(x, y)

{--
* def ignore(lambda () :: B)(A) :: B
From a function of no arguments, create a function
of one argument, which is ignored.
--}
def ignore(f)(_) = f()

{--
* def ignore2(lambda () :: C)(A, B) :: C
From a function of no arguments, create a function
of two arguments, which are ignored.
--}
def ignore2(f)(_, _) = f()

{--
* def compose(lambda (B) :: C, lambda (A) :: B)(A) :: C
Compose two single-argument functions.
--}
def compose(f,g)(x) = f(g(x))

{--
* def while(lambda (A) :: Boolean, lambda (A) :: A)(A) :: A

Iterate a function while a predicate is satisfied, publishing
each value passed to the function. The exact behavior is specified
by the following implementation:

<programlisting><![CDATA[
def while(p,f) = 
  def loop(x) = if(p(x)) >> ( x | loop(f(x)) )
  loop]]></programlisting>

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 0 1 2 3 4 5
while(
  lambda (n) = (n <= 5),
  lambda (n) = n+1
)(0)]]></programlisting>
--}
def while(p,f) = 
  def loop(x) = if(p(x)) >> ( x | loop(f(x)) )
  loop

{--
* def repeat(lambda () :: A) :: A
Call a function sequentially, publishing each value returned by the function.
The expression <code>repeat(f)</code> is equivalent to
the infinite expression <code>f() >!_> f() >!_> f() >!_> ...</code>
--}
def repeat(f) = f() >!x> repeat(f)

{--
* def fork([lambda () :: A]) :: A
Call a list of functions in parallel, publishing
all values published by the functions.

The expression <code>fork([f,g,h])</code> is equivalent to
the expression <code>f() | g() | h()</code>
--}
def fork([]) = stop
def fork(p:ps) = p() | fork(ps)

{--
* def sequence([lambda () :: A]) :: Signal
Call a list of functions in sequence, publishing
a signal whenever the last function publishes. The
actual publications of the given functions are not
published.

The expression <code>sequence([f,g,h])</code> is equivalent to
the expression <code>f() >> g() >> h() >> signal</code>
--}
def sequence([]) = signal
def sequence(p:ps) = p() >> sequence(ps)
 
{--
* def join([lambda () :: A]) :: Signal
Call a list of functions in parallel and publish
a signal once all functions have completed.

The expression <code>join([f,g,h])</code> is equivalent to
the expression <code>f() >> stop | g() >> stop | h() >> stop ; signal</code>
--}
def join(ps) =
  fork(ps) >> stop ; signal

{--
* def por(lambda () :: Boolean, lambda () :: Boolean) :: Boolean
Parallel or. Evaluate two boolean functions in parallel, publishing
a value as soon as possible, and terminating any unnecessary ongoing
computation.
--}
def por(p1, p2) =
  let(
    val b1 = p1()
    val b2 = p2()
    if(b1) >> true | if(b2) >> true | (b1 || b2)
  )
  
{--
* def pand(lambda () :: Boolean, lambda () :: Boolean) :: Boolean
Parallel and. Evaluate two boolean functions in parallel, publishing
a value as soon as possible, and terminating any unnecessary ongoing
computation.
--}
def pand(p1, p2) =
  let(
    val b1 = p1()
    val b2 = p2()
    if(~b1) >> false | if(~b2) >> false | (b1 && b2)
  )
 
{--
* def collect(lambda () :: A) :: [A]
Run a function, collecting all publications in a list.
Return the list when the function terminates.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [signal, signal, signal, signal, signal]
collect(defer(signals, 5))]]></programlisting>
--}
def collect(p) =
  val b = Buffer()
  p() >x> b.put(x) >> stop
  ; b.getAll()