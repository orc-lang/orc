{-
The original quicksort algorithm (Hoare, C. A. R.
"Partition: Algorithm 63," "Quicksort: Algorithm 64,"
and "Find: Algorithm 65." Comm. ACM 4(7), 321-322, 1961),
was designed for efficient execution on a uniprocessor, and it
succeeded fantastically well. Encoding it as a functional program
typically ignores its efficient rearrangement of the elements of an
array. Further, no implementation highlights its concurrent
aspects. The following program attempts to overcome these two
limitations: the program is mostly functional in its structure, though
it manipulates the array elements in place. And, we encode parts of
the algorithm as concurrent activities where sequentiality is
unneeded.
-}

{- swap two refs' values -}
def swapRefs[X](Ref[X], Ref[X]) :: Signal
def swapRefs(x, y) = x? >z> x := y? >> y := z

{- parallel quicksort -}
def quicksort[X](Array[X]) :: Signal
def quicksort(a) =

--------------  Partition Procedure --------------
{- Given that l <= r, so that a(l).. a(r) is a non-empty array
    segment, which includes both a(l) and a(r).
       partition(p, l, r) permutes the elements of the array segment,
    and returns index r', l <= r' <= r, such that:
      for every i, l <= i < r', a(i)? <= p,
      for every i, r' < i <= r, a(i)? >  p.
    Note: Either of the partitioned segments may be empty. -}

def partition(Integer, Integer, Integer) :: Integer
def partition(p, l, r) =
   ------ Helper functions ------
   {-   lr(i) returns the smallest j, i >= j <= r, such that a(i)? > p,
      rl(i) returns the largest  j, j < i, such that a(i)? <= p. -}

   def lr(Integer) :: Integer
   def rl(Integer) :: Integer
   def lr(i) = if i < r && a(i)? <= p then lr(i+1) else i
   def rl(i) = if a(i)? > p then rl(i-1) else i
   ------ End of Helper functions ------

   {- Goal Expression for partition(p, l, r)
      Below, lr and rl run in parallel. -}
   (lr(l), rl(r)) >(l', r')>

   ( if (l' + 1 < r') >> swapRefs(a(l'), a(r')) >> partition(p, l'+1, r'-1)
   | if (l' + 1 = r') >> swapRefs(a(l'), a(r')) >> l'
   | if (l' + 1 > r') >> r'
   )
--------------  End of Partition Procedure --------------

--------------  Sort an array segment -------------
{- sort(l, r) sorts the segment a(l).. a(r) -}
def sort(Integer, Integer) :: Signal
def sort(l, r) =
   if l >= r then signal
   else
     {- partition and then sort recursively -}
     partition(a(l)?, l+1, r) >m>
     swapRefs(a(m), a(l)) >>
     {- Below, the two partitions are sorted in parallel -}
     (sort(l, m-1), sort(m+1, r)) >>
     signal
-------------- End of Sort an array segment -------------

{- Goal Expression for quicksort(a) -}
sort(0, a.length()-1)

--------------  Test Routines -------------
{- sorted(xs) returns true if non-empty list xs is sorted.
   We check if the result of quicksort is sorted.
   We do not check if the final array is a permutation of the initial
   array, because the array elements are merely swapped in this program. -}
def sorted[X](List[X]) :: Boolean
def sorted([x])    = true
def sorted(x:y:xs) = (x <= y) && sorted(y:xs)
--------------  End of Test Routines -------------

{- Test with a random array -}
signals(10) >>
fillArray(Array[Integer](20), lambda (_ :: Integer) = random(20)-10) >a>
quicksort(a) >>
map(lambda (x :: Integer) = x {- let -}, a) >l> -- convert array a to list l
sorted(l)

{-
OUTPUT:
true
true
true
true
true
true
true
true
true
true
-}