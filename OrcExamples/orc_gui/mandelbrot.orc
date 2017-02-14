{- Mandelbrot.orc -- Orc program Mandelbrot
 - 
 - Created by amp on Aug 15, 2012 10:25:54 AM
 -}

include "gui.inc"

class Complex {
	val real :: Number
	val imag :: Number
	
	def Complex(real_ :: Number, imag_ :: Number) :: Complex = new Complex { val real = real_ # val imag = imag_ } 
	
	def plus(other) = Complex(real + other.real, imag + other.imag)

	def square() = Complex(real*real - imag*imag, imag*real*2)
	def scale(f) = Complex(real * f, imag * f)

	def distance() = (real**2 + imag**2) ** 0.5
	
	def toString() = real + "+" + imag + "i"
}
def Complex(real_ :: Number, imag_ :: Number) :: Complex = new Complex { val real = real_ # val imag = imag_ } 

class RenderConfig {
	val center :: Top
	val size :: Number
	val resolution :: Integer
	val steps :: Integer
	val threshold :: Number
}
val RenderConfig = new {
	def apply(center_, size_ :: Number, resolution_ :: Integer, steps_ :: Integer, threshold_ :: Number) =
		new RenderConfig {
			val center = center_
			val size = size_
			val resolution = resolution_
			val steps = steps_
			val threshold = threshold_
		}
	def unapply(v) = (v.center, v.size, v.resolution, v.steps, v.threshold)
}

def computeMandelbrot(RenderConfig(center, size :: Number, resolution :: Integer, steps :: Integer, threshold :: Number)) = (
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

val renderEvent = FanoutChannel()

val button = Button("Render")
val canvas = Canvas(size, size)
val frame = Frame([button, canvas])
val currentConf = Ref(RenderConfig(Complex(0,0), 3.0, size, 10, 100))

button.onAction() >> renderEvent.put(RenderConfig(Complex(0,0), 3.0, size, 10, 100)) >> stop

|

renderEvent.listen() >conf> {|
	currentConf := conf >> stop 
	|
	(canvas.fill(300000)
	>> computeMandelbrot(conf) 
	>(x, y, c)> (if c then canvas.setPixel(x, y, 16777215) else canvas.setPixel(x, y, 0)) >> stop
	; signal)
	|
	repeat({ Rwait(300) >> canvas.repaint() }) >> stop
	|
	renderEvent.listen() >> signal
|} >> canvas.repaint() >> stop

|

canvas.onClicked() >e> (
	val conf = currentConf?
	val clickPos = Complex((e.getX() + 0.0) / canvas.getDisplayWidth() - 0.5, 
						   (e.getY() + 0.0) / canvas.getDisplayHeight() - 0.5)
	val complexPos = clickPos.scale(conf.size).plus(conf.center)
	Println(clickPos.toString() + " " + complexPos.toString()) |
	renderEvent.put(RenderConfig(complexPos, conf.size / 2, conf.resolution, conf.steps, conf.threshold))
) >> stop

|

frame.onClosing() >e> Println("Killing: " + e) 

|} >> Println("Killed") >> stop
