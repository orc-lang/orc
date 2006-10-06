-- Use M-x shell from emacs file. Then command 
--  rorc MisraTest.orc  (M-p) to repeat command
--  File name: ~/eclipse_workspace/OrcJava/test


-- (fset 'nx
--    [?- ?- ?! ?! ?  ?\C-s ?- ?- ?! ?! ?\C-  backspace backspace backspace backspace ?\C-x ?\C-s ?\C-x ?o ?\M-p])


-- **********    Trivial Test *****************************

--!! let(2)

-- **********     Library Sites *****************************

-- add(n, m)
-- sub(n, m)
-- mul(n, m)
-- div(n, m)

-- lt(n, m)
-- le(n, m)
-- eq(n, m)
-- ne(n, m)
-- ge(n, m)
-- gt(n, m)

-- and(b1, b2)
-- or(b1, b2)
-- not(b)

-- random(n), returns a random number in 0..n
-- if(b), returns true if b is true, silent otherwise

-- print(a, b, ...)
-- println(a, b, ...)

-- item(a, n)  -- get n'th item of tuple or string (zero-based index)

-- Rtimer(t)
-- true
-- false

-- **********  Expression Definitions  *****************************
	
def Metronome(x) = let(x) | Rtimer(x) >>  Metronome(x)

--!! Metronome(2000)

--!!  Metronome(2000) | Metronome(3000)


def BMetronome(x,n) = Rtimer(x) >> gt(n,0) >b> if(b) >!> sub(n,1) >m> BMetronome(x,m)

def zero   = if(false)
def signal = if(true)

-- Timer functions. second and minute have arguments between 0 and 60. 
-- hour can have any non-negative argument.
-- Arguments to longterm are (week, day, hour) 
--  its arguments may be negative, though the total is non-negative.

def second(x) =  mul(x,1000) >y> Rtimer(y)
def minute(x) =  mul(x,60)   >y> second(y)
def hour(x)   =  if(gt(x,24)) >> hour(24)  >> hour(sub(x,24))
               | if(le(x,24)) >> mul(x,60) >y> minute(y)
def longterm(w,d,h) = hour(add(mul(add(mul(w,7),d),24),h))              


-- Rdelay returns a random number x between 0 and 5000 after x millsecs.
-- change 5000 below to get different delays.

def Rdelay =  random(5000) >x> Rtimer(x) >> let(x)

--!!  Rdelay

-- **********  Site Definitions  *****************************

-- Simulate random sites. Sint returns a random integer. 
-- Sbool returns boolean, Sstr string.
-- The delay is also returned.

def Strue  =  Rdelay >x> let(true,x)
def Sfalse =  Rdelay >x> let(false,x)
def Sbool  =  let(x) where x in  Strue | Sfalse
def Sint   =  Rdelay >x> let(x,x)
def Sstr   =  Rdelay >x> let("str",x)

-- **********  Examples    *****************************

--!! let(x,y) where x in Sfalse; y in Sbool

def Query     = Sbool	
def Accept(x) = if(item(x,0)) >> let(x)
def RepeatQuery =
	Metronome(1000)
	>> Query
	>x> Accept(x)

--!!  RepeatQuery

def fib(m,n) = Rtimer(50) >> let(n) >!> fib(n, add(m,n))

--!! fib(0,1)

def loop(n) = if(gt(n,0)) >> let(n) >!> loop(sub(n,1))

--!! loop(10) >> Sstr

def join(M, N) =
	let(u, v) >> signal
	where u in M;
	      v in N

--!! join(Sbool, Sbool)

def parallelOr(M, N) =
	let(z)
	where z in { if(x) | if(y) | or(x, y) };
		  x in M >p> item(p,0);
		  y in N >p> item(p,0)
		  
--!! loop(10) >> parallelOr(Sbool, Sbool)

def Delayed(N, t) =
	Rtimer(t) >> let(u)
	where u in N
	
def Priority(M, N, t) =
	let(x) where x in M >y> let("M",item(y,1))| Delayed(N, t) >y> let("N",item(y,1))

--!! loop(10) >> Priority(Sbool, Sbool,1000)

-- *********************** Mail routines  ***********************

-- Rtimer(6000) >> SendMail("misra@cs.utexas.edu", 
-- 		 "misra@cs.utexas.edu", 
-- 		 "this is the first test", 
-- 		 "and a big message", 
-- 		 "mail.cs.utexas.edu")

def email(to,subject,msg) = 
  SendMail("misra@cs.utexas.edu", to, subject, msg,"mail.cs.utexas.edu")
def selfmail(subject,msg) = 
  email("misra@cs.utexas.edu",subject,msg)
