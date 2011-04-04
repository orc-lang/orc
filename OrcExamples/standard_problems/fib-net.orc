{-
author: Amin Shali
date: Wednesday, June 09 2010
-}

def fib(Channel[Integer], Channel[Integer], Integer) :: Top
def fib(in, out, 0) = in.get() >> out.put(0) >> fib(in, out, 0)
def fib(in, out, 1) = in.get() >> out.put(1) >> fib(in, out, 1)
def fib(in, out, n) = 
	val chinn_1 = Channel[Integer]()
	val chinn_2 = Channel[Integer]()
	val choutn_1 = Channel[Integer]()
	val choutn_2 = Channel[Integer]()

	def input(Channel[Integer], Channel[Integer], Channel[Integer], Channel[Integer], Channel[Integer]) :: Top
	def input(i, outn_1, outn_2, outn_1', outn_2') = 
  val s = Semaphore(1)
		s.acquire() >> i.get() >x> (
			if (x = 0) then
				(outn_1'.put(0), outn_2'.put(0)) >> signal 
			else (
				if (x = 1) then
					(outn_1'.put(1), outn_2'.put(0)) >> signal
				else 
					(outn_1.put(x-1), outn_2.put(x-2)) >> signal
			)
		) >> s.release() >> 
		input(i, outn_1, outn_2, outn_1', outn_2')

	def output(Channel[Integer], Channel[Integer], Channel[Integer]) :: Top
	def output(inn_1, inn_2, o) = 
		(inn_1.get(), inn_2.get()) >(a, b)> o.put(a+b) >> output(inn_1, inn_2, o)

	input(in, chinn_1, chinn_2, choutn_1, choutn_2) >> stop | 
	output(choutn_1, choutn_2, out) >> stop |
	fib(chinn_1, choutn_1, n-1) >> stop |
	fib(chinn_2, choutn_2, n-2) >> stop


val in1 = Channel[Integer]()
val out1 = Channel[Integer]()
def userin(Integer) :: Top
def userin(n) = upto(n) >v> in1.put(v+1) >> stop
def userout(Integer) :: Top
def userout(0) = signal
def userout(n) = out1.get() >z> Println(z) >> userout(n-1)
Let(fib(in1,out1,15) >> stop | userin(13) >> stop | userout(13)) >> stop

{-
OUTPUT:PERMUTABLE:
1
1
2
3
5
8
13
21
34
55
89
144
233
-}
