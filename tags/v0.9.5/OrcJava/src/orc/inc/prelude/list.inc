{--
Operations on lists.
Many of these functions are similar to those in the Haskell prelude, but
operate on the elements of a list in parallel.
--}

{--
* def each([A]) :: A
Publish every value in a list, simultaneously.
--}
def each([]) = stop
def each(h:t) = h | each(t)

{--
* def map(lambda (A) :: B, [A]) :: [B]
Apply a function to every element of a list (in parallel),
returning a list of the results.
--}
def map(f,[]) = []
def map(f,h:t) = f(h):map(f,t)

{--
* def reverse([A]) :: [A]
Return the reverse of the given list.
--}
def reverse(l) = 
  def tailrev([],x) = x
  def tailrev(h:t,x) = tailrev(t,h:x)
  tailrev(l,[]) 

{--
* def filter(lambda (A) :: Boolean, [A]) :: [A]
Return a list containing only those elements which satisfy the predicate.
The filter is applied to all list elements in parallel.
--}
def filter(p,[]) = []
def filter(p,x:xs) =
  val fxs = filter(p, xs)
  if p(x) then x:fxs else fxs

{--
* def head([A]) :: A
Return the first element of a list.
--}
def head(x:xs) = x
def head([]) = error("head: empty list")

{--
* def tail([A]) :: [A]
Return all but the first element of a list.
--}
def tail(x:xs) = xs
def tail([]) = error("tail: empty list")

{--
* def init([A]) :: [A]
Return all but the last element of a list.
--}
def init([]) = error("init: empty list")
def init([x]) = []
def init(x:xs) = x:init(xs)

{--
* def last([A]) :: A
Return the last element of a list.
--}
def last([]) = error("last: empty list")
def last([x]) = x
def last(x:xs) = last(xs)

{--
* def empty([A]) :: Boolean
Is the list empty?
--}
def empty([]) = true
def empty(_) = false

{--
* def index(Integer, [A]) :: A
Return the nth element of a list, counting from 0.
--}
def index(h:t, 0) = h
def index(h:t, n) = index(t, n-1)

{--
* def append([A], [A]) :: [A]
Return the first list concatenated with the second.
--}
def append([],l) = l
def append(h:t,l) = h:append(t,l)

{--
* def foldl(lambda (B, A) :: B, B, [A]) :: B
Reduce a list using the given left-associative binary operation and initial value.
Given the list <code>[x1, x2, x3, ...]</code> and initial value <code>x0</code>,
returns <code>f(... f(f(f(x0, x1), x2), x3) ...)</code>

Example using <code>foldl</code> to reverse a list:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [3, 2, 1]
foldl(flip((:)), [], [1,2,3])]]></programlisting>
--}
def foldl(f,z,[]) = z
def foldl(f,z,x:xs) = foldl(f,f(z,x),xs)

{--
* def foldl1(lambda (A, A) :: A, [A]) :: A
A special case of <code>foldl</code> which uses the last element of the list as the
initial value. It is an error to call this on an empty list.
--}
def foldl1(f,x:xs) = foldl(f,x,xs)
def foldl1(_,[]) = error("foldl1: empty list")

{--
* def foldr(lambda (A, B) :: B, B, [A]) :: B
Reduce a list using the given right-associative binary operation and initial value.
Given the list <code>[..., x3, x2, x1]</code> and initial value <code>x0</code>,
returns <code>f(... f(x3, f(x2, f(x1, x0))) ...)</code>

Example summing the numbers in a list:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: 6
foldr((+), 0, [1,2,3])]]></programlisting>
--}
def foldr(f,z,xs) = foldl(flip(f),z,reverse(xs))
  
{--
* def foldr1(lambda (A, A) :: A, [A]) :: A
A special case of <code>foldr</code> which uses the last element of the list as the
initial value. It is an error to call this on an empty list.
--}
def foldr1(f,z,xs) = foldl1(flip(f),reverse(xs))

