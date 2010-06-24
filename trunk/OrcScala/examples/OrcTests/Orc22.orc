{- Orc22.orc
 - 
 - Test for Orc datatypes
 - 
 - Created by Brian on Jun 3, 2010 1:44:27 PM
 -}

type Tree = Node(_,_,_) | Empty()
val l = Node(Empty(), 0, Empty())
val r = Node(Empty(), 2, Empty())
val t = Node(l,1,r)

t >Node(l,j,r)>
l >Node(_,i,_)>
r >Node(_,k,_)>
( i | j | k )

{-
OUTPUT:
0
1
2
-}
{-
OUTPUT:
0
2
1
-}
{-
OUTPUT:
1
0
2
-}
{-
OUTPUT:
1
2
0
-}
{-
OUTPUT:
2
0
1
-}
{-
OUTPUT:
2
1
0
-}
