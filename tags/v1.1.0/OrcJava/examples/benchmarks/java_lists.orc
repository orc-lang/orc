class LinkedList = java.util.LinkedList
val l = LinkedList()
val r = Ref()

def Counter(n) = n | Rtimer(1) >> Counter(n+1)

let(
    Counter(1) >n>
    l.add(n) >>
    l.remove() >n>
    r.write(n) >>
    stop
  | Rtimer(10000) >> r.read()
)