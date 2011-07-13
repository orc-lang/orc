def inc(x) = atomic x := x? + 1
def inc2(x) = atomic (inc(x), inc(x))

def assert(x, n) if (x = n) = Println("ok") >> stop
def assert(x, _) = Println("Unexpected result: " + x) >> stop


upto(10000) >>
TRef(0) >t> 
(inc2(t), inc(t), inc2(t)) >> 
assert(atomic t?, 5)