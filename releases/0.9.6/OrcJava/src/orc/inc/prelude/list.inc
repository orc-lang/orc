{--
Operations on lists.
Many of these functions are similar to those in the Haskell prelude, but
operate on the elements of a list in parallel.
--}

{--
@def each[A](List[A]) :: A
Publish every value in a list, simultaneously.

@implementation
--}
def each[A](List[A]) :: A
def each([]) = stop
def each(h:t) = h | each(t)

{--
@def map[A,B](lambda (A) :: B, List[A]) :: List[B]
Apply a function to every element of a list (in parallel),
returning a list of the results.

@implementation
--}
def map[A,B](lambda (A) :: B, List[A]) :: List[B]
def map(f,[]) = []
def map(f,h:t) = f(h):map(f,t)

{--
@def reverse[A](List[A]) :: List[A]
Return the reverse of the given list.

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
Return a list containing only those elements which satisfy the predicate.
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
Return the first element of a list.

@implementation
--}
def head[A](List[A]) :: A
def head(x:xs) = x
def head([]) = error("head: empty list")

{--
@def tail[A](List[A]) :: List[A]
Return all but the first element of a list.

@implementation
--}
def tail[A](List[A]) :: List[A]
def tail(x:xs) = xs
def tail([]) = error("tail: empty list")

{--
@def init[A](List[A]) :: List[A]
Return all but the last element of a list.

@implementation
--}
def init[A](List[A]) :: List[A]
def init([]) = error("init: empty list")
def init([x]) = []
def init(x:xs) = x:init(xs)

{--
@def last[A](List[A]) :: A
Return the last element of a list.

@implementation
--}
def last[A](List[A]) :: A
def last([]) = error("last: empty list")
def last([x]) = x
def last(x:xs) = last(xs)

{--
@def empty[A](List[A]) :: Boolean
Is the list empty?

@implementation
--}
def empty[A](List[A]) :: Boolean
def empty([]) = true
def empty(_) = false

{--
@def index[A](List[A], Integer) :: A
Return the nth element of a list, counting from 0.

@implementation
--}
def index[A](List[A], Integer) :: A
def index(h:t, 0) = h
def index(h:t, n) = index(t, n-1)

{--
@def append[A](List[A], List[A]) :: List[A]
Return the first list concatenated with the second.

@implementation
--}
def append[A](List[A], List[A]) :: List[A]
def append([],l) = l
def append(h:t,l) = h:append(t,l)

{--
@def foldl[A,B](lambda (B, A) :: B, B, List[A]) :: B
Reduce a list using the given left-associative binary operation and initial value.
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
A special case of <code>foldl</code> which uses the last element of the list as the
initial value. It is an error to call this on an empty list.

@implementation
--}
def foldl1[A](lambda (A, A) :: A, List[A]) :: A
def foldl1(f,x:xs) = foldl(f,x,xs)
def foldl1(_,[]) = error("foldl1: empty list")

{--
@def foldr[A,B](lambda (A, B) :: B, B, List[A]) :: B
Reduce a list using the given right-associative binary operation and initial value.
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
A special case of <code>foldr</code> which uses the last element of the list as the
initial value. It is an error to call this on an empty list.

@implementation
--}
def foldr1[A](lambda (A, A) :: A, List[A]) :: A
def foldr1(f,xs) = foldl1(flip(f),reverse(xs))

{--
@def afold[A](lambda (A, A) :: A, List[A]) :: A
Reduce a non-empty list using the given associative binary operation.
This function reduces independent subexpressions in parallel; the
calls exhibit a balanced tree structure, so the number of sequential 
reductions performed is O(log n).  For expensive reductions, this
is much more efficient than <code>foldl</code> or <code>foldr</code>.

@implementation
--}
def afold[A](lambda (A, A) :: A, List[A]) :: A
def afold(f, []) = error("afold: empty list")
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
Reduce a non-empty list using the given associative and commutative binary operation.
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
  val c = Buffer[A]()
  def work(Number, List[A]) :: A
  def work(i, x:y:rest) =
    c.put(f(x,y)) >> stop | work(i+1, rest)
  def work(i, [x]) = c.put(x) >> stop | work(i+1, [])
  def work(i, []) =
    if (i < 2) then c.get() 
    else c.get() >x> c.get() >y>
         ( c.put(f(x,y)) >> stop | work(i-1,[]) ) 
  work(0, L)



{--
@def zip[A,B](List[A], List[B]) :: List[(A,B)]
Combine two lists into a list of pairs.
The length of the shortest list determines the length of the result.

@implementation
--}
def zip[A,B](List[A], List[B]) :: List[(A,B)]
def zip([],_) = []
def zip(_,[]) = []
def zip(x:xs,y:ys) = (x,y):zip(xs,ys)

