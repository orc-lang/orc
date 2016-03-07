include "timeIt.inc"

def fibNaive(Integer) :: Integer
def fibNaive(n) = if n <= 1 then n else fibNaive(n-1) + fibNaive(n-2)

def fibPair(Integer) :: Integer
def fibPair(n) = 
  def fib(Integer) :: (Integer, Integer)
  def fib(0) = (0, 0)
  def fib(1) = (1, 0)
  def fib(n) = 
    val (f1, f2) = fib(n-1) #
    (f1 + f2, f1)
  fib(n)(0)  

def fibPairTailrec(Integer) :: Integer
def fibPairTailrec(n) = 
  def fib(Integer, Integer, Integer) :: Integer
  def fib(i, curr, _) if (i = n) = curr
  def fib(i, curr, prev) = fib(i+1, curr + prev, curr)
  fib(0, 0, 1)

--fibPairTailrec(10000) >x> Println(x) >> stop

--;

fibPairTailrec(1000) >x> Println(x) >> stop

;

fibPair(1000) >x> Println(x) >> stop

;

fibNaive(20) >x> Println(x) >> stop

{-
OUTPUT:
43466557686937456435688527675040625802564660517371780402481729089536555417949051890403879840079255169295922593080322634775209689623239873322471161642996440906533187938298969649928516003704476137795166849228875
43466557686937456435688527675040625802564660517371780402481729089536555417949051890403879840079255169295922593080322634775209689623239873322471161642996440906533187938298969649928516003704476137795166849228875
6765
-}
{-
BENCHMARK
-}
