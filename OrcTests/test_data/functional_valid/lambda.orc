def Counter(n :: Integer) =
	val nr = Channel[Integer]()
	nr.put(n) >>
	(lambda () =
	  nr.get() >n>
		( n | nr.put(n+1) >> stop ) )

Counter(0) >c> each(range(0,10)) >> c()
{-
OUTPUT:PERMUTABLE:
0
1
2
3
4
5
6
7
8
9
-}
