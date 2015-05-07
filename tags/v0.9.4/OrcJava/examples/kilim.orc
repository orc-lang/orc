class KilimExample = orc.lib.util.KilimExample
class KilimBuffer = orc.lib.state.KilimBuffer

val t = KilimExample("hi")
val b = KilimBuffer()

(
  t.exit() >> t.signal()  -- should not publish
  | t.error() >> t.signal() -- should not publish
) ; (
  t.signal() -- should publish
  | t.sleep(5000) >x> "5 "+x
  | t.sleep(2000) >x> "2 "+x
  | t.sleepThread(1000) >x> "1 "+x
  | t.sleepThread(1000) >x> "1 "+x
  | t.sleep(3000) >> b.put("3 buffered") >> stop
  | b.get()
)

{-
OUTPUT:
Error: java.lang.Exception: ERROR
Source location: examples/kilim.orc:9:5-14
signal
1 hi
1 hi
2 hi
3 buffered
5 hi
-}