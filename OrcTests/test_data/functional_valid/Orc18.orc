{- Orc18.orc
 - 
 - Simple test of Orc cells
 - 
 - Created by Brian on Jun 3, 2010 1:15:53 PM
 -}

val y = Cell[Integer]()
y.write(5) >> y.read() >q> Println(q) >> y.write(6) >> y.read() >t> Println(t)

{-
OUTPUT:
5
-}