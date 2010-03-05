def Counter(n :: Integer) =
	val nr = Buffer[Integer]()
	nr.put(n) >>
	(lambda () =
		nr.get() >n>
		( n | nr.put(n+1) >> stop ) )

Counter(0) >c> each(range(0,10)) >> c()
{-
OUTPUT:
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