{--
@def unzip[A,B](List[(A,B)]) :: (List[A], List[B])
Split a list of pairs into a pair of lists.

@implementation
--}
def unzip[A,B](List[(A,B)]) :: (List[A], List[B])
def unzip([]) = ([],[])
def unzip((x,y):z) = (x:xs,y:ys) <(xs,ys)< unzip(z)

{--
@def length[A](List[A]) :: Integer
Return the number of elements in a list.

@implementation
--}
def length[A](List[A]) :: Integer
def length([]) = 0
def length(h:t) = 1 + length(t)

{--
@def take[A](Integer, List[A]) :: List[A]
Given a number <code>n</code> and a list <code>l</code>,
return the first <code>n</code> elements of <code>l</code>.

@implementation
--}
def take[A](Integer, List[A]) :: List[A]
def take(0, _) = []
def take(_, []) = []
def take(n, x:xs) = x:take(n-1, xs)

{--
@def drop[A](Integer, List[A]) :: List[A]
Given a number <code>n</code> and a list <code>l</code>,
return the elements of <code>l</code> after the first <code>n</code>.

@implementation
--}
def drop[A](Integer, List[A]) :: List[A]
def drop(0, xs) = xs
def drop(_, []) = []
def drop(n, x:xs) = drop(n-1, xs)

{--
@def member[A](A, List[A]) :: Boolean
Return true if the given item is a member of the given list, and false
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
Merge two sorted lists.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [1, 2, 2, 3, 4, 5]
merge([1,2,3], [2,4,5])]]></programlisting>

@implementation
--}
def merge[A](List[A], List[A]) :: List[A]
def merge(xs,ys) = mergeBy((<), xs, ys)

{--
@def mergeBy[A](lambda (A,A) :: Boolean, List[A], List[A]) :: List[A]
Merge two lists using the given less-than relation.

@implementation
--}
def mergeBy[A](lambda (A,A) :: Boolean, List[A], List[A]) :: List[A]
def mergeBy(lt, xs, []) = xs
def mergeBy(lt, [], ys) = ys
def mergeBy(lt, x:xs, y:ys) =
  if lt(y,x) then y:mergeBy(lt,x:xs,ys)
  else x:mergeBy(lt,xs,y:ys)

{--
@def sort[A](List[A]) :: List[A]
Sort a list.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [1, 2, 3]
sort([1,3,2])]]></programlisting>

@implementation
--}
def sort[A](List[A]) :: List[A]
def sort(xs) = sortBy((<), xs)

{--
@def sortBy[A](lambda (A,A) :: Boolean, List[A]) :: List[A]
Sort a list using the given less-than relation.

@implementation
--}
def sortBy[A](lambda (A,A) :: Boolean, List[A]) :: List[A]
def sortBy(lt, []) = []
def sortBy(lt, [x]) = [x]
def sortBy(lt, xs) =
  length(xs)/2 >half>
  (take(floor(half),xs), drop(ceil(half),xs)) >(front, back)>
  mergeBy(lt, sortBy(lt, front), sortBy(lt, back))
 
{--
@def mergeUnique[A](List[A], List[A]) :: List[A]
Merge two sorted lists, discarding duplicates.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [1, 2, 3, 4, 5]
mergeUnique([1,2,3], [2,4,5])]]></programlisting>

@implementation
--}
def mergeUnique[A](List[A], List[A]) :: List[A]
def mergeUnique(xs,ys) = mergeUniqueBy((=), (<), xs, ys)

{--
@def mergeUniqueBy[A](lambda (A,A) :: Boolean, lambda (A,A) :: Boolean, List[A], List[A]) :: List[A]
Merge two lists, discarding duplicates, using the given equality and less-than relations.

@implementation
--}
def mergeUniqueBy[A](lambda (A,A) :: Boolean, lambda (A,A) :: Boolean, List[A], List[A]) :: List[A]
def mergeUniqueBy(eq, lt, xs, []) = xs
def mergeUniqueBy(eq, lt, [], ys) = ys
def mergeUniqueBy(eq, lt, x:xs, y:ys) =
  if eq(y,x) then mergeUniqueBy(eq, lt, xs, y:ys)
  else if lt(y,x) then y:mergeUniqueBy(eq,lt,x:xs,ys)
  else x:mergeUniqueBy(eq,lt,xs,y:ys)

{--
@def sortUnique[A](List[A]) :: List[A]
Sort a list, discarding duplicates.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [1, 2, 3]
sortUnique([1,3,2,3])]]></programlisting>

@implementation
--}
def sortUnique[A](List[A]) :: List[A]
def sortUnique(xs) = sortUniqueBy((=), (<), xs)

