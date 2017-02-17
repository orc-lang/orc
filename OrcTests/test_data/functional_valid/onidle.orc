-- Run g if f becomes idle/quiescent but not halted. Kill the whole mess when either half publishes.
def ifIdle[A](f :: lambda() :: A, g :: lambda() :: A) :: A = 
  {| 
    Vclock(IntegerTimeOrder) >> (
      (Vawait(0) >> Some(f()) ; None() ) |
      Vawait(1) >> Some(g())
    ) 
  |} >Some(x)> x

val c = Channel()

def test() = 
  --ifIdle({ c.get() }, { Println("get idle") }) |
  --ifIdle({ stop }, { Println("FAIL: stop idle") }) |
  ifIdle({ "Publish from left" }, { Println("FAIL: publication idle") }) |
  stop

def f(n) if (n :> 0) = 
  test() >x> Println(x) >> stop ; Println("============") >> f(n-1)

f(1000)

-- TODO: Once https://github.com/orc-lang/orc/issues/190 is fixed this should be made into a real regression test.
-- It's not a test at the moment because I really don't want to deal with a deadlock in the testing harness.