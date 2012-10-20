--
-- list.inc -- Orc standard prelude include, lists section
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
Operations on lists.

Many of these functions are similar to those in the Haskell prelude, but
operate on the elements of a <link linkend="ref.data.list">list</link> in parallel.
--}

{--
@def each[A](List[A]) :: A
<link linkend="ref.concepts.publish">Publish</link> every value in a <link linkend="ref.data.list">list</link>, simultaneously.

@implementation
--}
def each[A](List[A]) :: A
def each([]) = stop
def each(h:t) = h | each(t)

{--
@def map[A,B](lambda (A) :: B, List[A]) :: List[B]
Apply a function to every element of a <link linkend="ref.data.list">list</link> (in parallel),
<link linkend="ref.concepts.publish">publishing</link> a list of the results.

@implementation
--}
def map[A,B](lambda (A) :: B, List[A]) :: List[B]
def map(f,[]) = []
def map(f,h:t) = f(h):map(f,t)

{--
@def reverse[A](List[A]) :: List[A]
<link linkend="ref.concepts.publish">Publish</link> the reverse of the given <link linkend="ref.data.list">list</link>.

@implementation
--}
def reverse[A](List[A]) :: List[A]
def reverse(l) =
  def tailrev(List[A], List[A]) :: List[A]
  def tailrev([],x) = x
  def tailrev(h:t,x) = tailrev(t,h:x)
  tailrev(l,[])

{--
@def filter[A](lambda (A) :: Boolean, List[A]) :: List[A]
<link linkend="ref.concepts.publish">Publish</link> a <link linkend="ref.data.list">list</link> containing only those elements which satisfy the predicate.
The filter is applied to all list elements in parallel.

@implementation
--}
def filter[A](lambda (A) :: Boolean, List[A]) :: List[A]
def filter(p,[]) = []
def filter(p,x:xs) =
  val fxs = filter(p, xs)
  if p(x) then x:fxs else fxs

{--
@def head[A](List[A]) :: A
<link linkend="ref.concepts.publish">Publish</link> the first element of a <link linkend="ref.data.list">list</link>.

@implementation
--}
def head[A](List[A]) :: A
def head(x:xs) = x

{--
@def tail[A](List[A]) :: List[A]
<link linkend="ref.concepts.publish">Publish</link> all but the first element of a <link linkend="ref.data.list">list</link>.

@implementation
--}
def tail[A](List[A]) :: List[A]
def tail(x:xs) = xs

{--
@def init[A](List[A]) :: List[A]
<link linkend="ref.concepts.publish">Publish</link> all but the last element of a <link linkend="ref.data.list">list</link>.

@implementation
--}
def init[A](List[A]) :: List[A]
def init([x]) = []
def init(x:xs) = x:init(xs)

{--
@def last[A](List[A]) :: A
<link linkend="ref.concepts.publish">Publish</link> the last element of a <link linkend="ref.data.list">list</link>.

@implementation
--}
def last[A](List[A]) :: A
def last([x]) = x
def last(x:xs) = last(xs)

{--
@def empty[A](List[A]) :: Boolean
Is the <link linkend="ref.data.list">list</link> empty?

@implementation
--}
def empty[A](List[A]) :: Boolean
def empty([]) = true
def empty(_) = false

{--
@def index[A](List[A], Integer) :: A
<link linkend="ref.concepts.publish">Publish</link> the nth element of a <link linkend="ref.data.list">list</link>, counting from 0.

@implementation
--}
def index[A](List[A], Integer) :: A
def index(h:t, 0) = h
def index(h:t, n) = index(t, n-1)

{--
@def append[A](List[A], List[A]) :: List[A]
<link linkend="ref.concepts.publish">Publish</link> the first <link linkend="ref.data.list">list</link> concatenated with the second.

@implementation
--}
def append[A](List[A], List[A]) :: List[A]
def append([],l) = l
def append(h:t,l) = h:append(t,l)

{--
@def foldl[A,B](lambda (B, A) :: B, B, List[A]) :: B
Reduce a <link linkend="ref.data.list">list</link> using the given left-associative binary operation and initial value.
Given the list <code>[x1, x2, x3, ...]</code> and initial value <code>x0</code>,
returns <code>f(... f(f(f(x0, x1), x2), x3) ...)</code>

Example using <code>foldl</code> to reverse a list:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [3, 2, 1]
foldl(flip((:)), [], [1,2,3])]]></programlisting>

