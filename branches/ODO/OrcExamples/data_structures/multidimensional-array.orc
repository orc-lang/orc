{- multidimensional-array.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 10, 2010 3:02:30 PM
 -}

{- This class defines multi-dimensional array, called Matrix.
   A matrix is instantiated by giving a list of its bounds for each dimension.
   The number of dimensions is at least 1.
   Bound for a dimension is a pair, say (-2,0), that specifies the lower
   and upper indices for that dimension.

   There is just one method, item. It takes a list of indices and returns
   the Ref value of the corresponding element;
   Null is returned for a non-existent element.

   See how the matrix is defined, using item.
   Then a matrix element is accessed by its list of indices.
-}


type Bounds = (Integer, Integer)

class Matrix {
  type A
  val Mat :: Array[A]
  def item(List[Integer]) :: Ref[A]
}

def class Matrix0D {
  val Mat = Array[A](1)
  def item(List[Integer]) :: Ref[A]
  def item([]) = Mat(0)
}

def class MatrixND {
  val xs :: List[Bounds]

  {- size of a matrix given a list of bounds, one per dimension -}
  def size(List[Bounds]) :: Integer
  def size([]) = 1
  def size((l,h):ys) = (h-l+1)*size(ys)

  {- index(acc,xs,is) has
      acc: an integer
      xs: a list of bounds
      is: a list of indices
     It computes acc+j, where j is the linear index of the
     element at index is.
  -}
  def index(Integer, List[Bounds], List[Integer]) :: Integer
  def index(acc,[],[]) = acc
  def index(acc,(l,h):ys,i:is) = index(acc*(h-l+1)+(i-l),ys,is)

  val Mat = Array[A](size(xs))
  def item(List[Integer]) :: Ref[A]
  def item(is) = Mat(index(0,xs,is))
}
  
def Matrix[A'](List[Bounds]) :: Matrix with { type A = A' }
def Matrix[A'](0) = new Matrix0D with { type A = A' }
def Matrix[A'](xs') = new MatrixND with { type A = A' # val xs = xs' }

val B = Matrix[Integer]([]).item
val A = Matrix[Integer]([(-2,0),(-1,3),(-1,3)]).item

A([-1,2,1]) := 3 >> A([-1,2,1])?

--B([]) := 5 >> B([])?-- >> B([2])?

{-
OUTPUT:
3
-}
