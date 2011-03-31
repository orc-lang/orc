--
-- core.inc -- Orc standard prelude include, fundementals section
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
Fundamental sites and operators.

These declarations include both prefix and infix sites (operators).  For
consistency, all declarations are written in prefix form, with the site name
followed by the operands.  When the site name is surrounded in parentheses, as
in <code>(+)</code>, it denotes an infix operator.

For a more complete description of the built-in operators and their syntax, see
the <link linkend="ref.expressions.operators">Operators</link> article.
--}

{-
Basic types
-}
type String = "orc.types.StringType"
type Number = "orc.types.NumberType"
type Boolean = "orc.types.BooleanType"
type Integer = "orc.types.IntegerType"
type Signal = "orc.types.SignalType"

{--
@site Let() :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="pure"/>
</od:sitepropset>

When applied to no arguments, return a signal.


@site Let(A) :: A
When applied to a single argument, return that argument (behaving as the identity function).

@site Let(A, ...) :: (A, ...)
When applied to two or more arguments, return the arguments as a tuple.
--}
site Let = "orc.lib.builtin.Let"

{--
@site Ift(Boolean) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="pure"/>
</od:sitepropset>

<link linkend="ref.concepts.states.halt">Halt</link> <link linkend="ref.concepts.silent">silently</link> if the argument is false. Otherwise return a signal.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: "Always publishes"
  Ift(false) >> "Never publishes"
| Ift(true) >> "Always publishes"]]></programlisting>

Note: <code>If</code>, <code>IfT</code>, and <code>IfTrue</code>
are all aliases for <code>If</code>.

--}
site Ift = "orc.lib.builtin.If"

{--
@site Iff(Boolean) :: Signal

<od:sitepropset>
   <od:siteprop propname="definite"/>
   <od:siteprop propname="pure"/>
</od:sitepropset>

<link linkend="ref.concepts.states.halt">Halt</link> <link linkend="ref.concepts.silent">silently</link> if the argument is true. Otherwise return a signal.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: "Always publishes"
  Iff(false) >> "Always publishes"
| Iff(true) >> "Never publishes"]]></programlisting>

Note: <code>Unless</code>, <code>IfF</code>, and <code>IfFalse</code>
are all aliases for <code>Iff</code>.

--}
site Iff = "orc.lib.builtin.Unless"

{- Aliases for different styles of conditional site usage -}
site If = "orc.lib.builtin.If"
site Unless = "orc.lib.builtin.Unless"

{--
@site Error(String) :: Bot

<od:sitepropset>   
   <od:siteprop propname="definite"/>
</od:sitepropset>

<link linkend="ref.concepts.states.halt">Halt</link> <link linkend="ref.concepts.silent">silently</link> after outputting the given error message.

Example, using <code>Error</code> to implement assertions:
<programlisting language="orc-demo"><![CDATA[
def assert(b) =
  if b then signal else Error("assertion failed")

-- Fail with the error message: "assertion failed"
assert(false)]]></programlisting>
--}
site Error = "orc.lib.util.Error"


{--
@site (+)(Number, Number) :: Number
<code>a+b</code> returns the sum of <code>a</code> and <code>b</code>.
--}
site (+) = "orc.lib.math.Add"

{--
@site (-)(Number, Number) :: Number
<code>a-b</code> returns the value of <code>a</code> minus the value of <code>b</code>.
--}
site (-) = "orc.lib.math.Sub"

{--
@site (0-)(Number) :: Number
Return the additive inverse of the argument.
When this site appears as an operator, it is written in prefix form without the
zero, i.e. <code>(-a)</code>
--}
site (0-) = "orc.lib.math.UMinus"

{--
@site (*)(Number, Number) :: Number
<code>a*b</code> returns the product of <code>a</code> and <code>b</code>.
--}
site (*) = "orc.lib.math.Mult"

{--
@site (**)(Number, Number) :: Number
<code>a ** b</code> returns
<inlineequation><mathphrase>a<superscript>b</superscript></mathphrase></inlineequation>,
i.e. <code>a</code> raised to the <code>b</code>th power.
--}
site (**) = "orc.lib.math.Exponent"

{--
@site (/)(Number, Number) :: Number
<code>a/b</code> returns <code>a</code> divided by <code>b</code>.
If both arguments have integral types, <code>(/)</code> performs integral
division, rounding towards zero. Otherwise, it performs floating-point
division. If <code>b=0</code>, <code>a/b</code> <link linkend="ref.concepts.states.halt">halts</link> with an error.

Example:
<programlisting language="orc-demo"><![CDATA[
  7/3   -- publishes 2
| 7/3.0 -- publishes 2.333...]]></programlisting>
--}
site (/) = "orc.lib.math.Div"

