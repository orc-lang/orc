class KilimExample = orc.lib.util.KilimExample
class KilimBuffer = orc.lib.state.KilimBuffer

val t = KilimExample("hi")
val b = KilimBuffer()

println(t.signal()) >> stop
; t.exit() >> println("exit") >> stop
; t.error() >> println("exit") >> stop
; t.sleep(500) >x> println("5 "+x) >> stop
; t.sleep(200) >x> println("2 "+x) >> stop
; t.sleepThread(100) >x> println("1 "+x) >> stop
; t.sleepThread(100) >x> println("1 "+x) >> stop
; t.sleep(300) >> b.put("3 buffered") >> stop
  | b.get()

{-
OUTPUT:
signal
Error: java.lang.Exception: ERROR
Source location: examples/kilim.orc:9:3-12
5 hi
2 hi
1 hi
1 hi
3 buffered
-}