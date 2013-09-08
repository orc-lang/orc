{- recursive-descent-calculator.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 10, 2010 3:02:30 PM
 -}

{- Computes value of an arithmetic expression. Only +, unary minus and * are
   supported. White spaces are ignored. The expression and its
   value are output, or it reports the expression to be "illegal".

   The following grammar is used.

expr    ::= term | term + expr |
term    ::= factor | factor * term
factor  ::= ufactor | - ufactor
ufactor ::= integer | (expr)
integer ::= digit | digit integer

-}

def stringtolist(s :: String) :: List[String] = -- convert a string to a list omitting white space
  def rest(i :: Integer) :: List[String] =
    Ift(i >= s.length()) >> []
  | Ift(i <: s.length())  >> Ift(s.substring(i,i+1) /= " ") >> s.substring(i,i+1):rest(i+1)
  | Ift(i <: s.length())  >> Ift(s.substring(i,i+1)  = " ") >> rest(i+1)

  rest(0)

def calculate() :: Bot =
  Prompt("Write an expression")  >r> stringtolist(r) >xs>
  (expr(xs) >(n,[])> Println(r + " = " +n); Println(r +" is illegal")) >>
  calculate()

def expr(List[String]) :: (Integer, List[String])
def expr(xs) = term(xs) >(n,ys)>
               ( (n,ys)
               | ys >"+":zs> expr(zs) >(m,ws)> (n+m,ws)
               )

def term(List[String]) :: (Integer, List[String])
def term(xs) = factor(xs) >(n,ys)>
               ( (n,ys)
               | ys >"*":zs> term(zs) >(m,ws)> (n*m,ws)
               )

def factor(List[String]) :: (Integer, List[String])
def factor(xs) = ufactor(xs)
               | xs >"-":ys> ufactor(ys) >(n,zs)> (-n,zs) -- unary minus

def ufactor(List[String]) :: (Integer, List[String])
def ufactor(xs) = integer(xs)  >(Some((n,_)),ys)>     (n,ys)
                | xs >"(":ys> expr(ys) >(n,")":zs)> (n,zs)

{- integer(xs), where xs is a list of char, returns:
    (None(),xs) when xs is empty or its first symbol is a non-digit,
    (Some((v,m)),ys) when the first symbol of xs is a digit. Then,
      v = the integer prefix of xs,
      m = the smaleest power of 10 exceeding v,
      ys = the suffix of xs after removing v from it
-}

def integer(List[String]) :: (Option[(Integer, Integer)], List[String])
def integer([]) = (None(),[])
def integer(x:xs) =
  if "0" <= x && x <= "9" then
    (Read(x) :!: Integer) >d> integer(xs) >(t,ys)>
    ( t >None()>      (Some((d,10)),ys)
    | t >Some((v,m))> (Some((m * d+v, 10*m)),ys)
    )
  else (None(),x:xs)

calculate()
