package orc.test.item.scalabenchmarks

object Mandelbrot extends BenchmarkApplication[Unit, IndexedSeq[IndexedSeq[Boolean]]] with ExpectedBenchmarkResult[IndexedSeq[IndexedSeq[Boolean]]] {
  type D = Double
  object D {
    def apply(x: Integer): Double = x.toDouble
    def apply(x: Double): Double = x
    def valueOf(x: Double): Double = x    
  }

  implicit class DOps(val d: D) extends AnyVal {
    def nativepow(e: D) = D.valueOf(math.pow(d.toDouble, e.toDouble))
  }

  case class Complex(ri: D, im: D) {
    def +(o: Complex) = Complex(ri + o.ri, im + o.im)
    def squared = Complex(ri * ri - im * im, im * ri * 2)
    def distance = (ri*ri + im*im).nativepow(0.5)
  }

  val threshold: D = 100
  val steps: Integer = 10
  val gridsize: Integer = BenchmarkConfig.problemSizeSqrtScaledInt(100)
  val resolution = D(3.0) / D(gridsize)
  val offset = D(gridsize) / D(2.0)

  def point(c: Complex): Boolean = {
    def inner(z: Complex, n: Integer): Boolean = {
      val next = z.squared + c
      val isIn = z.distance < threshold
      if (n > steps || !isIn) isIn else inner(next, n + 1)
    }
    inner(Complex(0, 0), 0)
  }

  def cell(i: Integer, j: Integer) = point(Complex((D(i) - offset) * resolution, (D(j) - offset) * resolution))
  def row(i: Integer): IndexedSeq[Boolean] = {
    (0 until gridsize).map(cell(i, _))
  }

  def showRow(l: Seq[Boolean]) = {
    l.map(x => if (x) "@" else ".").reduce(_ + _)
  }

  def benchmark(ctx: Unit) = {
    //println("size = " + gridsize + ", resolution = " + resolution + ", offset = " + offset)

    val ll = (0 until gridsize).map(row(_))
    ll
    //val ls = ll.map(showRow(_))
    //println(ls.reduce(_ + "\n" + _))
  }

  val name: String = "Mandelbrot"

  def setup(): Unit = ()

  val size: Int = gridsize * gridsize

  val expectedMap: Map[Int, Int] = Map(
      1 -> 0xe0edb51,
      10 -> 0x1b3e0bad,
      100 -> 0x3f4d8b6b,
      )
}
