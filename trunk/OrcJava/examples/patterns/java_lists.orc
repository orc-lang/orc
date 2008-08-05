-- some permutation of 1 1 2 2 3 3
-- followed by 1 2 3

class LinkedList = java.util.LinkedList
val l = LinkedList()

l.add(1) >>
l.add(2) >>
l.add(3) >>
(each(l) | each(l) ; each(l))