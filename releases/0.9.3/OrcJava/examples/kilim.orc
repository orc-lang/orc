class KilimExample = orc.lib.util.KilimExample
class KilimBuffer = orc.lib.state.KilimBuffer

val t = KilimExample("hi")
val b = KilimBuffer()

  t.sleep(5000) >x> "5 "+x
| t.sleep(2000) >x> "2 "+x
| t.exit() >> t.signal()  -- should not publish
| t.error() >> t.signal() -- should not publish
| t.signal() -- should publish
| t.sleepThread(1000) >x> "1 "+x
| t.sleepThread(1000) >x> "1 "+x
| t.sleep(3000) >> b.put("3 buffered") >> stop
| b.get()