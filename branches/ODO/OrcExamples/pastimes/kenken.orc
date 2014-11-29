{- kenken.orc -- "KenKen helper" Orc program
 - 
 - $Id$
 - 
 - Created by misra
 -}

-- input a number like 2243 under the appropriate operation.
-- it is read as "decompose 224 into 3 cells under that operation".
-- To add a list of values [2,7,9] that appear in n lines, write [n,2,7,9]

def gen(s,n) = Ift(s <= n) >> (n | gen(s,n-1))

def parts(d,l,h,0,0) = []

def parts(d,l,h,p,v) =
  Ift(l * p <= v && h * p >= v) >>
  gen(l,h) >x>
  parts(d,x+d,h,p-1,v-x) >xs>
  x:xs

def sum(d,l) = lambda (h,p,v) = parts(d,l,h,p,v)

def exp(n,i) = if (n%i = 0) then (1 + exp(n/i,i)) else 0
val powers23 = [(1,0,0), (2,1,0), (3,0,1), (4,2,0), (6,1,1), (8,3,0), (9,0,2)]

def parts23(_,s,h,0,m,n) = Ift(m = 0 && n = 0) >> []
def parts23(_,s,h,1,m,n) = 2**m * 3**n >t> Ift(s <= t && t <= h) >> [t]
def parts23(i,s,h,q,m,n) =
  each(powers23) >(x,y,z)> Ift(s <= x && x <= h && y <= m && z <= n) >>
  parts23(i,x+i,h,q-1,m-y,n-z) >xs> (x:xs)
def concat(zs,0,_) = zs
def concat(zs,n,v) = v:concat(zs,n-1,v)

def prod(i) =
 lambda (h,p,v) = (
  (exp(v,2), exp(v,3),exp(v,5),exp(v,7)) >(a,b,c,d)>
  Ift(2**a * 3**b * 5**c * 7**d = v) >> -- otherwise there is no solution
  Ift((c = 0 || h >= 5) && (d = 0 || h >= 7)) >> -- otherwise there is no solution
  parts23(i,1,h,p-c-d,a,b) >xs>
  concat(xs,c,5) >ys> concat(ys,d,7)
 )

def interface(msg,f,h) =
  Prompt(msg + " : write value followed by number of cells for board size " + h)  >r>
    Read(r) >s> (s%10,s/10) >(p,v)>
  (  Println(msg + v + " into " + p + " parts in board size " + h) >> f(h,p,v)
   | interface(msg,f,h)
  )

def main() =
  Prompt("Welcome to KenKen helper. What is the board size?")  >r> Read(r) >board>
  (  interface("outline * " ,prod(0),board)
  | interface("inline * "  ,prod(1),board)
  | interface("inline + "  ,sum(1,1),board)
  | interface("outline + " ,sum(0,1),board)
  )

main()
