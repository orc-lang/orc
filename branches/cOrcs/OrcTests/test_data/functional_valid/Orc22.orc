{- Orc22.orc
 - 
 - Test for Orc datatypes
 - 
 - Created by Brian on Jun 3, 2010 1:44:27 PM
 -}

type Tree[T] = Node(Tree[T], T, Tree[T]) | Empty()
val l = Node(Empty(), 0, Empty())
val r = Node(Empty(), 2, Empty())
val t = Node(l,1,r)

t >Node(l,j,r)>
l >Node(_,i,_)>
r >Node(_,k,_)>
( i | j | k )

{-
OUTPUT:PERMUTABLE
0
1
2
-}
