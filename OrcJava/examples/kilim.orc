class KilimExample = orc.lib.util.KilimExample

val t = KilimExample("hi")
  t.sleep(5000)
| t.sleep(2000)
| t.exit() >> t.signal()
| t.error() >> t.signal()
| t.signal()