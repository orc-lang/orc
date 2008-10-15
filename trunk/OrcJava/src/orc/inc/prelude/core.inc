{--
Fundamental sites and operators.
For a more complete description of the built-in operators and their syntax, see
the <link linkend="language.base.operators">Operators</link> section of the
User Guide.
--}

{--
* site let<A>(A) :: A
When applied to a single argument, return that argument (behaving as the identity function).

* site let<A, ...>(A, ...) :: (A, ...)
When applied to zero, two, or more arguments, return the arguments in a tuple.
--}
site let = orc.runtime.sites.core.Let

{--
* site if(Boolean) :: Signal
Fail silently if the argument is false. Otherwise return a signal.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: "Always publishes"
  if(false) >> "Never publishes"
| if(true) >> "Always publishes"]]></programlisting>
--}
site if = orc.runtime.sites.core.If

{--
* site error(String) :: Bot
Fail with the given error message.

Example, using <code>error</code> to implement assertions:
<programlisting language="orc-demo"><![CDATA[
def assert(b) =
  if b then signal else error("assertion failed")

-- Fail with the error message: "assertion failed"
assert(false)]]></programlisting>
--}
site error = orc.runtime.sites.core.Error

{--
* site (+)(Number, Number) :: Number
<code>a+b</code> returns the sum of <code>a</code> and <code>b</code>.
--}
site (+) = orc.lib.math.Add

{--
* site (-)(Number, Number) :: Number
<code>a-b</code> returns the value of <code>a</code> minus the value of <code>b</code>.
--}
site (-) = orc.lib.math.Sub

{--
* site (0-)(Number) :: Number
Return the additive inverse of the argument.
When this site appears as an operator, it is written without the zero,
i.e. <code>(-a)</code>
--}
site (0-) = orc.lib.math.UMinus

{--
* site (*)(Number, Number) :: Number
<code>a*b</code> returns the product of <code>a</code> and <code>b</code>.
--}
site (*) = orc.lib.math.Mult

{--
* site (**)(Number, Number) :: Number
<code>a ** b</code> returns
<inlineequation><mathphrase>a<superscript>b</superscript></mathphrase></inlineequation>,
i.e. <code>a</code> raised to the <code>b</code>th power.
--}
site (**) = orc.lib.math.Exponent

{--
* site (/)(Number, Number) :: Number
<code>a/b</code> returns <code>a</code> divided by <code>b</code>.
If both arguments have integral types, <code>/<code> performs integral division, rounding
towards zero. Otherwise, it performs floating-point division.

Example:
<programlisting language="orc-demo"><![CDATA[
  7 / 3   -- publishes 2
| 7 / 3.0 -- publishes 2.333...]]></programlisting>
--}
site (/) = orc.lib.math.Div

{--
* site (%)(Number, Number) :: Number
<code>a%b</code> computes the remainder of <code>a/b</code>. If <code>a</code>
and <code>b</code> have integral types, then the remainder is given by
the expression <code>a - (a/b)*b</code>. For a full description, see the
<link xmlns:xlink="http://www.w3.org/1999/xlink" xlink:href="http://java.sun.com/docs/books/jls/third_edition/html/expressions.html#15.17.3">Java Language Specification, 3rd edition</link>.
--}
site (%) = orc.lib.math.Mod

{--
* site (<)(Comparable, Comparable) :: Boolean
<code>a &lt; b</code> returns true if <code>a</code> is less than <code>b</code>, and false otherwise.
--}
site (<) = orc.lib.comp.Less

{--
* site (<=)(Comparable, Comparable) :: Boolean
<code>a &lt;= b</code> returns true if <code>a</code> is less than or equal to <code>b</code>, and false otherwise.
--}
site (<=) = orc.lib.comp.Leq

{--
* site (>)(Comparable, Comparable) :: Boolean
<code>a > b</code> returns true if <code>a</code> is greater than <code>b</code>, and false otherwise.
--}
site (>) = orc.lib.comp.Greater

{--
* site (>=)(Comparable, Comparable) :: Boolean
<code>a >= b</code> returns true if <code>a</code> is greater than or equal to <code>b</code>, and false otherwise.
--}
site (>=) = orc.lib.comp.Greq

{--
* site (=)(Top, Top) :: Boolean
<code>a = b</code> returns true if <code>a</code> is equal to <code>b</code>, and false otherwise.
The precise definition of ``equal'' depends on the values being compared, but
obeys the rule that if two values are considered equal, then one may be
substituted locally for the other without affecting the behavior of the
program.  Generally, two values are guaranteed to be considered equal only if
they are identical (in the sense of having the same object identity). In most
cases, immutable objects are also considered equal if their contents are equal.
--}
site (=) = orc.runtime.sites.core.Equal

{--
* site (/=)(Top, Top) :: Boolean
<code>a/=b</code> returns false if <code>a=b</code>, and true otherwise.
--}
site (/=) = orc.runtime.sites.core.Inequal

{--
* site (~)(Boolean) :: Boolean
Return the logical negation of the argument.
--}
site (~) = orc.runtime.sites.core.Not

{--
* site (&&)(Boolean, Boolean) :: Boolean
Return the logical conjunction of the arguments. This is not a short-circuiting
operator; both arguments must be evaluated and available before the result is
computed.
--}
site (&&) = orc.lib.bool.And

{--
* site (||)(Boolean, Boolean) :: Boolean
Return the logical disjunction of the arguments. This is not a short-circuiting
operator; both arguments must be evaluated and available before the result is
computed.
--}
site (||) = orc.lib.bool.Or

{--
* site (:)<A>(A, [A]) :: [A]
<code>a:b</code> returns the list formed by prepending the element <code>a</code>
to the list <code>b</code>.

* pattern (:)<A>([A]) :: (A, [A])
The inverse of the list constructor <code>(:)</code>. Returns the head and tail
of the list.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: (3, [4, 5])
[3,4,5] >x:xs> (x,xs)]]></programlisting>
--}
site (:) = orc.runtime.sites.core.Cons

{--
* def abs(Number) :: Number
Return the absolute value of the argument.
--}
def abs(x) = if x < 0 then -x else x

{--
* def signum(Number) :: Number
Return the sign of the argument, satisfying the equation <code>signum(a)*abs(a) = a</code>.
--}
def signum(x) =
  if x < 0 then -1
  else if x > 0 then 1
  else 0

{--
* def min<A extends Comparable>(A,A) :: A
Return the lesser of the arguments. If the arguments
are equal, returns the first argument.
--}
def min(x,y) = if y < x then y else x

{--
* def max<A extends Comparable>(A,A) :: A
Return the greater of the arguments. If the arguments
are equal, returns the second argument.
--}
def max(x,y) = if x > y then x else y
