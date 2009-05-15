class FileInputStream = java.io.FileInputStream
class InputStreamReader = java.io.InputStreamReader
class StringBuilder = java.lang.StringBuilder

def GetContent(InputStreamReader) :: String
def GetContent(in) =
  val buf = Array[Integer](1024, "char")
  val out = StringBuilder()
  def loop(Integer) :: String
  def loop(len) =
    if len < 0 then
      out.toString()
    else
      out.append(buf, 0, len) >>
      loop(in.read(buf))
  loop(in.read(buf)) >>
  out.toString()

InputStreamReader(FileInputStream("examples/java_io_sample.txt")) >reader>
println(GetContent(reader)) >>
stop

{-
OUTPUT:
'Twas brillig, and the slithy toves
Did gyre and gimble in the wabe;
All mimsy were the borogoves,
And the mome raths outgrabe.
-}