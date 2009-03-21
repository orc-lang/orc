def Actor() =
  type Message = Do(_) | Stop()
  val m = Buffer()
  def dof(f) = m.put(Do(f))
  def stopf() = m.put(Stop())
  def loop() =
    def case(Do(f)) = f() >> stop ; loop()
    def case(Stop()) = stop
    case(m.get())
  isolated (loop() | Record("do", dof, "stop", stopf))

val a = Actor()
val c = Counter(2)
( a.do(lambda () = print("hi ") >> Rtimer(50) >> print("there ") >> c.dec())
, Rtimer(10) >> a.do(lambda () = println("bob") >> c.dec())) >>
c.onZero() >>
a.stop() >>
stop
{-
OUTPUT:
hi there bob
-}