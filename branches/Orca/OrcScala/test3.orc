def randabort() = Ift(Random(2) = 0) >> Abort()

atomic ( 
  Println("txn initiated") >> 1 
| Rwait(2000) >> 2 
| Rwait(1000) >> randabort()
)
