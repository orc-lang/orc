import class FileInputStream = "java.io.FileInputStream"
import class InputStreamReader = "java.io.InputStreamReader"
import class StringBuilder = "java.lang.StringBuilder"

import class Char = "java.lang.Character"

def GetContent(InputStreamReader) :: String
def GetContent(in) =
  val buf = Array[Char](1024, "char")
  val out = StringBuilder()
  def loop(Integer) :: String
  def loop(len) =
    if (len <: 0) then
      out.toString()
    else
      out.append(buf, 0, len) >>
      loop(in.read(buf))
  loop(in.read(buf)) >>
  out.toString()

InputStreamReader(FileInputStream("test_data/functional_valid/java/java_io_sample.txt")) >reader>
Println(GetContent(reader)) >>
stop

{-
OUTPUT:
'Twas brillig, and the slithy toves
Did gyre and gimble in the wabe;
All mimsy were the borogoves,
And the mome raths outgrabe.

-}
