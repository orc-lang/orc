{-
OUTPUT:
1
1
2
2
3
3
1
2
3
-}
class LinkedList = java.util.LinkedList
val l = LinkedList()

l.add(1) >>
l.add(2) >>
l.add(3) >>
-- verify that iterators are fully independent
(each(l) | each(l) ; each(l))