{--
@def sortUniqueBy[A](lambda (A,A) :: Boolean, lambda (A,A) :: Boolean, List[A]) :: List[A]
Sort a list, discarding duplicates, using the given equality and less-than relations.

@implementation
--}
def sortUniqueBy[A](lambda (A,A) :: Boolean, lambda (A,A) :: Boolean, List[A]) :: List[A]
def sortUniqueBy(eq, lt, []) = []
def sortUniqueBy(eq, lt, [x]) = [x]
def sortUniqueBy(eq, lt, xs) =
  length(xs)/2 >half>
  (take(floor(half),xs), drop(ceil(half),xs)) >(front, back)>
  mergeUniqueBy(eq, lt,
    sortUniqueBy(eq, lt, front),
    sortUniqueBy(eq, lt, back))
  
{--
@def group[A,B](List[(A,B)]) :: List[(A,List[B])]
Given a list of pairs, group together the second
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
Given a list of pairs, group together the second
elements of consecutive pairs with equal first elements,
using the given equality relation.

@implementation
--}
def groupBy[A,B](lambda (A,A) :: Boolean, List[(A,B)]) :: List[(A,List[B])]
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
<code>rangeBy(low, high, skip)</code> returns a sorted list of
numbers <code>n</code> which satisfy <code>n = low + skip*i</code> (for some
integer <code>i</code>), <code>n &gt;= low</code>, and <code>n &lt; high</code>.

@implementation
--}
def rangeBy(Number, Number, Number) :: List[Number]
def rangeBy(low, high, skip) =
  if low < high
  then low:rangeBy(low+skip, high, skip)
  else []
 
{--
@def range(Number, Number) :: List[Number]
Generate a list of numbers in the given half-open range.

@implementation
--}
def range(Number, Number) :: List[Number]
def range(low, high) = rangeBy(low, high, 1)

{--
@def any[A](lambda (A) :: Boolean, List[A]) :: Boolean
Return true if any of the elements of the list match the predicate, and false otherwise.
The predicate is applied to all elements of the list in parellel; the result
is returned as soon as it is known and any unnecessary evaluation of the predicate
terminated.

@implementation
--}
def any[A](lambda (A) :: Boolean, List[A]) :: Boolean
def any(p, []) = false
def any(p, x:xs) =
  let(
    val b1 = p(x)
    val b2 = any(p, xs)
    if(b1) >> true | if(b2) >> true | (b1 || b2)
  )
  
{--
@def all[A](lambda (A) :: Boolean, List[A]) :: Boolean
Return true if all of the elements of the list match the predicate, and false otherwise.
The predicate is applied to all elements of the list in parellel; the result
is returned as soon as it is known and any unnecessary evaluation of the predicate
terminated.

@implementation
--}
def all[A](lambda (A) :: Boolean, List[A]) :: Boolean
def all(p, []) = true
def all(p, x:xs) =
  let(
    val b1 = p(x)
    val b2 = all(p, xs)
    if(~b1) >> false | if(~b2) >> false | (b1 && b2)
  )

{--
@def sum(List[Number]) :: Number
Return the sum of all numbers in a list.
The sum of an empty list is 0.

@implementation
--}
def sum(List[Number]) :: Number
def sum(xs) = foldl((+) :: lambda (Number, Number) :: Number, 0, xs)

{--
@def product(List[Number]) :: Number
Return the product of all numbers in a list.
The product of an empty list is 1.

@implementation
--}
def product(List[Number]) :: Number
def product(xs) = foldl((*) :: lambda (Number, Number) :: Number, 1, xs)

{--
@def and(List[Boolean]) :: Boolean
Return the boolean conjunction of all boolean values in the list.
The conjunction of an empty list is <code>true</code>.

@implementation
--}
def and(List[Boolean]) :: Boolean
def and([]) = true
def and(false:xs) = false
def and(true:xs) = and(xs)

{--
@def or(List[Boolean]) :: Boolean
Return the boolean disjunction of all boolean values in the list.
The disjunction of an empty list is <code>false</code>.

@implementation
--}
def or(List[Boolean]) :: Boolean
def or([]) = false
def or(true:xs) = true
def or(false:xs) = or(xs)

{--
@def minimum[A](List[A]) :: A
Return the minimum element of a non-empty list.

@implementation
--}
def minimum[A](List[A]) :: A
def minimum(xs) = foldl1(lambda (x::A, y::A) = min(x,y), xs)

{--
@def maximum[A](List[A]) :: A
Return the maximum element of a non-empty list.

@implementation
--}
def maximum[A](List[A]) :: A
def maximum(xs) = foldl1(lambda (x::A, y::A) = max(x,y), xs)
