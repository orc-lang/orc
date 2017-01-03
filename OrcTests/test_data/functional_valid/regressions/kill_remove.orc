{- kill_remove.orc -- Orc program kill_remove
 -
 - Created by amp on Feb 17, 2015 1:53:14 PM
 -}
 
 {-
 Attempt to trigger an issue in which when killing an otherwise, multiple
 subgroups can try to remove themselves from the otherwise group causing 
 it to repeatly halt causing an assertion in the OtherwiseGroup.
 -}

class Complex {
	val imag :: Number
	val real :: Number
	
	def Complex(Number, Number) :: Top

	def plus(other) = Complex(real + other.real, imag + other.imag)

	def square() = Complex(real*real - imag*imag, imag*real*2)

	def distance() = (real**2 + imag**2) ** 0.5
	
	def toString() = real + "+" + imag + "i"
}
def Complex(r :: Number, i :: Number) :: Top = 
	val recurs = Complex
	new Complex with { val real = r # val imag = i # val Complex = recurs }

def computeMandelbrot(center, size :: Number, resolution :: Integer, steps :: Integer, threshold :: Number) = (
	def point(c) =
		def inner(z, n) = 
			val next = z.square().plus(c) #
			val isIn = z.distance() <: threshold #
			--Println(z.toString()) >> stop |
			(Ift(n :> steps || ~isIn) >> isIn) ; inner(next, n + 1)
		inner(Complex(0,0), 0)
	
	val pixelSize = size / resolution
	val halfres = resolution / 2
	
	--Println((pixelSize, halfres)) |
	
	upto(resolution) >i> upto(resolution) >j> (
		val pos = Complex((i-halfres)*pixelSize, (j-halfres)*pixelSize).plus(center) #
		--Println("c = " + pos.toString()) >> stop |
		(i, j, point(pos))
	)
)

val size = 32

{|
	(computeMandelbrot(Complex(0,0), 3.0, size, 10, 100) >> stop ; signal)
	|
	Rwait(100)
|} >> Rwait(200)

{-
OUTPUT:
signal
-}

