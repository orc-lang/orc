def interface(msg) =
  val aa = ""
  Prompt(msg + " : write value followed by number of cells for board size ")  >>
  (  println(msg + " into " + " parts in board size ") >> println("Out 2") >> stop
   | interface(msg)
  )

def main() =
Prompt("Prompt 1") >>
( interface("A")
| interface("B")
| interface("C")
| interface("D")
)

main()


