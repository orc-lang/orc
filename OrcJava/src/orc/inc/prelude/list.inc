{--
def each<A>([A]) :: A
Publish every value in a sequence, simultaneously.
--}
def each([]) = stop
def each(h:t) = h | each(t)

{--
def map<A,B>(lambda (A) :: B, [A]) :: [B]
Apply a function to every element of a sequence (in parallel),
producing a list of the results.
--}
def map(f,[]) = []
def map(f,h:t) = f(h):map(f,t)

{--
def reverse<A>([A]) :: [A]
Reverse a list.
--}
def reverse(l) = 
  def tailrev([],x) = x
  def tailrev(h:t,x) = tailrev(t,h:x)
  tailrev(l,[]) 

{--
def filter<A>(lambda (A) :: Boolean, [A]) :: [A]
Return a list containing only those elements which satisfy the predicate.
--}
def filter(p,[]) = []
def filter(p,x:xs) =
  val fxs = filter(p, xs)
  if p(x) then x:fxs else fxs

{--
def head<A>([A]) :: A
Return the first element of a sequence.
--}
def head(x:xs) = x
def head([]) = error("head: empty list")

{--
def tail<A>([A]) :: [A]
Return all but the first element of a sequence.
--}
def tail(x:xs) = xs
def tail([]) = error("tail: empty list")

{--
def init<A>([A]) :: [A]
Return all but the last element of a sequence.
--}
def init([]) = error("init: empty list")
def init([x]) = []
def init(x:xs) = x:init(xs)

{--
def last<A>([A]) :: A
Return the last element of a sequence.
--}
def last([]) = error("last: empty list")
def last([x]) = x
def last(x:xs) = last(xs)

{--
def empty<A>([A]) :: Boolean
Is the sequence empty?
--}
def empty([]) = true
def empty(_) = false

{--
def index<A>(Integer, [A]) :: A
Return the nth element of a list, counting from 0.
--}
def index(h:t, 0) = h
def index(h:t, n) = index(t, n-1)

{--
def append<A>([A], [A]) :: [A]
Concatenate two lists.
--}
def append([],l) = l
def append(h:t,l) = h:append(t,l)

{--
def foldl<A,B>(lambda (B, A) :: B, B, [A]) :: B
Reduce a list using the given left-associative binary operation and initial value.
Given the list <code>[x1, x2, x3, ...]</code> and initial value <code>x0</code>,
returns <code>f(... f(f(f(x0, x1), x2), x3) ...)</code>
--}
def foldl(f,z,[]) = z
def foldl(f,z,x:xs) = foldl(f,f(z,x),xs)

{--
def foldl1<A>(lambda (A, A) :: A, [A]) :: A
A special case of <code>foldl</code> which uses the last element of the list as the
initial value.
--}
def foldl1(f,x:xs) = foldl(f,x,xs)
def foldl1(_,[]) = error("foldl1: empty list")

{--
def foldr<A,B>(lambda (A, B) :: B, B, [A]) :: B
Reduce a list using the given right-associative binary operation and initial value.
Given the list <code>[..., x3, x2, x1]</code> and initial value <code>x0</code>,
returns <code>f(... f(x3, f(x2, f(x1, x0))) ...)</code>
--}
def foldr(f,z,xs) = foldl(flip(f),z,reverse(xs))
  
{--
def foldr1<A>(lambda (A, A) :: A, [A]) :: A
A special case of <code>foldr</code> which uses the last element of the list as the
initial value.
--}
def foldr1(f,z,xs) = foldl1(flip(f),reverse(xs))

{--
def zip<A,B>([A], [B]) :: (A, B)
Combine two lists into a list of pairs.
The length of the shortest list determines the length of the result.
--}
def zip([],_) = []
def zip(_,[]) = []
def zip(x:xs,y:ys) = (x,y):zip(xs,ys)

{--
def unzip<A,B>([(A,B)]) :: ([A], [B])
Split a list of pairs into a pair of lists.
--}
def unzip([]) = ([],[])
def unzip((x,y):z) = (x:xs,y:ys) <(xs,ys)< unzip(z)

{--
def length<A>([A]) :: Integer
Return the number of elements in a list.
--}
def length([]) = 0
def length(h:t) = 1 + length(t)

{--
def take<A>(Integer, [A]) :: [A]
Return the first <code>n</code> elements of a list.
--}
def take(0, _) = []
def take(_, []) = []
def take(n, x:xs) = x:take(n-1, xs)

{--
def drop<A>(Integer, [A]) :: [A]
Return the elements of a list after the first <code>n</code>.
--}
def drop(0, xs) = xs
def drop(_, []) = []
def drop(n, x:xs) = drop(n-1, xs)

{--
def member<A>(A, [A]) :: Boolean
Is the given item a member of the list?
--}
def member(item, []) = false
def member(item, h:t) =
  if item = h then true
  else member(item, t)
  
{--
def merge<A extends Comparable>([A], [A]) :: [A]
Merge two lists.
--}
def merge(xs,ys) = mergeBy((<), xs, ys)

{--
def mergeBy<A>(lambda (A,A) :: Boolean, [A], [A]) :: [A]
Merge two lists using the given less-than relation.
--}
def mergeBy(lt, xs, []) = xs
def mergeBy(lt, [], ys) = ys
def mergeBy(lt, x:xs, y:ys) =
  if lt(y,x) then y:mergeBy(lt,x:xs,ys)
  else x:mergeBy(lt,xs,y:ys)

{--
def sort<A extends Comparable>([A]) :: [A]
Sort a list.
--}
def sort(xs) = sortBy((<), xs)

{--
def sortBy<A>(lambda (A,A) :: Boolean, [A]) :: [A]
Sort a list using the given less-than relation.
--}
def sortBy(lt, []) = []
def sortBy(lt, [x]) = [x]
def sortBy(lt, xs) =
  length(xs)/2 >half>
  (take(half,xs), drop(half,xs)) >(front, back)>
  mergeBy(lt, sortBy(lt, front), sortBy(lt, back))
  
{--
def group<A,B>([(A,B)]) :: [(A,[B])]
Given a list of pairs, group together the second
elements of all pairs with equal first elements.
--}
def group(xs) = groupBy((=), xs)

{--
def groupBy<A,B>(lambda (A,A) :: Boolean, [(A,B)]) :: [(A,[B])]
Given a list of pairs, group together the second
elements of all pairs with equal first elements,
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
def range(Integer, Integer) :: [Integer]
Generate a list of integers in the given half-open range.
--}
def range(low, high) =
  if low <= high then
    low:range(low+1, high)
  else []

{--
def any<A>(lambda (A) :: Boolean, [A]) :: Boolean
Do any the elements of the list match the predicate?
Uses parallel-or to return a result as soon as possible and terminate
unnecessary computation.
--}
def any(p, []) = false
def any(p, x:xs) =
  por(
    delay(p,x),
    lambda () = any(p,xs) )
  
{--
def all<A>(lambda (A) :: Boolean, [A]) :: Boolean
Do all the elements of the list match the predicate?
Uses parallel-and to return a result as soon as possible and terminate
unnecessary computation.
--}
def all(p, []) = true
def all(p, x:xs) =
  pand(
    delay(p,x),
    lambda () = any(p,xs) )

{--
def sum([Number]) :: Number
Compute the sum of all numbers in a list.
--}
def sum(xs) = foldl((+), 0, xs)
