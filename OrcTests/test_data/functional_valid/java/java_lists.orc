import class LinkedList = "java.util.LinkedList"
val l = LinkedList[Integer]()

l.add(1) >>
l.add(2) >>
l.add(3) >>
-- convert to native list
iterableToList[Integer](l)

{-
OUTPUT:PERMUTABLE
[1, 2, 3]
-}
