def foo() = "foo"

MakeSite(foo)

{-
type inference on chained datatype instantiation:
-}

{-
type Either[A,B] = Left(A) | Right(B)

  Left(Left(true))
| Left(Right(true))
| Right(Left(true))
| Right(Right(6))
-}

{-
type Tree[A, B] = Leaf(B) | Node(Tree[A], Buffer[A], Tree[A])

val x = Leaf[Integer, Boolean](true) 
val y = Leaf[Integer, Boolean](stop)
Node(x, Buffer[Integer](), y)
  -}
{-
type Tree[A] = Leaf | Tree(Tree[Int => A], Tree[Int => A]) 
-}

-- type Z[A] = (lambda () :: A)

{-
type N = Integer

def tup[X,Y](X,Y) :: (X,Y)
def tup(x,y) = (x,y)

def duotup[X,Y](X,Y) :: ((X,Y),(Y,X))
def duotup(x,y) = tup[(X,Y),(Y,X)](tup[X,Y](x,y), tup[Y,X](y,x))

duotup(1,1)
-}

