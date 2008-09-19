site cat = orc.lib.str.Cat
site print = orc.lib.str.Print
site println = orc.lib.str.Println

site random = orc.lib.util.Random
site UUID = orc.lib.util.UUID
site Thread = orc.lib.util.ThreadSite
site Null = orc.lib.util.JavaNull
site Array = orc.lib.util.JavaArray
site ReadText = orc.lib.util.ReadText

site Buffer = orc.lib.state.Buffer
site SyncChannel = orc.lib.state.SyncChannel
site Cell = orc.lib.state.Cell
site Ref = orc.lib.state.Ref

site parseInt = orc.lib.str.AtoI
site parseBool = orc.lib.str.AtoB

def ForkJoin(ps) =
  def par([]) = stop
  def par(h:t) = h() | par(t)
  par(ps) >> stop ; signal

def collect(p) =
  val b = Buffer()
  p() >x> b.put(x) >> stop
  ; b.getAll()

def range(low, high) =
  if low <= high then
    low:range(low+1, high)
  else []
  
def lines(text) =
  val out = text.split("\\n")
  if out.get(out.length()-1) = "" then
    out.split(0, out.length()-1)
  else out

def unlines(line:lines) = cat(line, "\n", unlines(lines))
def unlines([]) = ""