@implementation
--}
def foldl[A,B](lambda (B, A) :: B, B, List[A]) :: B
def foldl(f,z,[]) = z
def foldl(f,z,x:xs) = foldl(f,f(z,x),xs)

{--
@def foldl1[A](lambda (A, A) :: A, List[A]) :: A
A special case of <code>foldl</code> which uses the first element of the <link linkend="ref.data.list">list</link> as the
initial value. If called on an empty list, <link linkend="ref.concepts.states.halt">halt</link>.

@implementation
--}
def foldl1[A](lambda (A, A) :: A, List[A]) :: A
def foldl1(f,x:xs) = foldl(f,x,xs)

{--
@def foldr[A,B](lambda (A, B) :: B, B, List[A]) :: B
Reduce a <link linkend="ref.data.list">list</link> using the given right-associative binary operation and initial value.
Given the list <code>[..., x3, x2, x1]</code> and initial value <code>x0</code>,
returns <code>f(... f(x3, f(x2, f(x1, x0))) ...)</code>

Example summing the numbers in a list:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 6
foldr((+), 0, [1,2,3])]]></programlisting>

@implementation
--}
def foldr[A,B](lambda (A, B) :: B, B, List[A]) :: B
def foldr(f,z,xs) = foldl(flip(f),z,reverse(xs))

{--
@def foldr1[A](lambda (A, A) :: A, List[A]) :: A
A special case of <code>foldr</code> which uses the last element of the <link linkend="ref.data.list">list</link> as the
initial value. If called on an empty list, <link linkend="ref.concepts.states.halt">halt</link>.

@implementation
--}
def foldr1[A](lambda (A, A) :: A, List[A]) :: A
def foldr1(f,xs) = foldl1(flip(f),reverse(xs))

