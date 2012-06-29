{- MutableRecursiveDescent.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 11, 2010 12:31:15 PM
 -}

{- Parse a string using mutable storage.
   Publish true iff the string is an arithmetic expression.
   First, the input string is transferred to array A omitting white spaces.
   The length of A is published.
   Next, parse(n) is called, where n is the length of the string.
   It parses the string of length n from A and publishes all possible suffix
   lengths after matching the prefix to an expression. If parse(n)
   publishes 0, then the entire string is an expression, and ``true'' is
   printed; otherwise, ``false'' is printed. 

   Call to expr(i),for instance, processes the suffix of length i from
   A. For each prefix of this string that is an expression, the
   length of the remaining suffix is published.
-}
val A = Array(100)

{- Convert a string to an array omitting white space.
   Publish the length of the array.
-}
def stringtoarray(s) = 
  {- Function rest(i,j) converts starting from position i in s.
     It writes the next non-white space in array position j.
  -}
  def rest(i,j) = 
    Ift(i >= s.length()) >> j
  | Ift(i <: s.length())  >> Ift(s.substring(i,i+1) /= " ") >> 
    A(j) := s.substring(i,i+1) >> rest(i+1,j+1)
  | Ift(i <: s.length())  >> Ift(s.substring(i,i+1)  = " ") >> rest(i+1,j)

  rest(0,0)

def parse(n) = -- Parse the string of length n from array A

  -- test if the first symbol of suffix of length k is c
  def test(k,c) = Ift(k :> 0) >> Ift(A(n-k)? = c) 
 
  def expr(i) =  Ift(i :> 0) >> term(i) >j> (j | test(j,"+") >> expr(j-1))
  def term(i) =  Ift(i :> 0) >> factor(i) >j> (j | test(j,"*") >> term(j-1))
  def factor(i) = test(i,"3") >> i-1 |  test(i,"5") >> i-1 |  
    test(i,"(") >> expr(i-1) >j> test(j,")") >> j-1
  expr(n)

def is_expr() = 
  Prompt("Write an expression")  >r> stringtoarray(r) >n> 
  (parse(n) >0> Println("true"); Println("false")) >> 
  is_expr()

is_expr()