{--
@site (%)(Number, Number) :: Number
<code>a%b</code> computes the remainder of <code>a/b</code>. If <code>a</code>
and <code>b</code> have integral types, then the remainder is given by
the expression <code>a - (a/b)*b</code>. For a full description, see the
<link xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="http://java.sun.com/docs/books/jls/third_edition/html/expressions.html#15.17.3">Java Language Specification, 3rd edition</link>.
--}
site (%) = "orc.lib.math.Mod"

{--
@site (<:)(Top, Top) :: Boolean
<code>a &lt;: b</code> returns true if <code>a</code> is less than <code>b</code>, and false otherwise.
--}
site (<:) = "orc.lib.comp.Less"

{--
@site (<=)(Top, Top) :: Boolean
<code>a &lt;= b</code> returns true if <code>a</code> is less than or equal to <code>b</code>, and false otherwise.
--}
site (<=) = "orc.lib.comp.Leq"

{--
@site (:>)(Top, Top) :: Boolean
<code>a :> b</code> returns true if <code>a</code> is greater than <code>b</code>, and false otherwise.
--}
site (:>) = "orc.lib.comp.Greater"

{--
@site (>=)(Top, Top) :: Boolean
<code>a >= b</code> returns true if <code>a</code> is greater than or equal to <code>b</code>, and false otherwise.
--}
site (>=) = "orc.lib.comp.Greq"

{--
@site (=)(Top, Top) :: Boolean
<code>a = b</code> returns true if <code>a</code> is equal to <code>b</code>,
and false otherwise.  The precise definition of "equal" depends on the values
being compared, but always obeys the rule that if two values are considered
equal, then one may be substituted locally for the other without affecting the
behavior of the program.

Two values with the same object identity are always considered equal.
Orc data structures, such as records, are equal if their contents are equal.
Other types are free to implement their own equality
relationship provided it conforms to the rules given here.

Note that although values of different types may be compared with
<code>=</code>, the substitutability principle requires that such values are
always considered inequal, i.e. the comparison will return <code>false</code>.
--}
site (=) = "orc.lib.builtin.Eq"

{--
@site (/=)(Top, Top) :: Boolean
<code>a/=b</code> returns false if <code>a=b</code>, and true otherwise.
--}
site (/=) = "orc.lib.comp.Inequal"

{--
@site (~)(Boolean) :: Boolean
Return the logical negation of the argument.
--}
site (~) = "orc.lib.bool.Not"

{--
@site (&&)(Boolean, Boolean) :: Boolean
Return the logical conjunction of the arguments. This is not a short-circuiting
operator; both arguments must publish before the result is
computed.
--}
site (&&) = "orc.lib.bool.And"

{--
@site (||)(Boolean, Boolean) :: Boolean
Return the logical disjunction of the arguments. This is not a short-circuiting
operator; both arguments must publish before the result is
computed.
--}
site (||) = "orc.lib.bool.Or"

{--
@site (:)[A](A, List[A]) :: List[A]

The list <code>a:b</code> is formed by prepending the element <code>a</code> to
the list <code>b</code>.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: (3, [4, 5])
3:4:5:[] >x:xs> (x,xs)]]></programlisting>
--}
site (:) = "orc.lib.builtin.structured.ConsConstructor"

{--
@def abs(Number) :: Number
Return the absolute value of the argument.

@implementation
--}
def abs(Number) :: Number
def abs(x) = if x <: 0 then -x else x

{--
@def signum(Number) :: Number
<code>signum(a)</code> returns <code>-1</code> if <code>a&lt;0</code>,
<code>1</code> if <code>a&gt;0</code>, and <code>0</code> if <code>a=0</code>.

@implementation
--}
def signum(Number) :: Number
def signum(x) =
  if x <: 0 then -1
  else if x :> 0 then 1
  else 0

{--
@def min[A](A,A) :: A
Return the lesser of the arguments. If the arguments
are equal, return the first argument.

@implementation
--}
def min[A](A,A) :: A
def min(x,y) = if y <: x then y else x

{--
@def max[A](A,A) :: A
Return the greater of the arguments. If the arguments
are equal, return the second argument.

@implementation
--}
def max[A](A,A) :: A
def max(x,y) = if x :> y then x else y


{--
@site Floor(Number) :: Integer
Return the greatest integer less than
or equal to this number.
--}
site Floor = "orc.lib.math.Floor"

{--
@site Ceil(Number) :: Integer
Return the least integer greater than
or equal to this number.
--}
site Ceil = "orc.lib.math.Ceil"

{--
@def sqrt(Number) :: Integer
Return the square root of this number.

@implementation
--}
def sqrt(Number) :: Number
def sqrt(n) = n ** 0.5