{--
@def afold[A](lambda (A, A) :: A, List[A]) :: A
Reduce a non-empty <link linkend="ref.data.list">list</link> using the given associative binary operation.
This function reduces independent subexpressions in parallel; the
calls exhibit a balanced tree structure, so the number of sequential
reductions performed is O(log n).  For expensive reductions, this
is much more efficient than <code>foldl</code> or <code>foldr</code>.

@implementation
--}
def afold[A](lambda (A, A) :: A, List[A]) :: A
def afold(f, [x]) = x
{- Here's the interesting part -}
def afold(f, xs) =
  def afold'(List[A]) :: List[A]
  def afold'([]) = []
  def afold'([x]) = [x]
  def afold'(x:y:xs) = f(x,y):afold'(xs)
  afold(f, afold'(xs))


{--
@def cfold[A](lambda (A, A) :: A, List[A]) :: A
Reduce a non-empty <link linkend="ref.data.list">list</link> using the given associative and commutative binary operation.
This function opportunistically reduces independent subexpressions in parallel, so the number of
sequential reductions performed is as small as possible.  For expensive reductions, this
is much more efficient than <code>foldl</code> or <code>foldr</code>. In cases
where the reduction does not always take the same amount of time to complete, it
is also more efficient than <code>afold</code>.

@implementation
--}
def cfold[A](lambda (A, A) :: A, List[A]) :: A
def cfold(f, []) = stop
def cfold(f, [x]) = x
def cfold(f, [x,y]) = f(x,y)
def cfold(f, L) =
  val c = Channel[A]()
  def work(Number, List[A]) :: A
  def work(i, x:y:rest) =
    c.put(f(x,y)) >> stop | work(i+1, rest)
  def work(i, [x]) = c.put(x) >> stop | work(i+1, [])
  def work(i, []) =
    if (i <: 2) then c.get()
    else c.get() >x> c.get() >y>
         ( c.put(f(x,y)) >> stop | work(i-1,[]) )
  work(0, L)



{--
@def zip[A,B](List[A], List[B]) :: List[(A,B)]
Combine a <link linkend="ref.data.tuple">pair</link> of <link linkend="ref.data.list">lists</link> into a list of pairs.
The length of the shortest list determines the length of the result.

@implementation
--}
def zip[A,B](List[A], List[B]) :: List[(A,B)]
def zip([],_) = []
def zip(_,[]) = []
def zip(x:xs,y:ys) = (x,y):zip(xs,ys)

{--
@def unzip[A,B](List[(A,B)]) :: (List[A], List[B])
Split a <link linkend="ref.data.list">list</link> of <link linkend="ref.data.tuple">pairs</link> into a pair of lists.

@implementation
--}
def unzip[A,B](List[(A,B)]) :: (List[A], List[B])
def unzip([]) = ([],[])
def unzip((x,y):z) = (x:xs,y:ys) <(xs,ys)< unzip(z)


{--
@def concat[A](List[List[A]]) :: List[A]
Concatenate a <link linkend="ref.data.list">list</link> of lists into a single list.

@implementation
--}
def concat[A](List[List[A]]) :: List[A]
def concat([]) = []
def concat(h:t) = append(h,concat(t))


{--
@def length[A](List[A]) :: Integer
<link linkend="ref.concepts.publish">Publish</link> the number of elements in a <link linkend="ref.data.list">list</link>.

@implementation
--}
def length[A](List[A]) :: Integer
def length([]) = 0
def length(h:t) = 1 + length(t)

{--
@def take[A](Integer, List[A]) :: List[A]
Given a number <code>n</code> and a <link linkend="ref.data.list">list</link> <code>l</code>,
<link linkend="ref.concepts.publish">publish</link> a list of the first <code>n</code> elements of <code>l</code>.
If <code>n</code> exceeds the length of <code>l</code>,
or <code>n &lt; 0</code>, take <link linkend="ref.concepts.states.halt">halts</link> with an error.

@implementation
--}
def take[A](Integer, List[A]) :: List[A]
def take(0, _) = []
def take(n, x:xs) =
  if n :> 0 then x:take(n-1, xs)
  else Error("Cannot take(" + n + ", _)")

{--
@def drop[A](Integer, List[A]) :: List[A]
Given a number <code>n</code> and a <link linkend="ref.data.list">list</link> <code>l</code>,
<link linkend="ref.concepts.publish">publish</link> a list of the elements of <code>l</code> after the first <code>n</code>.
If <code>n</code> exceeds the length of <code>l</code>,
or <code>n &lt; 0</code>, drop <link linkend="ref.concepts.states.halt">halts</link> with an error.

@implementation
--}
def drop[A](Integer, List[A]) :: List[A]
def drop(0, xs) = xs
def drop(n, x:xs) =
  if n :> 0 then drop(n-1, xs)
  else Error("Cannot drop(" + n + ", _)")

{--
@def member[A](A, List[A]) :: Boolean
<link linkend="ref.concepts.publish">Publish</link> true if the given item is a member of the given <link linkend="ref.data.list">list</link>, and false
otherwise.

@implementation
--}
def member[A](A, List[A]) :: Boolean
def member(item, []) = false
def member(item, h:t) =
  if item = h then true
  else member(item, t)

{--
@def merge[A](List[A], List[A]) :: List[A]
Merge two sorted <link linkend="ref.data.list">lists</link>.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [1, 2, 2, 3, 4, 5]
merge([1,2,3], [2,4,5])]]></programlisting>

@implementation
--}
def merge[A](List[A], List[A]) :: List[A]
def merge(xs,ys) = mergeBy((<:), xs, ys)

{--
@def mergeBy[A](lambda (A,A) :: Boolean, List[A], List[A]) :: List[A]
Merge two <link linkend="ref.data.list">lists</link> using the given less-than relation.

@implementation
--}
def mergeBy[A](lambda (A,A) :: Boolean,
               List[A], List[A]) :: List[A]
def mergeBy(lt, xs, []) = xs
def mergeBy(lt, [], ys) = ys
def mergeBy(lt, x:xs, y:ys) =
  if lt(y,x) then y:mergeBy(lt,x:xs,ys)
  else x:mergeBy(lt,xs,y:ys)

{--
@def sort[A](List[A]) :: List[A]
Sort a <link linkend="ref.data.list">list</link>.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [1, 2, 3]
sort([1,3,2])]]></programlisting>

@implementation
--}
def sort[A](List[A]) :: List[A]
def sort(xs) = sortBy((<:), xs)

{--
@def sortBy[A](lambda (A,A) :: Boolean, List[A]) :: List[A]
Sort a <link linkend="ref.data.list">list</link> using the given less-than relation.

@implementation
--}
def sortBy[A](lambda (A,A) :: Boolean, List[A]) :: List[A]
def sortBy(lt, []) = []
def sortBy(lt, [x]) = [x]
def sortBy(lt, xs) =
  val half = Floor(length(xs)/2)
  val front = take(half, xs)
  val back = drop(half, xs)
  mergeBy(lt, sortBy(lt, front), sortBy(lt, back))

