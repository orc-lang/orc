include "timeIt.inc"

def fibNaive(Integer) :: Integer
def fibNaive(0) = 0
def fibNaive(1) = 1
def fibNaive(n) = fibNaive(n-1) + fibNaive(n-2)

def fibPair(Integer) :: Integer
def fibPair(n) = 
  def fib(Integer) :: (Integer, Integer)
  def fib(0) = (0, 0)
  def fib(1) = (1, 0)
  def fib(n) = 
    val (f1, f2) = fib(n-1) #
    (f1 + f2, f1)
  fib(n)(0)  

fibPair(1000) >x> Println(x) >> stop

;

fibNaive(19) >x> Println(x) >> stop

{-
OUTPUT:
43466557686937456435688527675040625802564660517371780402481729089536555417949051890403879840079255169295922593080322634775209689623239873322471161642996440906533187938298969649928516003704476137795166849228875
4181
-}
{-
BENCHMARK
-}
