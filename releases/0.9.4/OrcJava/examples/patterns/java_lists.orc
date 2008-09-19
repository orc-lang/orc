-- some interleaving of of 1 2 3 with 1 2 3
-- followed by 1 2 3

class LinkedList = java.util.LinkedList
val l = LinkedList()

l.add(1) >>
l.add(2) >>
l.add(3) >>
-- verify that iterators are fully independent
(each(l) | each(l) ; each(l))