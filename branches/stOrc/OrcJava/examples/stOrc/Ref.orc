-- Ref.orc -- Demonstrate simple uses of Ref in stOrc
--
-- $Id$
--

val low = 1 :: Integer
val mid = 4 :: Integer{A4}
val high = 6 :: Integer{A6}
val unused = 8 :: Integer{F8}

-- The following 3 lines are a workaround for type var inference
type StoreType = Integer{A4}
def (:=)(ref::Ref[StoreType], val::StoreType) = ref.write(val)
def (?)(ref::Ref[StoreType]) = ref.read()


val refMid = Ref[Integer{A4}]()


--refMid := 1 >> stop | refMid?

refMid := mid >> stop | refMid?

--refMid := high >> stop | refMid?

{-
TYPE;  Integer{A4}
OUTPUT:
4
-}