{--
* def afold(lambda (A, A) :: A, [A]) :: A
Reduce a non-empty list using the given associative binary operation.
This function reduces independent subexpressions in parallel; the
calls exhibit a balanced tree structure, so the number of sequential 
reductions performed is O(log n).  For expensive reductions, this
is much more efficient than <code>foldl</code> or <code>foldr</code>.
--}
def afold(f, []) = error("afold: empty list")
def afold(f, [x]) = x
{- Here's the interesting part -}
def afold(f, xs) =
  def afold'([]) = []
  def afold'([x]) = [x]
  def afold'(x:y:xs) = f(x,y):afold'(xs)
  afold(f, afold'(xs))


{--
* def cfold(lambda (A, A) :: A, [A]) :: A
Reduce a non-empty list using the given associative and commutative binary operation.
This function opportunistically reduces independent subexpressions in parallel, so the number of
sequential reductions performed is as small as possible.  For expensive reductions, this
is much more efficient than <code>foldl</code> or <code>foldr</code>. In cases
where the reduction does not always take the same amount of time to complete, it 
is also more efficient than <code>afold</code>.
--}
def cfold(f, []) = stop
def cfold(f, [x]) = x
def cfold(f, [x,y]) = f(x,y)
def cfold(f, L) =
  val c = Buffer()
  def work(i, x:y:rest) = c.put(f(x,y)) >> stop | work(i+1, rest)
  def work(i, [x]) = c.put(x) >> stop | work(i+1, [])
  def work(i, []) =
    if (i < 2) then 
      c.get() 
    else 
      c.get() >x> c.get() >y> ( c.put(f(x,y)) >> stop | work(i-1,[]) ) 
  work(0, L)



{--
* def zip([A], [B]) :: (A, B)
Combine two lists into a list of pairs.
The length of the shortest list determines the length of the result.
--}
def zip([],_) = []
def zip(_,[]) = []
def zip(x:xs,y:ys) = (x,y):zip(xs,ys)

{--
* def unzip([(A,B)]) :: ([A], [B])
Split a list of pairs into a pair of lists.
--}
def unzip([]) = ([],[])
def unzip((x,y):z) = (x:xs,y:ys) <(xs,ys)< unzip(z)

{--
* def length([A]) :: Integer
Return the number of elements in a list.
--}
def length([]) = 0
def length(h:t) = 1 + length(t)

{--
* def take(Integer, [A]) :: [A]
Given a number <code>n</code> and a list <code>l</code>,
return the first <code>n</code> elements of <code>l</code>.
--}
def take(0, _) = []
def take(_, []) = []
def take(n, x:xs) = x:take(n-1, xs)

{--
* def drop(Integer, [A]) :: [A]
Given a number <code>n</code> and a list <code>l</code>,
return the elements of <code>l</code> after the first <code>n</code>.
--}
def drop(0, xs) = xs
def drop(_, []) = []
def drop(n, x:xs) = drop(n-1, xs)

{--
* def member(A, [A]) :: Boolean
Return true if the given item is a member of the given list, and false
otherwise.
--}
def member(item, []) = false
def member(item, h:t) =
  if item = h then true
  else member(item, t)
  
{--
* def merge([A], [A]) :: [A], A <: Comparable
Merge two sorted lists.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [1, 2, 2, 3, 4, 5]
merge([1,2,3], [2,4,5])]]></programlisting>
--}
def merge(xs,ys) = mergeBy((<), xs, ys)

{--
* def mergeBy(lambda (A,A) :: Boolean, [A], [A]) :: [A]
Merge two lists using the given less-than relation.
--}
def mergeBy(lt, xs, []) = xs
def mergeBy(lt, [], ys) = ys
def mergeBy(lt, x:xs, y:ys) =
  if lt(y,x) then y:mergeBy(lt,x:xs,ys)
  else x:mergeBy(lt,xs,y:ys)

{--
* def sort([A]) :: [A], A <: Comparable
Sort a list.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [1, 2, 3]
sort([1,3,2])]]></programlisting>
--}
def sort(xs) = sortBy((<), xs)

{--
* def sortBy(lambda (A,A) :: Boolean, [A]) :: [A]
Sort a list using the given less-than relation.
--}
def sortBy(lt, []) = []
def sortBy(lt, [x]) = [x]
def sortBy(lt, xs) =
  length(xs)/2 >half>
  (take(half,xs), drop(half,xs)) >(front, back)>
  mergeBy(lt, sortBy(lt, front), sortBy(lt, back))
 
{--
* def mergeUnique([A], [A]) :: [A], A <: Comparable
Merge two sorted lists, discarding duplicates.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [1, 2, 3, 4, 5]
mergeUnique([1,2,3], [2,4,5])]]></programlisting>
--}
def mergeUnique(xs,ys) = mergeUniqueBy((=), (<), xs, ys)