{--
@def mergeUnique[A](List[A], List[A]) :: List[A]
Merge two sorted <link linkend="ref.data.list">lists</link>, discarding duplicates.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [1, 2, 3, 4, 5]
mergeUnique([1,2,3], [2,4,5])]]></programlisting>

@implementation
--}
def mergeUnique[A](List[A], List[A]) :: List[A]
def mergeUnique(xs,ys) = mergeUniqueBy((=), (<:), xs, ys)

{--
@def mergeUniqueBy[A](lambda (A,A) :: Boolean, lambda (A,A) :: Boolean, List[A], List[A]) :: List[A]
Merge two <link linkend="ref.data.list">lists</link>, discarding duplicates, using the given equality and less-than relations.

@implementation
--}
def mergeUniqueBy[A](lambda (A,A) :: Boolean,
                     lambda (A,A) :: Boolean,
                      List[A], List[A])
  :: List[A]
def mergeUniqueBy(eq, lt, xs, []) = xs
def mergeUniqueBy(eq, lt, [], ys) = ys
def mergeUniqueBy(eq, lt, x:xs, y:ys) =
  if eq(y,x) then mergeUniqueBy(eq, lt, xs, y:ys)
  else if lt(y,x) then y:mergeUniqueBy(eq,lt,x:xs,ys)
  else x:mergeUniqueBy(eq,lt,xs,y:ys)

{--
@def sortUnique[A](List[A]) :: List[A]
Sort a <link linkend="ref.data.list">list</link>, discarding duplicates.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [1, 2, 3]
sortUnique([1,3,2,3])]]></programlisting>

@implementation
--}
def sortUnique[A](List[A]) :: List[A]
def sortUnique(xs) = sortUniqueBy((=), (<:), xs)

{--
@def sortUniqueBy[A](lambda (A,A) :: Boolean, lambda (A,A) :: Boolean, List[A]) :: List[A]
Sort a <link linkend="ref.data.list">list</link>, discarding duplicates, using the given equality and less-than relations.

@implementation
--}
def sortUniqueBy[A](lambda (A,A) :: Boolean,
                    lambda (A,A) :: Boolean,
                    List[A])
  :: List[A]
def sortUniqueBy(eq, lt, []) = []
def sortUniqueBy(eq, lt, [x]) = [x]
def sortUniqueBy(eq, lt, xs) =
  val half = Floor(length(xs)/2)
  val front = take(half, xs)
  val back = drop(half, xs)
  mergeUniqueBy(eq, lt,
    sortUniqueBy(eq, lt, front),
    sortUniqueBy(eq, lt, back))

{--
@def group[A,B](List[(A,B)]) :: List[(A,List[B])]
Given a <link linkend="ref.data.list">list</link> of <link linkend="ref.data.tuple">pairs</link>, group together the second
elements of consecutive pairs with equal first elements.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [(1, [1, 2]), (2, [3]), (3, [4]), (1, [3])]
group([(1,1), (1,2), (2,3), (3,4), (1,3)])]]></programlisting>

@implementation
--}
def group[A,B](List[(A,B)]) :: List[(A,List[B])]
def group(xs) = groupBy((=), xs)

{--
@def groupBy[A,B](lambda (A,A) :: Boolean, List[(A,B)]) :: List[(A,List[B])]
Given a <link linkend="ref.data.list">list</link> of <link linkend="ref.data.tuple">pairs</link>, group together the second
elements of consecutive pairs with equal first elements,
using the given equality relation.

@implementation
--}
def groupBy[A,B](lambda (A,A) :: Boolean,
                 List[(A,B)])
  :: List[(A,List[B])]
def groupBy(eq, []) = []
def groupBy(eq, (k,v):kvs) =
  def helper(A, List[B], List[(A,B)]) :: List[(A,List[B])]
  def helper(k,vs, []) = [(k,vs)]
  def helper(k,vs, (k2,v):kvs) =
    if eq(k2,k) then helper(k, v:vs, kvs)
    else (k,vs):helper(k2, [v], kvs)
  helper(k,[v], kvs)

