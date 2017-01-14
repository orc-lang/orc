package orc.scalabenchmarks

import orc.test.BenchmarkApplication

object Mandelbrot extends BenchmarkApplication {
  type D = BigDecimal
  val D = BigDecimal
  
  implicit class DOps(val d: D) extends AnyVal {
    def nativepow(e: D) = D.valueOf(math.pow(d.toDouble, e.toDouble))
  }

  case class Complex(ri: D, im: D) {
    def +(o: Complex) = Complex(ri + o.ri, im + o.im)    
    def squared = Complex(ri*ri - im*im, im*ri*2)    
    def distance = (ri.pow(2) + im.pow(2)).nativepow(0.5)
  }

  val threshold: D = 100
  val steps: Integer = 10
  val size: Integer = 64
  val resolution = D(3.0) / D(size)
  val offset = D(size) / D(2.0)

  def point(c: Complex): Boolean = {
  	def inner(z: Complex, n: Integer): Boolean = {
  		val next = z.squared + c
  		val isIn = z.distance < threshold
  		if (n > steps || !isIn) isIn else inner(next, n + 1)
  	}
  	inner(Complex(0,0), 0)
  }

  def cell(i: Integer, j: Integer) = point(Complex((D(i) - offset) * resolution, (D(j) - offset) * resolution))
  def row(i: Integer): Array[Boolean] = {
    (0 until size).map(cell(i, _)).toArray
  }

  def showRow(l: Seq[Boolean]) = {
    l.map(x => if (x) "@" else ".").reduce(_ + _)
  }

  def main(args: Array[String]): Unit = {
    println("size = " + size + ", resolution = " + resolution + ", offset = " + offset)

    val ll = Util.timeIt((0 until size).map(row(_)).toArray)
    val ls = ll.map(showRow(_))
    println(ls.reduce(_ + "\n" + _))
  }

}
