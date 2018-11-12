package orc.util

import scala.collection.SortedMap
import scala.math.Numeric

object NumberFormatting {
  val metricPrefixMap = SortedMap(
    1.0e18 -> "E",
    1.0e15 -> "P",
    1.0e12 -> "T",
    1.0e9 -> "G",
    1.0e6 -> "M",
    1.0e3 -> "k",
    1.0 -> "",
    1.0e-3 -> "m",
    1.0e-6 -> "µ",
    1.0e-9 -> "n",
    1.0e-12 -> "p",
    1.0e-15 -> "f",
    1.0e-18 -> "a",
  )
  val metricPrefixSearchMap = metricPrefixMap.map({ case (m, p) => (m * 999, (m, p))}) +
        (0.0 -> metricPrefixMap.head) + (Double.MaxValue -> metricPrefixMap.last)

  implicit class NumberAdds[T : Numeric](v: T) {
    import Numeric.Implicits._
    def unit(rawunit: String): String = {
      def unitp = rawunit.replaceFirst(raw"^\h", "\u202F").replaceAll(raw"\h", "\u00A0")
      def unitn = rawunit.replaceFirst(raw"^\h", "")
      v match {
        case Long.MaxValue | Long.MinValue => s"? $unitn"
        case 0 => s"0 $unitn"
        case _ if v.toDouble < 0 => "-" + (-v unit rawunit)
        case v if v.toDouble.isNaN => s"NaN $unitn"
        case v if v.toDouble.isInfinity => s"∞ $unitn"
        case _ =>
          val (mult, prefix) = metricPrefixSearchMap.from(v.toDouble).head._2
          f"${v.toDouble / mult}%3.1f $prefix${if (prefix.isEmpty) unitn else unitp}"
      }
    }
  }
}
