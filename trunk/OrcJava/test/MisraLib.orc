---- add(n, m)
---- sub(n, m)
---- mul(n, m)
---- div(n, m)

---- lt(n, m)
---- le(n, m)
---- eq(n, m)
---- ne(n, m)
---- ge(n, m)
---- gt(n, m)

---- and(b1, b2)
---- or(b1, b2)
---- not(b)

---- random(n)
---- if(b)

---- print(a, b, ...)
---- println(a, b, ...)

---- item(a, n)  -- get n'th item of tuple or string (zero-based index)

---- Rtimer(t)
---- true
---- false


---- A test:

---- add(3, 234) >!a> println("foo ", a) >> mul(32, 32) >!> lt(3,34) >x> not(x)
---- >> let(99,100) >y> item(y, 1) >!> item("william", 1)

	
def Metronome(x) = let(true) | Rtimer(x) >>  Metronome(x)
def BMetronome(x,n) = Rtimer(x) >> gt(n,0) >b> if(b) >!> sub(n,1) >m> BMetronome(x,m)
def loop(n) = if(gt(n,0)) >> let(n) >!> loop(sub(n,1))

def zero   = if(false)
def signal = if(true)

-- Timer functions. second and minute have arguments between 0 and 60. 
-- hour can have any non-negative argument.
-- Arguments to longterm are (week, day, hour) 
--  its arguments may be negative, though the total is non-negative.

def second(x) =  mul(x,1000) >y> Rtimer(y)
def minute(x) =  mul(x,60)   >y> second(y)
def hour(x)   =  if(ge(x,24)) >> hour(24)  >> hour(sub(x,24))
               | if(lt(x,24)) >> mul(x,60) >y> minute(y)
def longterm(w,d,h) = hour(add(mul(add(mul(w,7),d),24),h))              
def test(w,d,h) = let(add(mul(add(mul(w,7),d),24),h))

-- Rdelay returns a random number x between 0 and 5000 after x millsecs.
-- change 5000 below to get diffrent delays.

def Rdelay =  random(5000) >x> Rtimer(x) >> let(x)

-- Simulate random sites. Sint returns a random integer. 
-- Sbool returns boolean, Sstr string.
-- The delay is also returned.

def Strue  =  Rdelay >x> let(true,x)
def Sfalse =  Rdelay >x> let(false,x)
def Sbool  =  let(x) where x in  Strue | Sfalse
def Sint   =  Rdelay >x> let(x,x)
def S0   =  Rdelay >x> let(0,x)
def S1   =  Rdelay >x> let(1,x)
def S2   =  Rdelay >x> let(2,x)
def S3   =  Rdelay >x> let(3,x)
def S4   =  Rdelay >x> let(4,x)
def S5   =  Rdelay >x> let(5,x)
def S6   =  Rdelay >x> let(7,x)
def S8   =  Rdelay >x> let(8,x)
def S9   =  Rdelay >x> let(9,x)
def S10   =  Rdelay >x> let(10,x)
def S100   =  Rdelay >x> let(100,x)
def Sstr   =  Rdelay >x> let("str",x)

-- Mail routines
def email(to,subject,msg) = 
  SendMail("misra@cs.utexas.edu", to, subject, msg,"mail.cs.utexas.edu")
def selfmail(subject,msg) = 
  email("misra@cs.utexas.edu",subject,msg)

def Query     = Sbool	
def Accept(x) = if(item(x,0)) >> let(x)	
def RepeatQuery =
--	Metronome(1000)
    loop(10)
	>> Query
	>x> Accept(x)

def Delayed(N, t) =
	Rtimer(t) >> let(u,t)
	where u in N
	
def Priority(M, N, t) =
	let(x) where x in M | Delayed(N, t)

-- loop(10) >> Priority(Sbool, Sbool,1000)


