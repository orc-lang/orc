-- amshali
-- Monday, July 05 2010

-- sort output

def sort[A](lambda () :: A, lambda (A,A) :: Integer) :: List[A]
def sort(input, comparator) =
  val b = Channel[A]()
  val l = Ref[List[A]]([])
  def sort_aux(A, List[A]) :: List[A]
  def sort_aux(x, []) = [x]
  def sort_aux(x, y:[]) = if (comparator(x, y) <: 0) then x:[y] else y:[x]
  def sort_aux(x, y:yl) = if (comparator(x, y) <: 0) then x:y:yl else y:sort_aux(x, yl)
  def sort_Channel() :: Signal
  def sort_Channel() = (b.get() >x> (l := sort_aux(x, l?))  >> sort_Channel() >> stop); signal
 
  # (input() >x> b.put(x)  >> stop; b.close()>>stop) | sort_Channel() >> l?


sort(lambda()=( (1,(2,3)) | (4,true) | (5,[6,7]) | (8,signal) ) >(x,_)> x, 
  lambda(x :: Integer, y :: Integer) = x - y)


{-
OUTPUT:
[1, 4, 5, 8]
-}
