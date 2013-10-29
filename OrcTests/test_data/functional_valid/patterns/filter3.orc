-- Filter using size match, push

(

( (1,2) | (1,2,3) | (5,6) | (5,6,7) ) >(x,y)> (y,x)

) :!: Signal  {- As currently written, this program cannot pass the typechecker -}

{-
OUTPUT:PERMUTABLE
(2, 1)
(6, 5)
-}