{--
@def rangeBy(Number, Number, Number) :: List[Number]
<code>rangeBy(low, high, skip)</code> returns a sorted <link linkend="ref.data.list">list</link> of
numbers <code>n</code> which satisfy <code>n = low + skip*i</code> (for some
integer <code>i</code>), <code>n &gt;= low</code>, and <code>n &lt; high</code>.

@implementation
--}
def rangeBy(Number, Number, Number) :: List[Number]
def rangeBy(low, high, skip) =
  if low <: high
  then low:rangeBy(low+skip, high, skip)
  else []

{--
@def range(Number, Number) :: List[Number]
Generate a <link linkend="ref.data.list">list</link> of numbers in the given half-open range.

@implementation
--}
def range(Number, Number) :: List[Number]
def range(low, high) = rangeBy(low, high, 1)

{--
@def any[A](lambda (A) :: Boolean, List[A]) :: Boolean
<link linkend="ref.concepts.publish">Publish</link> true if any of the elements of the <link linkend="ref.data.list">list</link> match the predicate, and false otherwise.
The predicate is applied to all elements of the list in parallel; the result
is returned as soon as it is known and any unnecessary execution of the predicate
<link linkend="ref.concepts.states.kill">killed</link>.

@implementation
--}
def any[A](lambda (A) :: Boolean, List[A]) :: Boolean
def any(p, []) = false
def any(p, x:xs) =
  Let(
    val b1 = p(x)
    val b2 = any(p, xs)
    Ift(b1) >> true | Ift(b2) >> true | (b1 || b2)
  )

{--
@def all[A](lambda (A) :: Boolean, List[A]) :: Boolean
<link linkend="ref.concepts.publish">Publish</link> true if all of the elements of the <link linkend="ref.data.list">list</link> match the predicate, and false otherwise.
The predicate is applied to all elements of the list in parallel; the result
is returned as soon as it is known and any unnecessary execution of the predicate
<link linkend="ref.concepts.states.kill">killed</link>.

@implementation
--}
def all[A](lambda (A) :: Boolean, List[A]) :: Boolean
def all(p, []) = true
def all(p, x:xs) =
  Let(
    val b1 = p(x)
    val b2 = all(p, xs)
    Iff(b1) >> false | Iff(b2) >> false | (b1 && b2)
  )

{--
@def sum(List[Number]) :: Number
<link linkend="ref.concepts.publish">Publish</link> the sum of all numbers in a <link linkend="ref.data.list">list</link>.
The sum of an empty list is 0.

@implementation
--}
def sum(List[Number]) :: Number
def sum(xs) = foldl(
  (+) :: lambda (Number, Number) :: Number,
  0, xs)

{--
@def product(List[Number]) :: Number
<link linkend="ref.concepts.publish">Publish</link> the product of all numbers in a <link linkend="ref.data.list">list</link>.
The product of an empty list is 1.

@implementation
--}
def product(List[Number]) :: Number
def product(xs) = foldl(
  (*) :: lambda (Number, Number) :: Number,
  1, xs)

{--
@def and(List[Boolean]) :: Boolean
<link linkend="ref.concepts.publish">Publish</link> the boolean conjunction of all boolean values in the <link linkend="ref.data.list">list</link>.
The conjunction of an empty list is <code>true</code>.

@implementation
--}
def and(List[Boolean]) :: Boolean
def and([]) = true
def and(false:xs) = false
def and(true:xs) = and(xs)

{--
@def or(List[Boolean]) :: Boolean
<link linkend="ref.concepts.publish">Publish</link> the boolean disjunction of all boolean values in the <link linkend="ref.data.list">list</link>.
The disjunction of an empty list is <code>false</code>.

@implementation
--}
def or(List[Boolean]) :: Boolean
def or([]) = false
def or(true:xs) = true
def or(false:xs) = or(xs)

{--
@def minimum[A](List[A]) :: A
<link linkend="ref.concepts.publish">Publish</link> the minimum element of a non-empty <link linkend="ref.data.list">list</link>.

@implementation
--}
def minimum[A](List[A]) :: A
def minimum(xs) =
  def minA(x :: A, y :: A) = min(x,y)
  foldl1(minA, xs)

{--
@def maximum[A](List[A]) :: A
<link linkend="ref.concepts.publish">Publish</link> the maximum element of a non-empty <link linkend="ref.data.list">list</link>.

@implementation
--}
def maximum[A](List[A]) :: A
def maximum(xs) =
  def maxA(x :: A, y :: A) = max(x,y)
  foldl1(maxA, xs)
