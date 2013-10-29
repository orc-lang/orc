import class LinkedList = "java.util.LinkedList"
val l = LinkedList()
val r = Ref()

def Counter(n) = n | Rwait(1) >> Counter(n+1)

Let(
    Counter(1) >n>
    l.add(n) >>
    l.remove() >n>
    r.write(n) >>
    stop
  | Rwait(10000) >> r.read()
)
