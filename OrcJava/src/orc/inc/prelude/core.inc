{--
* site let<A>(A) :: A
When applied to a single argument, return that argument (behaving as the identity function).

* site let<A, ...>(A, ...) :: (A, ...)
When applied to zero, two, or more arguments, return the arguments in a tuple.
--}
site let = orc.runtime.sites.core.Let

{--
* site if(Boolean) :: Signal
Fail silently if the argument is false.

<programlisting language="orc-demo"><![CDATA[
  if(false) >> "Never publishes"
| if(true) >> "Always publishes"]]></programlisting>
--}
site if = orc.runtime.sites.core.If

{--
* site error(String) :: Bot
Fail with the given error message.

<programlisting language="orc-demo"><![CDATA[
def assert(b) =
  if b then signal else error("assertion failed")
assert(false)]]></programlisting>
--}
site error = orc.runtime.sites.core.Error

{--
* site (+)(Number, Number) :: Number
Add two numbers.
--}
site (+) = orc.lib.math.Add

{--
* site (-)(Number, Number) :: Number
Subtract the right argument from the left.
--}
site (-) = orc.lib.math.Sub

{--
* site (0-)(Number) :: Number
Return the additive inverse of the argument.
This site is written without the 0 when it appears
as an operator, i.e. <code>-a</code>
--}
site (0-) = orc.lib.math.UMinus

{--
* site (*)(Number, Number) :: Number
Multiply two numbers.
--}
site (*) = orc.lib.math.Mult

{--
* site (**)(Number, Number) :: Number
<code>a ** b</code> computes <code>a</code> to the <code>b</code>th power.
--}
site (**) = orc.lib.math.Exponent

{--
* site (/)(Number, Number) :: Number
If both arguments have integral types, perform integral division, rounding
towards zero. Otherwise, perform floating-point division. For example:

<programlisting language="orc-demo"><![CDATA[
  7 / 3 -- publishes 2
| 7 / 3.0 -- publishes 2.333...]]></programlisting>
--}
site (/) = orc.lib.math.Div

{--
* site (%)(Number, Number) :: Number
<code>a % b</code> computes the remainder of <code>a / b</code>, satisfying the equation <code>a = (a/b)*b+(a%b)</code>.
For a full description, see the
<link xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="http://java.sun.com/docs/books/jls/third_edition/html/expressions.html#15.17.3">Java Language Specification, 3rd edition</link>.
--}
site (%) = orc.lib.math.Mod

{--
* site (<)(Comparable, Comparable) :: Boolean
Is the left argument less than the right?
--}
site (<) = orc.lib.comp.Less

{--
* site (<=)(Comparable, Comparable) :: Boolean
Is the left argument less than or equal to the right?
--}
site (<=) = orc.lib.comp.Leq

{--
* site (>)(Comparable, Comparable) :: Boolean
Is the left argument greater than the right?
--}
site (>) = orc.lib.comp.Greater

{--
* site (>=)(Comparable, Comparable) :: Boolean
Is the left argument greater than or equal to the right?
--}
site (>=) = orc.lib.comp.Greq

{--
* site (=)(Top, Top) :: Boolean
Is the left argument equivalent to the right?  Values are considered equivalent
if one can be substituted locally for another without affecting the behavior of the
program.  Therefore two different immutable objects are equivalent if they
share equivalent content, while two different mutable objects are never
equivalent.

When <code>a</code> and <code>b</code> are arbitrary Java objects, <code>a =
b</code> is interpreted as <code>a.equals(b)</code>, which may not follow the
rules given above.
--}
site (=) = orc.runtime.sites.core.Equal

{--
* site (/=)(Top, Top) :: Boolean
Is it not the case that the left argument is equivalent to the right,
as described for <code>(=)</code>?
--}
site (/=) = orc.runtime.sites.core.Inequal

{--
* site (~)(Boolean) :: Boolean
Compute the logical negation of the argument.
--}
site (~) = orc.runtime.sites.core.Not

{--
* site (&&)(Boolean, Boolean) :: Boolean
Compute the logical conjunction of the arguments. This is not a short-circuiting
operator; both arguments must be evaluated and available before the result is
computed.
--}
site (&&) = orc.lib.bool.And

{--
* site (||)(Boolean, Boolean) :: Boolean
Compute the logical disjunction of the arguments. This is not a short-circuiting
operator; both arguments must be evaluated and available before the result is
computed.
--}
site (||) = orc.lib.bool.Or

{--
* site (:)<A>(A, [A]) :: [A]
Prepend an element to a list.

* pattern (:)<A>([A]) :: (A, [A])
Deconstruct a list into a head and tail.
--}
site (:) = orc.runtime.sites.core.Cons