{--
* def mergeUniqueBy(lambda (A,A) :: Boolean, lambda (A,A) :: Boolean, [A], [A]) :: [A]
Merge two lists, discarding duplicates, using the given equality and less-than relations.
--}
def mergeUniqueBy(eq, lt, xs, []) = xs
def mergeUniqueBy(eq, lt, [], ys) = ys
def mergeUniqueBy(eq, lt, x:xs, y:ys) =
  if eq(y,x) then mergeUniqueBy(eq, lt, xs, y:ys)
  else if lt(y,x) then y:mergeUniqueBy(eq,lt,x:xs,ys)
  else x:mergeUniqueBy(eq,lt,xs,y:ys)

{--
* def sortUnique([A]) :: [A], A <: Comparable
Sort a list, discarding duplicates.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [1, 2, 3]
sortUnique([1,3,2,3])]]></programlisting>
--}
def sortUnique(xs) = sortUniqueBy((=), (<), xs)

{--
* def sortUniqueBy(lambda (A,A) :: Boolean, lambda (A,A) :: Boolean, [A]) :: [A]
Sort a list, discarding duplicates, using the given equality and less-than relations.
--}
def sortUniqueBy(eq, lt, []) = []
def sortUniqueBy(eq, lt, [x]) = [x]
def sortUniqueBy(eq, lt, xs) =
  length(xs)/2 >half>
  (take(half,xs), drop(half,xs)) >(front, back)>
  mergeUniqueBy(eq, lt, sortUniqueBy(eq, lt, front), sortUniqueBy(eq, lt, back))
  
{--
* def group([(A,B)]) :: [(A,[B])]
Given a list of pairs, group together the second
elements of consecutive pairs with equal first elements.

Example:
<programlisting language="orc-demo"><![CDATA[
-- Publishes: [(1, [1, 2]), (2, [3]), (3, [4]), (1, [3])]
group([(1,1), (1,2), (2,3), (3,4), (1,3)])]]></programlisting>
--}
def group(xs) = groupBy((=), xs)

{--
* def groupBy(lambda (A,A) :: Boolean, [(A,B)]) :: [(A,[B])]
Given a list of pairs, group together the second
elements of consecutive pairs with equal first elements,
using the given equality relation.
--}
def groupBy(eq, []) = []
def groupBy(eq, (k,v):kvs) =
  def helper(k,vs, []) = [(k,vs)]
  def helper(k,vs, (k2,v):kvs) =
    if eq(k2,k) then helper(k, v:vs, kvs)
    else (k,vs):helper(k2, [v], kvs)
  helper(k,[v], kvs)
 
{--
* def range(Integer, Integer) :: [Integer]
Generate a list of integers in the given half-open range.
--}
def range(low, high) =
  if low <= high then
    low:range(low+1, high)
  else []

{--
* def any(lambda (A) :: Boolean, [A]) :: Boolean
Return true if any of the elements of the list match the predicate, and false otherwise.
The predicate is applied to all elements of the list in parellel; the result
is returned as soon as it is known and any unnecessary evaluation of the predicate
terminated.
--}
def any(p, []) = false
def any(p, x:xs) =
  por(
    (lambda () = p(x)),
    lambda () = any(p,xs) )
  
{--
* def all(lambda (A) :: Boolean, [A]) :: Boolean
Return true if all of the elements of the list match the predicate, and false otherwise.
The predicate is applied to all elements of the list in parellel; the result
is returned as soon as it is known and any unnecessary evaluation of the predicate
terminated.
--}
def all(p, []) = true
def all(p, x:xs) =
  pand(
    (lambda () = p(x)),
    lambda () = any(p,xs) )

{--
* def sum([Number]) :: Number
Return the sum of all numbers in a list.
The sum of an empty list is 0.
--}
def sum(xs) = foldl((+), 0, xs)

{--
* def product([Number]) :: Number
Return the product of all numbers in a list.
The product of an empty list is 1.
--}
def product(xs) = foldl((*), 1, xs)

{--
* def and([Boolean]) :: Boolean
Return the boolean conjunction of all boolean values in the list.
The conjunction of an empty list is <code>true</code>.
--}
def and([]) = true
def and(false:xs) = false
def and(true:xs) = and(xs)

{--
* def or([Boolean]) :: Boolean
Return the boolean disjunction of all boolean values in the list.
The disjunction of an empty list is <code>false</code>.
--}
def or([]) = false
def or(true:xs) = true
def or(false:xs) = or(xs)

{--
* def minimum([A]) :: A, A <: Comparable
Return the minimum element of a non-empty list.
--}
def minimum(xs) = foldl1(min, xs)

{--
* def maximum([A]) :: A, A <: Comparable
Return the maximum element of a non-empty list.
--}
def maximum(xs) = foldl1(max, xs)
